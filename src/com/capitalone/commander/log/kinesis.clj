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
  (:require [clojure.spec :as s]
            [clojure.core.async :as a]
            [clojure.data.fressian :as fressian]
            [com.stuartsierra.component :as c]
            [com.capitalone.commander.log :as l]
            [io.pedestal.log :as log])
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
           [com.amazonaws.services.kinesis.clientlibrary.lib.worker KinesisClientLibConfiguration ShutdownReason InitialPositionInStream Worker]
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
      (.setExplicitHashKey (str partition))
      (.setSequenceNumberForOrdering offset)
      (.setData (fressian/write value :footer? true)))))

(s/fdef producer-record
        :args (s/cat :record ::l/producer-record)
        :ret #(instance? PutRecordRequest %))

(defrecord ProducerComponent [^AmazonKinesisAsync producer last-offset]
  c/Lifecycle
  (start [this]
    (assoc this :producer (AmazonKinesisAsyncClient.)
                :last-offset (atom nil)))
  (stop  [this]
    (when producer (.shutdown producer))
    (dissoc this :producer))

  l/EventProducer
  (-send! [this record result-ch]
    (let [{:keys [topic value key partition offset]} record]
      (.putRecordAsync producer
                       (producer-record (assoc record :offset @last-offset))
                       (reify AsyncHandler
                         (onSuccess [_ _ result]
                           (a/put! result-ch {:offset    (.getSequenceNumber ^PutRecordResult result)
                                              :partition (.getShardId ^PutRecordResult result)
                                              :topic     (:topic record)
                                              :timestamp nil}))
                         (onError [_ t]
                           (log/error :exception t)))))
    result-ch))

(defmethod l/construct-producer :kinesis
  [producer-config]
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
                                (log/error :msg "Shutting down interrupted checkpointing"
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
      (log/info :msg "Initializing record processor for shard" :shard shard)
      (set! shard-id shard)))

  ;; TODO: retry with backoff
  (^void processRecords [this ^ProcessRecordsInput input]
    (let [records (.getRecords input)]
      (log/info :msg "Processing records from shard"
                :shard shard-id
                :record-count (count records))
      (doseq [^Record record records]
        (try
          (let [record-map {:key       (.getPartitionKey record)
                            :value     (-> record .getData fressian/read)
                            :topic     stream
                            :partition shard-id
                            :offset    (.getSequenceNumber record)
                            :timestamp (.getApproximateArrivalTimestamp record)}]
            (a/put! channel record-map))
          (catch Throwable t
            (log/error :msg "Error when processing record"
                       :exception t
                       :record record
                       :mediation :skipping))))

      ;; Checkpoint once every checkpoint interval.
      (when (> (System/currentTimeMillis) next-checkpoint-msec)
        (checkpoint (.getCheckpointer input) shard-id)
        (set! next-checkpoint-msec (+ (System/currentTimeMillis) CHECKPOINT_INTERVAL_MILLIS)))))

  (^void shutdown [this ^ShutdownInput input]
    (log/info :msg "Shutting down" :shard shard-id)
    (when (= (.getShutdownReason input) ShutdownReason/TERMINATE)
      (-> input .getCheckpointer (checkpoint shard-id)))))

(deftype RecordFactory [stream channel]
  IRecordProcessorFactory
  (createProcessor [this]
    (log/info :msg "Creating Processor" :this this)
    (Consumer. nil nil stream channel)))

(defrecord ConsumerComponent [^String worker-id ^String application-name consumers]
  c/Lifecycle
  (start [this]
    (java.security.Security/setProperty "networkaddress.cache.ttl" "60")
    this)
  (stop [this]
    this)

  ;; TODO: manage offsets manually within index (if present), rather than by checkpoint-per-time-period
  l/EventConsumer
  (-consume-onto-channel! [this topics index ch timeout]
    (log/info ::-consume-onto-channel! [this topics index ch timeout])
    (doseq [topic topics]
      (log/info :phase :worker-config
                :application-name application-name
                :stream topic
                :worker-id worker-id)
      (let [config (KinesisClientLibConfiguration. application-name
                                                   topic
                                                   (DefaultAWSCredentialsProviderChain.)
                                                   worker-id)
            worker (Worker. (RecordFactory. topic ch)
                            (if index
                              (.withInitialPositionInStream config InitialPositionInStream/TRIM_HORIZON)
                              (.withInitialPositionInStream config InitialPositionInStream/LATEST)))]
        (a/thread
          (try
            (.run worker)
            (catch Throwable t
              (log/error :phase ::worker-processing :exception t)
              (throw t))))))
    ch))

;; TODO: spec for consumer-config, assert values conform prior to constructing ConsumerComponent
(defmethod l/construct-consumer :kinesis
  [{:keys [worker-id group-id]
    :or   {worker-id (some-> (InetAddress/getLocalHost)
                             .getCanonicalHostName
                             (str ":" (UUID/randomUUID)))}
    :as consumer-config}]
  (assert group-id)
  (map->ConsumerComponent {:worker-id        worker-id
                           :application-name group-id}))
