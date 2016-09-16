;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.kafka
  (:refer-clojure :exclude [partition])
  (:require [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as p]
            [clojure.spec :as s]
            [clojure.data.fressian :as fressian]
            [com.stuartsierra.component :as c]
            [io.pedestal.log :as log]
            [com.capitalone.commander.util :as util]
            [com.capitalone.commander :as commander])
  (:import [org.apache.kafka.clients.producer MockProducer KafkaProducer ProducerRecord Callback RecordMetadata]
           [org.apache.kafka.clients.consumer MockConsumer KafkaConsumer ConsumerRecord OffsetResetStrategy]
           [org.apache.kafka.common.serialization Serializer Deserializer]
           [org.apache.kafka.common.errors WakeupException]
           [org.apache.kafka.common TopicPartition]))

(set! *warn-on-reflection* true)

(s/def ::ReadPort  #(satisfies? p/ReadPort %))
(s/def ::WritePort #(satisfies? p/WritePort %))
(s/def ::Channel   #(satisfies? p/Channel %))

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

(defrecord Producer [config ^Serializer key-serializer ^Serializer value-serializer producer]
  c/Lifecycle
  (start [this]
    (if producer
      this
      (assoc this :producer (KafkaProducer. ^java.util.Map config key-serializer value-serializer))))
  (stop  [this]
    (when producer (.close ^org.apache.kafka.clients.producer.Producer producer))
    (dissoc this :producer)))

(def default-producer-config
  {"acks" "all"})

(defn construct-producer
  "Constructs and returns a Producer according to config map (See
  https://kafka.apache.org/documentation.html#producerconfigs for
  details)."
  [producer-config]
  (let [{:keys [servers timeout-ms client-id config key-serializer value-serializer]
         :or {config           {}
              key-serializer   (FressianSerializer.)
              value-serializer (FressianSerializer.)
              client-id        ""}}
        producer-config]
    (map->Producer {:config           (assoc config
                                             "request.timeout.ms" (str timeout-ms)
                                             "bootstrap.servers"  servers
                                             "client.id"          client-id)
                    :key-serializer   key-serializer
                    :value-serializer value-serializer})))

(defn mock-producer
  "Returns a MockProducer, useful for testing, etc."
  []
  (let [key-serializer   (FressianSerializer.)
        value-serializer (FressianSerializer.)]
    (map->Producer {:config           default-producer-config
                    :key-serializer   key-serializer
                    :value-serializer value-serializer
                    :producer         (MockProducer. true key-serializer value-serializer)})))

(s/def ::key ::commander/id)
(s/def ::value any?)

(s/def ::producer-record
  (s/keys :req-un [::commander/topic ::value]
          :opt-un [::key ::commander/partition]))

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
        :args (s/cat :record ::producer-record)
        :ret #(instance? ProducerRecord %))

(defn send!
  "Sends record (a map of :topic, :value and
  optionally :key, :partition) via the given Producer component.
  Returns ch (a promise-chan unless otherwise specified). ch will
  convey record metadata."
  ([producer record]
   (send! producer record (a/promise-chan)))
  ([producer record ch]
   (when-let [^org.apache.kafka.clients.producer.Producer
              kafka-producer (:producer producer)]
     (.send kafka-producer
            (producer-record record)
            (reify
              Callback
              (^void onCompletion [_ ^RecordMetadata rm ^Exception e]
               (let [ret (when rm
                           {:offset    (.offset rm)
                            :partition (.partition rm)
                            :topic     (.topic rm)
                            :timestamp (.timestamp rm)})]
                 (a/put! ch (or ret e))))))
     ch)))

(s/def ::record-metadata (s/keys :req-un [::commander/topic
                                          ::commander/partition
                                          ::commander/offset
                                          ::commander/timestamp]))

(s/fdef send!
        :args (s/cat :producer #(instance? Producer %)
                     :record   ::producer-record
                     :ch       (s/? ::WritePort))
        :ret  ::ReadPort
        :fn   #(= (-> % :args :ch) (-> % :ret)))

(defn kafka-consumer-onto-ch-async!
  "On a new thread, polls on a loop the given a KafkaConsumer created
  by zero-arity fn consumer-ctor, putting onto ch a map
  of :key, :value, :topic, :partition, and :offset for every
  ConsumerRecord it receives. Exits loop, unsubscribes, and closes
  KafkaConsumer on error, or if ch is closed.

  Caller can optionally specify a polling timeout (in milliseconds,
  defaults to 100)."
  ([consumer-ctor ch]
   (kafka-consumer-onto-ch-async! consumer-ctor ch 10000))
  ([consumer-ctor ch timeout]
   (log/debug ::kafka-consumer-onto-ch-async! [consumer-ctor ch timeout])
   (a/thread
     (let [^KafkaConsumer consumer (consumer-ctor)]
       (log/debug ::kafka-consumer-onto-ch-async! :consumer
                  :consumer consumer
                  :assignments (->> consumer
                                    .assignment
                                    (map (fn [^TopicPartition tp]
                                           (hash-map :topic (.topic tp)
                                                     :partition (.partition tp))))))
       (try
         (loop []
           (log/trace ::kafka-consumer-onto-ch-async! :loop
                      :consumer consumer
                      :positions (for [^TopicPartition tp (.assignment consumer)]
                                   {:topic     (.topic tp)
                                    :partition (.partition tp)
                                    :position  (.position consumer tp)}))
           (if (p/closed? ch)
             :done
             (let [records (.poll consumer timeout)]
               (doseq [^ConsumerRecord record (-> records .iterator iterator-seq)]
                 (let [record-map {:key       (.key record)
                                   :value     (.value record)
                                   :topic     (.topic record)
                                   :partition (.partition record)
                                   :offset    (.offset record)
                                   :timestamp (.timestamp record)}]
                   (log/debug ::kafka-consumer-onto-ch-async! :record-received :record-map record-map)
                   (when-not (a/>!! ch record-map)
                     (log/debug ::kafka-consumer-onto-ch-async! :destination-closed :ch ch))))
               (recur))))
         (catch Exception e
           (log/error ::kafka-consumer-onto-ch-async! "Exception while polling Kafka, exiting poll loop and re-throwing."
                      :exception e)
           (throw e))
         (finally
           (log/info ::kafka-consumer-onto-ch-async! "Cleaning up KafkaConsumer and closing.")
           (.unsubscribe consumer)
           (.close consumer)))))))

(defn topics-consumer-ch
  "Creates a KafkaConsumer for the given config map (must include at
  least \"bootstrap.servers\" and \"group.id\"), and subscribes to the
  given list of topics.  Returns a channel (ch if given) that will
  receive a map of :key, :value, :topic, :partition, and :offset for
  every ConsumerRecord conveyed on the subscribed topics.

  Caller can optionally specify a key- and value-deserializer for the
  underlying KafkaConsumer, otherwise KafkaConsumer uses
  FressianDeserializer for both."
  ([config topics]
   (topics-consumer-ch config topics (a/chan 10)))
  ([config topics ch]
   (topics-consumer-ch config topics ch (FressianDeserializer.) (FressianDeserializer.)))
  ([config             #_{s/Str s/Any}
    topics             #_[s/Str]
    ch                 #_WritePort
    key-deserializer   #_Deserializer
    value-deserializer #_Deserializer]
   (log/info ::topics-consumer-ch [config topics ch key-deserializer value-deserializer])
   (let [consumer-ctor (fn []
                         (doto (KafkaConsumer. ^java.util.Map config
                                               ^Deserializer key-deserializer
                                               ^Deserializer value-deserializer)
                           (.subscribe topics)))]

     (kafka-consumer-onto-ch-async! consumer-ctor ch)
     ch)))

(defn partition-topic-consumer-ch
  "Creates a KafkaConsumer for the given config map (must include at
  least \"bootstrap.servers\" and \"group.id\"), assigns the Consumer
  to the given topic and partition (0 if not specified), and seeks to
  the given offset (0 if not specified).  Returns a channel (ch if
  given) that will receive a map of :key, :value, :topic, :partition,
  and :offset for every ConsumerRecord conveyed on the given
  topic/partition.

  Caller can optionally specify a key- and value-deserializer for the
  underlying KafkaConsumer, otherwise KafkaConsumer uses
  FressianDeserializer for both."
  ([config topic]
   (partition-topic-consumer-ch config topic 0))
  ([config topic partition]
   (partition-topic-consumer-ch config topic partition 0))
  ([config topic partition offset]
   (partition-topic-consumer-ch config
                                topic
                                partition
                                offset
                                (a/chan 10)))
  ([config topic partition offset ch]
   (partition-topic-consumer-ch config
                                topic
                                partition
                                offset
                                ch
                                (FressianDeserializer.)
                                (FressianDeserializer.)))
  ([config             #_{"bootstrap.servers" s/Str
                          "group.id"          s/Str
                          s/Str               s/Any}
    topic              #_s/Str
    partition          #_s/Int
    offset             #_s/Int
    ch                 #_WritePort
    key-deserializer   #_Deserializer
    value-deserializer #_Deserializer]
   (log/info ::partition-topic-consumer-ch [config topic partition offset ch key-deserializer value-deserializer])
   (let [consumer-ctor (fn []
                         (let [tp       (TopicPartition. topic (or partition 0))
                               consumer (KafkaConsumer. ^java.util.Map config
                                                        ^Deserializer key-deserializer
                                                        ^Deserializer value-deserializer)]
                           (log/debug ::consumer-ctor consumer)
                           (doto consumer
                             (.assign [tp])
                             (.seek tp (or offset 0)))))]
     (kafka-consumer-onto-ch-async! consumer-ctor ch)
     ch)))
