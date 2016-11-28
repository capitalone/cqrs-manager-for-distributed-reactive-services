;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.log.kafka
  (:refer-clojure :exclude [partition])
  (:require [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as p]
            [clojure.spec :as s]
            [clojure.data.fressian :as fressian]
            [com.stuartsierra.component :as c]
            [io.pedestal.log :as log]
            [com.capitalone.commander.util :as util]
            [com.capitalone.commander.index :as index]
            [com.capitalone.commander.log :as l])
  (:import [org.apache.kafka.clients.producer Producer MockProducer KafkaProducer ProducerRecord Callback RecordMetadata]
           [org.apache.kafka.clients.consumer Consumer ConsumerRebalanceListener MockConsumer KafkaConsumer ConsumerRecord OffsetResetStrategy]
           [org.apache.kafka.common.serialization Serializer Deserializer]
           [org.apache.kafka.common.errors WakeupException]
           [org.apache.kafka.common TopicPartition]))

(set! *warn-on-reflection* true)

(deftype FressianSerializer []
  Serializer
  (close [_])
  (configure [_ _ _])
  (serialize [_ _ data]
    (util/buf->bytes (fressian/write data :footer? true))))

(deftype FressianDeserializer []
  Deserializer
  (close [_])
  (configure [_ _ _])
  (deserialize [_ _ data]
    (fressian/read data)))

(defn ^{:private true} producer-record
  "Constructs a ProducerRecord from a map conforming to
  ProducerRecordSchema."
  [record]
  (let [{:keys [topic value key partition]} record
        topic                               (str topic)]
    (cond
      (and partition key) (ProducerRecord. topic (int partition) key value)
      key                 (ProducerRecord. topic key value)
      :else               (ProducerRecord. topic value))))

(s/fdef producer-record
        :args (s/cat :record ::l/producer-record)
        :ret #(instance? ProducerRecord %))

(defrecord ProducerComponent [^Producer producer ctor]
  c/Lifecycle
  (start [this]
    (assoc this :producer (ctor)))
  (stop  [this]
    (when producer (.close producer))
    this)

  l/EventProducer
  (-send! [this record result-ch]
    (.send producer
           (producer-record record)
           (reify
             Callback
             (^void onCompletion [_ ^RecordMetadata rm ^Exception e]
              (let [ret (when rm
                          {:offset    (.offset rm)
                           :partition (.partition rm)
                           :topic     (.topic rm)
                           :timestamp (.timestamp rm)})]
                (a/put! result-ch (or ret e))))))
    result-ch))

(defn construct-producer
  "Constructs and returns a Producer according to config map (See
  https://kafka.apache.org/documentation.html#producerconfigs for
  details)."
  [producer-config]
  (let [{:keys [servers timeout-ms client-id config key-serializer value-serializer]
         :or {config           {}
              key-serializer   (FressianSerializer.)
              value-serializer (FressianSerializer.)
              client-id        "commander-rest-producer"}}
        producer-config]
    (map->ProducerComponent
     {:ctor #(KafkaProducer. ^java.util.Map
                             (assoc config
                                    "request.timeout.ms" (str timeout-ms)
                                    "bootstrap.servers" servers
                                    "client.id" client-id
                                    "compression.type" "gzip"
                                    "acks" "all")
                             ^Serializer key-serializer
                             ^Serializer value-serializer)})))

(defn mock-producer
  "Returns a MockProducer, useful for testing, etc."
  []
  (map->ProducerComponent
   {:ctor #(MockProducer. true (FressianSerializer.) (FressianSerializer.))}))

(defrecord ConsumerComponent [^Consumer consumer ctor]
  c/Lifecycle
  (start [this]
    (assoc this :consumer (ctor)))
  (stop [this]
    (when consumer (.wakeup consumer))
    (dissoc this :consumer))

  l/EventConsumer
  (-subscribe! [_ topics index]
    (if index
      (.subscribe consumer
                  ^java.util.Collection topics
                  (reify ConsumerRebalanceListener
                    (onPartitionsAssigned [_ partitions]
                      (log/info ::ConsumerRebalanceListener :onPartitionsAssigned
                                :partitions partitions)
                      (doseq [^TopicPartition partition partitions]
                        (let [offset (or (index/find-latest-partition-offset index
                                                                             (.topic partition)
                                                                             (.partition partition))
                                         -1)]
                          (.seek consumer partition (inc offset)))))
                    (onPartitionsRevoked  [_ partitions]
                      (log/info ::ConsumerRebalanceListener :onPartitionsRevoked
                                :partitions partitions))))
      (.subscribe consumer topics)))

  (-consume-onto-channel! [this ch timeout]
    (log/debug ::kafka-consumer-onto-ch! [this ch timeout])
    (a/thread
      (log/debug ::kafka-consumer-onto-ch! :consumer
                 :consumer consumer)
      (try
        (loop []
          (log/trace ::kafka-consumer-onto-ch! :loop
                     :consumer consumer)
          (if (p/closed? ch)
            :done
            (let [records (.poll consumer timeout)]
              (doseq [^ConsumerRecord record records]
                (let [record-map {:key       (.key record)
                                  :value     (.value record)
                                  :topic     (.topic record)
                                  :partition (.partition record)
                                  :offset    (.offset record)
                                  :timestamp (.timestamp record)}]
                  (log/debug ::kafka-consumer-onto-ch! :record-received :record-map record-map)
                  (when-not (a/>!! ch record-map)
                    (log/debug ::kafka-consumer-onto-ch! :destination-closed :ch ch))))
              (recur))))
        (catch WakeupException e
          (log/error ::kafka-consumer-onto-ch! "Wakeup received from another thread, closing."
                     :exception e))
        (catch Exception e
          (log/error ::kafka-consumer-onto-ch! "Exception while polling Kafka, and re-throwing."
                     :exception e)
          (throw e))
        (finally
          (log/info ::kafka-consumer-onto-ch! "Cleaning up Kafka consumer and closing.")
          (.close consumer)
          :done)))))

(defn construct-consumer
  "Creates a KafkaConsumer for the given config map (must include at
  least :servers and :group-id)"
  [consumer-config]
  (let [{:keys [servers group-id client-id config key-deserializer value-deserializer]
         :or {config             {}
              key-deserializer   (FressianDeserializer.)
              value-deserializer (FressianDeserializer.)
              client-id          "commander-consumer"}}
        consumer-config]
    (log/info ::construct-consumer [config key-deserializer value-deserializer])
    (map->ConsumerComponent {:ctor #(KafkaConsumer. ^java.util.Map
                                                    (assoc config
                                                           "bootstrap.servers" servers
                                                           "group.id" group-id
                                                           "client.id" client-id
                                                           "enable.auto.commit" false)
                                                    ^Deserializer key-deserializer
                                                    ^Deserializer value-deserializer)})))

(defn mock-consumer []
  (map->ConsumerComponent {:ctor #(MockConsumer. OffsetResetStrategy/LATEST)}))
