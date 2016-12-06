;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.index.dynamodb
  (:require [clojure.spec :as s]
            [clojure.data.fressian :as fressian]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [com.capitalone.commander.util :as util]
            [com.capitalone.commander.index :as index]
            [clojure.walk :as walk])
  (:import [com.amazonaws.services.dynamodbv2
            AmazonDynamoDBAsync
            AmazonDynamoDBAsyncClient]
           [com.amazonaws.services.dynamodbv2.model
            AttributeValue
            GetItemRequest
            PutRequest WriteRequest
            ScanRequest]
           [java.util UUID]))

(set! *warn-on-reflection* true)

(defn event-from-select
  [event-map]
  (reduce (fn [agg [^String k ^AttributeValue v]]
            (if (= k "command")
              agg
              (assoc agg
                (keyword k) (case k
                              "id" (-> v .getS UUID/fromString)
                              "action" (-> v .getS keyword)
                              "data" (-> v .getB fressian/read)
                              "timestamp" (.getN v)
                              "topic" (.getS v)
                              "partition" (.getS v)
                              "offset" (.getS v)
                              "parent" (-> v .getS UUID/fromString)
                              v))))
          {}
          event-map))

(defn command-from-select
  [command-map]
  (-> command-map
      event-from-select
      (update-in [:children]
                 #(map (fn [^AttributeValue child]
                         (-> child .getS UUID/fromString))
                       (.getL ^AttributeValue %)))))

(defn- event-for-insert
  [event]
  (reduce (fn [agg [k v]]
            (assoc agg
              (name k)
              (cond
                (= k :action)
                (-> v util/keyword->string AttributeValue.)

                (= k :data)
                (doto (AttributeValue.)
                  (.setB (fressian/write v :footer? true)))

                (= k :children)
                (doto (AttributeValue.)
                  (.setL (map #(AttributeValue. (str %)) v)))

                (uuid? v)   (AttributeValue. (str v))
                (string? v) (AttributeValue. (str v))
                (number? v) (doto (AttributeValue.)
                              (.setN (str v))))))
          {"command" (doto (AttributeValue.)
                       (.setBOOL false))}
          event))

(defn ^WriteRequest put-write-request
  [^java.util.Map mp]
  (-> mp
      PutRequest.
      WriteRequest.))

(defn- command-for-insert
  [command]
  (-> command
      event-for-insert
      (assoc "command" (doto (AttributeValue.)
                         (.setBOOL true)))))

(defrecord DynamoDBIndex [^String table-name ^AmazonDynamoDBAsync client]
  component/Lifecycle
  (start [component]
    (assoc component :client (AmazonDynamoDBAsyncClient.)))
  (stop [component]
    (when client (.shutdown client))
    (dissoc component :client))

  index/CommonDataAccess
  (-find-latest-partition-offset [index topic part]
    (some-> client
            (.getItem (GetItemRequest. table-name
                                       {"topic"     (AttributeValue. ^String topic)
                                        "partition" (AttributeValue. ^String part)}
                                       true))
            .getItem
            ^AttributeValue (get "offset")
            .getS))

  index/CommandDataAccess
  (-fetch-commands [index limit offset]
    (let [request (doto (ScanRequest. table-name)
                    (.setFilterExpression "#c = :c")
                    (.setExpressionAttributeNames {"#c" "command"})
                    (.setExpressionAttributeValues {":c" (doto (AttributeValue.)
                                                           (.setBOOL true))}))]
      (when offset
        (.addExclusiveStartKeyEntry request "offset" (AttributeValue. (str offset))))
      (.setLimit request (int limit))
      {:commands (->> request
                      (.scan client)
                      .getItems
                      (map command-from-select))
       :limit    limit
       :offset   offset
       :total    (-> client (.describeTable table-name) .getTable .getItemCount)})) ;; TODO: remove this during implementation of https://github.com/capitalone/cqrs-manager-for-distributed-reactive-services/issues/11
  (-fetch-command-by-id [index id]
    (some-> client
            (.getItem (GetItemRequest. table-name
                                       {"id"      (AttributeValue. (str id))
                                        "command" (.withBOOL (AttributeValue.) true)}
                                       true))
            .getItem
            command-from-select))
  (-insert-commands! [index commands]
    (some-> client
            (.batchWriteItem {table-name (map (comp put-write-request
                                                    command-for-insert)
                                              commands)})
            .getUnprocessedItems
            empty?))

  index/EventDataAccess
  (-fetch-events [index limit offset]
    (let [request (doto (ScanRequest. table-name)
                    (.setFilterExpression "#c = :c")
                    (.setExpressionAttributeNames {"#c" "command"})
                    (.setExpressionAttributeValues {":c" (doto (AttributeValue.)
                                                           (.setBOOL false))}))]
      (when offset
        (.addExclusiveStartKeyEntry request "offset" (AttributeValue. (str offset))))
      (.setLimit request (int limit))
      {:events (->> request
                    (.scan client)
                    .getItems
                    (map event-from-select))
       :limit  limit
       :offset offset
       :total  (-> client (.describeTable table-name) .getTable .getItemCount)})) ;; TODO: remove this during implementation of https://github.com/capitalone/cqrs-manager-for-distributed-reactive-services/issues/11
  (-fetch-event-by-id [index id]
    (some-> client
            (.getItem (GetItemRequest. table-name
                                       {"id"      (AttributeValue. (str id))
                                        "command" (.withBOOL (AttributeValue.) false)}
                                       true)) ;; TODO: evaluate the need for consistent read here. It's probably necessary since involved with consumer offset handling
            .getItem
            event-from-select))
  (-insert-events! [index events]
    (some-> client
            (.batchWriteItem {table-name (map (comp event-for-insert
                                                    put-write-request)
                                              events)})
            .getUnprocessedItems
            empty?)))

(defmethod index/construct-index :dynamodb
  [config]
  (log/info ::index/construct-index :dynamodb :config config)
  (map->DynamoDBIndex {:table-name (:table-name config)}))
