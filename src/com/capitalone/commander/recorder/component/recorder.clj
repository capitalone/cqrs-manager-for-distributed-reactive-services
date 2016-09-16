;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.recorder.component.recorder
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [clojure.java.jdbc :as j]
            [com.capitalone.commander.kafka :as kafka]
            [com.capitalone.commander.database :as database]))

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

(defn record-commands!
  "Records all commands arriving on commands-ch to the given database
  component.  Returns a channel containing the results of the database
  transactions."
  ([database commands-ch]
   (record-commands! database commands-ch 10))
  ([database commands-ch in-flight]
   (log/debug ::record-commands! [database commands-ch in-flight])
   (a/go-loop []
     (when-some [command (a/<! commands-ch)]
       (try
         (database/insert-commands! database (command-map command))
         (catch Exception e
           (log/error :msg "Error indexing command" :command command :exception e)))
       (recur)))))

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

(defn record-events!
  "Records all events arriving on events-ch to the given database
  component.  Returns a channel containing the results of the database
  transactions."
  ([database events-ch]
   (record-events! database events-ch 10))
  ([database events-ch in-flight]
   (log/debug ::record-events! [database events-ch in-flight])
   (a/go-loop []
     (when-some [event (a/<! events-ch)]
       (try
         (database/insert-events! database (event-map event))
         (catch Exception e
           (log/error :msg "Error indexing event" :event event :exception e)))
       (recur)))))

(defrecord Recorder [database kafka-consumer-config
                     commands-topic commands-partition commands-ch
                     events-topic  events-partition  events-ch]
  component/Lifecycle
  (start [this]
    (let [events-offset   (database/find-latest-events-offset database events-topic events-partition)
          commands-offset (database/find-latest-commands-offset database commands-topic commands-partition)
          commands-ch     (kafka/partition-topic-consumer-ch kafka-consumer-config
                                                             commands-topic
                                                             commands-partition)
          events-ch      (kafka/partition-topic-consumer-ch kafka-consumer-config
                                                            events-topic
                                                            events-partition)]
      (record-commands! database commands-ch)
      (record-events!   database events-ch)
      (assoc this
             :commands-ch commands-ch
             :events-ch   events-ch)))
  (stop [this]
    (when events-ch (a/close! events-ch))
    (when commands-ch (a/close! commands-ch))
    this))

(defn construct-recorder
  [config]
  (map->Recorder
   (select-keys config [:kafka-consumer-config
                        :commands-topic :commands-partition
                        :events-topic   :events-partition])))
