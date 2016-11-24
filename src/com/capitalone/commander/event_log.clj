;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.event-log
  (:require [clojure.core.async :as a]))

(set! *warn-on-reflection* true)

(defprotocol EventProducer
  (-send! [this record result-ch]
    "Sends a single record to the Event Log.
    Returns result-ch, which will convey record metadata."))

(defn send!
  "Writes a single record to the Event Log"
  ([producer record]
   (send! producer record (a/promise-chan)))
  ([producer record result-ch]
   (-send! producer record result-ch)))

(s/def ::record-metadata (s/keys :req-un [:com.capitalone.commander/topic
                                          :com.capitalone.commander/partition
                                          :com.capitalone.commander/offset
                                          :com.capitalone.commander/timestamp]))

(s/def ::key :com.capitalone.commander/id)
(s/def ::value any?)

(s/def ::producer-record
  (s/keys :req-un [:com.capitalone.commander/topic ::value]
          :opt-un [::key :com.capitalone.commander/partition]))

(s/fdef send!
        :args (s/cat :producer #(instance? EventProducer %)
                     :record   ::producer-record
                     :ch       (s/? ::WritePort))
        :ret  ::ReadPort
        :fn   #(= (-> % :args :ch) (-> % :ret)))

;; TODO: specs for consumer

(defprotocol EventConsumer
  (-subscribe! [this topics]
    "Initialize this consumer, subscribing to each of the
    topics. Return value isn't meaningful, this function is executed
    for side effects.")

  (-consume-onto-channel [this channel timeout]
    "Consumes records from the consumer (polling every `timeout` ms)
    and conveys them on the channel"))

(defn subscribe!
  "Initialize this consumer, subscribing to each of the
  topics. Return value isn't meaningful, this function is executed
  for side effects."
  [consumer topics]
  (-subscribe! consumer topics))

(defn consume-onto-channel
  "Consumes records from the consumer and conveys them on the channel.  Returns the channel."
  ([consumer channel]
   (consume-onto-channel consumer channel 10000))
  ([consumer channel timeout]
   (-consume-onto-channel consumer channel timeout)
   channel))
