;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.log.kinesis
  (:refer-clojure :exclude [partition])
  (:require [clojure.spec :as s]
            [clojure.core.async :as a]
            [clojure.data.fressian :as fressian]
            [io.pedestal.log :as log]
            [com.stuartsierra.component :as c]
            [com.capitalone.commander.log :as l]
            [clojure.string :as string])
  (:import [com.amazonaws.services.kinesis
            AmazonKinesisAsync
            AmazonKinesisAsyncClient]
           [com.amazonaws.services.kinesis.model
            PutRecordRequest
            PutRecordResult
            Record]
           [com.amazonaws.handlers AsyncHandler]
           [com.amazonaws.services.kinesis.clientlibrary.interfaces.v2
            IRecordProcessorFactory
            IRecordProcessor]
           [com.amazonaws.services.kinesis.clientlibrary.types
            ShutdownInput
            ProcessRecordsInput
            InitializationInput]
           [com.amazonaws.services.kinesis.clientlibrary.lib.worker KinesisClientLibConfiguration ShutdownReason InitialPositionInStream Worker Worker$Builder]
           [java.net InetAddress]
           [java.util UUID]
           [com.amazonaws.services.kinesis.clientlibrary.interfaces IRecordProcessorCheckpointer]
           [com.amazonaws.auth DefaultAWSCredentialsProviderChain]
           [com.amazonaws.services.kinesis.clientlibrary.exceptions ShutdownException ThrottlingException]))

(set! *warn-on-reflection* true)

(defn- ^PutRecordRequest producer-record
  "Constructs a ProducerRecord from a map conforming to
  ProducerRecordSchema."
  [record]
  (let [{:keys [topic value key partition offset]} record]
    (doto (PutRecordRequest.)
      (.setStreamName (str topic))
      (.setPartitionKey (when key (str key)))
      (.setExplicitHashKey (when partition (str partition)))
      (.setSequenceNumberForOrdering offset)
      (.setData (fressian/write value :footer? true)))))

