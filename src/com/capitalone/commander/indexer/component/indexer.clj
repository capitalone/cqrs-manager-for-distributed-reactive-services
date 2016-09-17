;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.indexer.component.indexer
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [clojure.java.jdbc :as j]
            [com.capitalone.commander.kafka :as kafka]
            [com.capitalone.commander.database :as database]
            [com.capitalone.commander.database :as d]
            [com.capitalone.commander.kafka :as k])
  (:import [org.apache.kafka.clients.consumer Consumer ConsumerRebalanceListener]
           (org.apache.kafka.common TopicPartition)))

(set! *warn-on-reflection* true)

(defn command-map
  [{:keys [key value topic partition offset timestamp] :as command}]
  (log/debug ::command-map [command])
  (let [{:keys [action data]} value]
    {:id        key
     :action    action
     :data      data
     :timestamp timestamp
     :topic     topic
     :partition partition
     :offset    offset}))

(defn event-map
  [{:keys [key value topic partition offset timestamp] :as event}]
  (log/debug ::event-map [event])
  (let [{:keys [action data parent]} value]
    {:id        key
     :parent    parent
     :action    action
     :data      data
     :timestamp timestamp
     :topic     topic
     :partition partition
     :offset    offset}))

(defn record-commands-and-events!
  "Records all commands and events arriving on ch to the given database
  component. Returns the go-loop channel that will convey :done when ch is closed."
  [database commands-topic events-topic ch]
  (log/debug ::record-events! [database commands-topic events-topic ch])
  (a/go-loop []
    (when-some [msg (a/<! ch)]
      (try
        (log/debug ::record-events! :msg :msg msg)
        (condp = (:topic msg)
          events-topic   (database/insert-events! database (event-map msg))
          commands-topic (database/insert-commands! database (command-map msg))
          (log/warn ::record-commands-and-events! "Unexpected topic and message"
                    :topic (:topic msg)
                    :msg msg))
        (catch Exception e
          (log/error :msg "Error indexing event" :msg msg :exception e)))
      (recur))))

(defrecord Indexer [database kafka-consumer commands-topic events-topic ch]
  component/Lifecycle
  (start [this]
    (let [ch (a/chan 1)
          ^Consumer consumer (:consumer kafka-consumer)]
      (.subscribe consumer ^java.util.List [commands-topic events-topic]
                  (reify ConsumerRebalanceListener
                    (onPartitionsAssigned [_ partitions]
                      (log/info ::ConsumerRebalanceListener :onPartitionsAssigned
                                :partitions partitions)
                      (doseq [^TopicPartition partition partitions]
                        (let [offset (or (d/find-latest-partition-offset database
                                                                         (.topic partition)
                                                                         (.partition partition))
                                         -1)]
                          (.seek consumer partition (inc offset)))))
                    (onPartitionsRevoked  [_ partitions]
                      (log/info ::ConsumerRebalanceListener :onPartitionsRevoked
                                :partitions partitions))))

      (k/kafka-consumer-onto-ch! kafka-consumer ch)
      (record-commands-and-events! database commands-topic events-topic ch)
      (assoc this :ch ch)))
  (stop [this]
    (when ch (a/close! ch))
    (dissoc this :ch)))

(defn construct-indexer
  [config]
  (map->Indexer
   (select-keys config [:kafka-consumer-config :commands-topic :events-topic])))
