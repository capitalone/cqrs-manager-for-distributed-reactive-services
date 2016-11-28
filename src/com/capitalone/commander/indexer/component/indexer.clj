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
            [com.capitalone.commander.api :as api]
            [com.capitalone.commander.index :as index]
            [com.capitalone.commander.log :as l]))

(set! *warn-on-reflection* true)

(defn record-commands-and-events!
  "Records all commands and events arriving on ch to the given index
  component. Returns the go-loop channel that will convey :done when ch is closed."
  [index commands-topic events-topic ch]
  (log/debug ::record-events! [index commands-topic events-topic ch])
  (a/go-loop []
    (when-some [msg (a/<! ch)]
      (try
        (log/debug ::record-events! :msg :msg msg)
        (condp = (:topic msg)
          events-topic   (index/insert-events! index (api/event-map msg))
          commands-topic (index/insert-commands! index (api/command-map msg))
          (log/warn ::record-commands-and-events! "Unexpected topic and message"
                    :topic (:topic msg)
                    :msg msg))
        (catch Exception e
          (log/error :msg "Error indexing event" :msg msg :exception e)))
      (recur))))

(defrecord Indexer [index kafka-consumer commands-topic events-topic ch]
  component/Lifecycle
  (start [this]
    (let [ch (a/chan 1)
          topics [commands-topic events-topic]]
      ;; TODO: move this into c.c.c.log.kafka implementation
      (l/subscribe! kafka-consumer topics index)
      (l/consume-onto-channel! kafka-consumer ch)
      (record-commands-and-events! index commands-topic events-topic ch)
      (assoc this :ch ch)))
  (stop [this]
    (when ch (a/close! ch))
    (dissoc this :ch)))

(defn construct-indexer
  [config]
  (map->Indexer
   (select-keys config [:kafka-consumer-config :commands-topic :events-topic])))
