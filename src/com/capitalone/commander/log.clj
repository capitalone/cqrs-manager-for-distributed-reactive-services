;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.log
  (:require [clojure.spec :as s]
            [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as p]))

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

(s/def ::ReadPort  #(satisfies? p/ReadPort %))
(s/def ::WritePort #(satisfies? p/WritePort %))
(s/def ::Channel   #(satisfies? p/Channel %))

(s/def ::record-metadata (s/keys :req-un [:com.capitalone.commander/topic
                                          :com.capitalone.commander/partition
                                          :com.capitalone.commander/offset
                                          :com.capitalone.commander/timestamp]))

(s/def ::key :com.capitalone.commander/id)
(s/def ::value any?)

(s/def ::offset (s/nilable :com.capitalone.commander/offset))

(s/def ::producer-record
  (s/keys :req-un [:com.capitalone.commander/topic ::value]
          :opt-un [::key :com.capitalone.commander/partition ::offset]))

(s/fdef send!
        :args (s/cat :producer #(satisfies? EventProducer %)
                     :record   ::producer-record
                     :ch       (s/? ::WritePort))
        :ret  ::ReadPort
        :fn   #(= (-> % :args :ch) (-> % :ret)))

(defmulti construct-producer "Builds a Log producer according to the :type key in the given config map" :type)

;; TODO: specs for consumer
(defprotocol EventConsumer
  (-consume-onto-channel! [this topics index channel timeout]
    "Initialize this consumer, subscribing to the given list of
    topics. If index is nil, consumer will begin with latest values in
    each partition/shard assigned to this consumer.  If index is
    non-nil, consumer position is looked up in index per
    topic/partition assigned to this consumer. Finally, consumes
    records from the consumer (polling every `timeout` ms, if
    applicable to event source) and conveys them on the channel.
    Returns the given channel."))

(defn consume-onto-channel!
  "Initialize this consumer, subscribing to the given list of
  topics. If index is nil, consumer will begin with latest values in
  each partition/shard assigned to this consumer.  If index is
  non-nil, consumer position is looked up in index per topic/partition
  assigned to this consumer. Finally, consumes records from the
  consumer (polling every `timeout` ms, if applicable to event source)
  and conveys them on the channel.  Returns the given channel."
  ([consumer topics channel]
   (consume-onto-channel! consumer topics nil channel))
  ([consumer topics index channel]
   (consume-onto-channel! consumer topics index channel 10000))
  ([consumer topics index channel timeout]
   (-consume-onto-channel! consumer topics index channel timeout)
   channel))

(defmulti construct-consumer "Builds a Log consumer according to the :type key in the given config map" :type)