(s/fdef producer-record
        :args (s/cat :record ::l/producer-record)
        :ret #(instance? PutRecordRequest %))

(defn- shard-id->partition
  [shard-id]
  (string/replace-first shard-id "shardId-" ""))

(defrecord ProducerComponent [^AmazonKinesisAsync producer last-offsets]
  c/Lifecycle
  (start [this]
    (assoc this :producer (AmazonKinesisAsyncClient.)
                :last-offsets (atom {})))
  (stop  [this]
    (when producer (.shutdown producer))
    (dissoc this :producer :last-offsets))

  l/EventProducer
  (-send! [this record result-ch]
    (let [{:keys [topic value key partition offset]} record]
      (.putRecordAsync producer
                       (producer-record (assoc record :offset (get @last-offsets topic)))
                       (reify AsyncHandler
                         (onSuccess [_ _ result]
                           (let [sequence-number (.getSequenceNumber ^PutRecordResult result)]
                             (swap! last-offsets assoc topic sequence-number)
                             (a/put! result-ch {:offset    sequence-number
                                                :partition (-> ^PutRecordResult result .getShardId shard-id->partition)
                                                :topic     topic
                                                :timestamp (System/currentTimeMillis)})))
                         (onError [_ t]
                           (log/error :exception t)))))
    result-ch))

(defmethod l/construct-producer :kinesis
  [producer-config]
  (log/info ::l/construct-producer :kinesis
            :config producer-config)
  (map->ProducerComponent {}))

(def NUM_RETRIES 10)
(def BACKOFF_TIME_MS 3000)

;; TODO
(defn- checkpoint [^IRecordProcessorCheckpointer checkpointer shard-id]
  (log/info ::checkpoint shard-id)
  (loop [i 0]
    (let [needs-retry? (and (< i NUM_RETRIES)
                            (try
                              (.checkpoint checkpointer)
                              false
                              (catch ShutdownException e
                                (log/info :msg "Shutting down interrupted checkpointing"
                                          :exception e)
                                false)
                              (catch IllegalStateException e
                                (log/error :msg "Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library."
                                           :exception e)
                                false)
                              (catch ThrottlingException e
                                (if (>= i (dec NUM_RETRIES))
                                  (do
                                    (log/error :msg "Checkpoint failed"
                                               :failed-attempts (inc i)
                                               :total-retries NUM_RETRIES
                                               :exception e)
                                    false)
                                  (do
                                    (log/info :msg "Transient issue when checkpointing"
                                              :failed-attempts (inc i)
                                              :total-retries NUM_RETRIES
                                              :exception e)
                                    true)))))]
      (when needs-retry?
        (try (Thread/sleep BACKOFF_TIME_MS)
             (catch InterruptedException e
               (log/error :msg "Interrupted sleep"
                          :exception e)))
        (recur (inc i))))))

(def CHECKPOINT_INTERVAL_MILLIS 60000)

(deftype Consumer [^:volatile-mutable shard-id ^:volatile-mutable next-checkpoint-msec stream channel]
  IRecordProcessor
  (^void initialize [this ^InitializationInput input]
    (let [shard (.getShardId input)]
      (log/info :msg "Initializing record processor for shard"
                :stream stream
                :shard shard
                :channel channel)
      (set! shard-id shard)))

  ;; TODO: retry with backoff
  (^void processRecords [this ^ProcessRecordsInput input]
    (let [records (.getRecords input)]
      (log/info :msg "Processing records from shard"
                :stream stream
                :shard shard-id
                :record-count (count records))
      (doseq [^Record record records]
        (try
          (let [record-map {:key       (UUID/fromString (.getPartitionKey record))
                            :value     (-> record .getData fressian/read)
                            :topic     stream
                            :partition (shard-id->partition shard-id)
                            :offset    (.getSequenceNumber record)
                            :timestamp (-> record .getApproximateArrivalTimestamp .getTime)}]
            (log/debug :msg "Sending record map on channel..."
                       :channel channel
                       :record-map record-map)
            (a/put! channel record-map)
            (log/debug :msg "record sent"
                       :channel channel
                       :record-map record-map))
          (catch Throwable t
            (log/error :msg "Error when processing record"
                       :exception t
                       :record record
                       :mediation :skipping))))

      ;; Checkpoint once every checkpoint interval.
      (when (> (System/currentTimeMillis) next-checkpoint-msec)
        (log/info :msg "About to checkpoint" :stream stream :shard-id shard-id)
        (checkpoint (.getCheckpointer input) shard-id)
        (set! next-checkpoint-msec (+ (System/currentTimeMillis) CHECKPOINT_INTERVAL_MILLIS)))))

  (^void shutdown [this ^ShutdownInput input]
    (log/info :msg "Shutting down" :shard shard-id)
    (when (= (.getShutdownReason input) ShutdownReason/TERMINATE)
      (-> input .getCheckpointer (checkpoint shard-id)))))

(defrecord RecordFactory [stream channel]
  IRecordProcessorFactory
  (createProcessor [this]
    (log/info :msg "Creating Processor" :this this)
    (Consumer. nil 0 stream channel)))

(defrecord ConsumerComponent [^String client-id ^String application-name workers]
  c/Lifecycle
  (start [this]
    (java.security.Security/setProperty "networkaddress.cache.ttl" "60")
    (assoc this :workers (atom #{})))
  (stop [this]
    (doseq [^Worker worker @workers]
      @(.requestShutdown worker))
    (dissoc this :workers))

  ;; TODO: manage offsets manually within index (if present), rather than by checkpoint-per-time-period
  l/EventConsumer
  (-consume-onto-channel! [this topics index ch timeout]
    (log/info ::-consume-onto-channel! [this topics index ch timeout])
    (doseq [topic topics]
      (log/info :phase :worker-config
                :application-name application-name
                :stream topic
                :client-id client-id)
      (let [config (KinesisClientLibConfiguration. application-name
                                                   topic
                                                   (DefaultAWSCredentialsProviderChain.)
                                                   (str topic ":" client-id))
            worker (-> (Worker$Builder.)
                       (.recordProcessorFactory (RecordFactory. topic ch))
                       (.config (if index
                                  (.withInitialPositionInStream config InitialPositionInStream/TRIM_HORIZON)
                                  (.withInitialPositionInStream config InitialPositionInStream/LATEST)))
                       .build)]
        (swap! workers conj worker)
        (a/thread
          (try
            (.run worker)
            (catch Throwable t
              (log/error :phase ::worker-processing :exception t)
              (throw t))))))
    ch))

;; TODO: spec for consumer-config, assert values conform prior to constructing ConsumerComponent
(defmethod l/construct-consumer :kinesis
  [{:keys [client-id group-id]
    :as consumer-config}]
  (log/info ::l/construct-consumer :kinesis
            :config consumer-config)
  (assert group-id)
  (map->ConsumerComponent {:client-id        (str client-id
                                                  ":"
                                                  (.getCanonicalHostName (InetAddress/getLocalHost))
                                                  ":"
                                                  (UUID/randomUUID))
                           :application-name group-id}))
