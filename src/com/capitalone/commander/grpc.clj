;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.
(ns com.capitalone.commander.grpc
  (:require [clojure.core.async :as a]
            [io.pedestal.log :as log]
            [com.stuartsierra.component :as c]
            [com.capitalone.commander.util :as util]
            [com.capitalone.commander.api :as api])
  (:import java.util.UUID
           [com.capitalone.commander.grpc
            CommanderProtos
            CommanderProtos$UUID
            CommanderProtos$CommandParams
            CommanderProtos$Command
            CommanderProtos$PagedCommands
            CommanderProtos$Event
            CommanderProtos$PagedEvents
            CommanderProtos$PagingInfo
            CommanderProtos$StreamRequest]
           [io.grpc Server ServerBuilder]
           io.grpc.stub.StreamObserver
           [com.google.protobuf Struct Value Value$KindCase ListValue NullValue]))

(set! *warn-on-reflection* true)

(defn ^CommanderProtos$UUID uuid->proto
  [uuid]
  (-> (CommanderProtos$UUID/newBuilder)
      (.setValue (str uuid))
      .build))

(declare map->struct)
(defn ^Value value->proto
  [value]
  (let [builder (Value/newBuilder)
        builder (cond
                  (boolean? value)    (.setBoolValue builder value)
                  (sequential? value) (->> value
                                           (map value->proto)
                                           (.addAllValues (ListValue/newBuilder))
                                           (.setListValue builder))
                  (nil? value)        (.setNullValue builder NullValue/NULL_VALUE)
                  (number? value)     (.setNumberValue builder (double value))
                  (string? value)     (.setStringValue builder value)
                  (map? value)        (.setStructValue builder (map->struct value))
                  :else               builder)]
    (.build builder)))

(declare struct->map)
(defn proto->value
  [^Value value]
  (condp = (.getKindCase value)
    Value$KindCase/BOOL_VALUE   (.getBoolValue value)
    Value$KindCase/LIST_VALUE   (->> value
                                     .getListValue
                                     .getValuesList
                                     (mapv proto->value))
    Value$KindCase/NULL_VALUE   nil
    Value$KindCase/NUMBER_VALUE (.getNumberValue value)
    Value$KindCase/STRING_VALUE (.getStringValue value)
    Value$KindCase/STRUCT_VALUE (-> value .getStructValue struct->map)
    value))

(defn struct->map
  [^Struct struct]
  (reduce (fn [agg [k v]]
            (assoc agg (keyword k) (proto->value v)))
          {}
          (.getFieldsMap struct)))

(defn ^Struct map->struct
  [m]
  (.build
   (reduce (fn [builder [k v]]
             (.putFields builder
                         (util/keyword->string k)
                         (value->proto v)))
           (Struct/newBuilder)
           m)))

(defn ^CommanderProtos$Command command->proto
  [command]
  (log/debug ::command->proto [command])
  (let [builder (-> (CommanderProtos$Command/newBuilder)
                    (.setId        (-> command :id uuid->proto))
                    (.setAction    (-> command :action util/keyword->string))
                    (.setData      (-> command :data map->struct))
                    (.setTimestamp (-> command :timestamp))
                    (.setTopic     (-> command :topic))
                    (.setPartition (-> command :partition))
                    (.setOffset    (-> command :offset)))]
    (doseq [id (:children command)]
      (.addChildren builder (uuid->proto id)))
    (.build builder)))

(defn ^CommanderProtos$Event event->proto
  [event]
  (log/debug ::event->proto [event])
  (-> (CommanderProtos$Event/newBuilder)
      (.setId        (-> event :id uuid->proto))
      (.setParent    (-> event :parent uuid->proto))
      (.setAction    (-> event :action util/keyword->string))
      (.setData      (-> event :data map->struct))
      (.setTimestamp (-> event :timestamp))
      (.setTopic     (-> event :topic))
      (.setPartition (-> event :partition))
      (.setOffset    (-> event :offset))
      .build))

(defn make-service
  [api]
  (proxy [com.capitalone.commander.grpc.CommanderGrpc$CommanderImplBase] []
    (createCommand [^CommanderProtos$CommandParams request
                    ^StreamObserver response]
      (let [action  (.getAction request)
            data    (.getData request)
            sync    (.getSync request)
            command (api/create-command api
                                        {:action (keyword action)
                                         :data   (struct->map data)}
                                        sync)
            builder (CommanderProtos$Command/newBuilder)]
        (.onNext response (command->proto command))
        (.onCompleted response)))
    (listCommands [^CommanderProtos$PagingInfo request
                   ^StreamObserver response]
      (let [limit          (.getLimit request)
            offset         (.getOffset request)
            paged-commands (api/list-commands api limit offset)
            builder        (-> (CommanderProtos$PagedCommands/newBuilder)
                               (.setLimit (:limit paged-commands))
                               (.setOffset (:offset paged-commands))
                               (.setTotal (:total paged-commands)))]
        (doseq [command (:commands paged-commands)]
          (.addCommands builder (command->proto command)))
        (.onNext response (.build builder))
        (.onCompleted response)))
    (commandById [^CommanderProtos$UUID request
                  ^StreamObserver response]
      (some->> request
               .getValue
               UUID/fromString
               (api/get-command-by-id api)
               command->proto
               (.onNext response))
      (.onCompleted response))
    (commandStream [^CommanderProtos$StreamRequest request
                    ^StreamObserver response]
      (let [ch (api/commands-ch api)]
        (a/go-loop []
          (if-some [command (a/<! ch)]
            (do (.onNext response (command->proto command))
                (recur))
            (do (.onCompleted response)
                :done)))))

    (listEvents [^CommanderProtos$PagingInfo request
                 ^StreamObserver response]
      (let [limit          (.getLimit request)
            offset         (.getOffset request)
            paged-events (api/list-events api limit offset)
            builder        (-> (CommanderProtos$PagedEvents/newBuilder)
                               (.setLimit (:limit paged-events))
                               (.setOffset (:offset paged-events))
                               (.setTotal (:total paged-events)))]
        (doseq [event (:events paged-events)]
          (.addEvents builder (event->proto event)))
        (.onNext response (.build builder))
        (.onCompleted response)))
    (eventById [^CommanderProtos$UUID request
                ^StreamObserver response]
      (some->> request
               .getValue
               UUID/fromString
               (api/get-event-by-id api)
               event->proto
               (.onNext response)))
    (eventStream [^CommanderProtos$StreamRequest request
                  ^StreamObserver response]
      (let [ch (api/events-ch api)]
        (a/go-loop []
          (if-some [event (a/<! ch)]
            (do (.onNext response (event->proto event))
                (recur))
            (do (.onCompleted response)
                :done)))))))

(defrecord GrpcServer [api port ^Server server]
  c/Lifecycle
  (start [this]
    (log/info ::GrpcServer :start :args [this])
    (let [builder (ServerBuilder/forPort port)
          service (make-service api)
          _       (.addService builder service)
          server  (.build builder)]
      (.start server)
      (assoc this :server server)))
  (stop  [this]
    (log/info ::GrpcServer :stop :args [this])
    (when server (.shutdown server))
    (dissoc this :server)))

(defn construct-grpc-server
  [{:keys [port] :as config}]
  (map->GrpcServer {:port port}))

(comment

  (require '[com.capitalone.commander.grpc :as grpc])

  (def client (com.capitalone.commander.grpc.CommanderGrpc/newBlockingStub
               (-> (io.grpc.ManagedChannelBuilder/forAddress "localhost" (int 8980))
                   (.usePlaintext true)
                   .build)))

  (.createCommand client (-> (com.capitalone.commander.grpc.CommanderProtos$CommandParams/newBuilder)
                             (.setAction "create-customer")
                             (.setData (grpc/map->struct {:first_name "Foo"
                                                          :last_name  "Bar"
                                                          :address    {:street_number "1234"
                                                                       :street_name   "Main St."
                                                                       :city          "Anytown"
                                                                       :state         "NH"
                                                                       :zip           "03755"}}))
                             .build))

  (.listCommands client (-> (com.capitalone.commander.grpc.CommanderProtos$PagingInfo/newBuilder)
                            (.setLimit 10)
                            .build))

  (.commandById client (-> (com.capitalone.commander.grpc.CommanderProtos$UUID/newBuilder)
                           (.setValue "8f599a70-821f-11e6-8fff-2063287b86c9")
                           .build))

  (.commandStream client (.build (com.capitalone.commander.grpc.CommanderProtos$StreamRequest/newBuilder)))

  (.listEvents client (-> (com.capitalone.commander.grpc.CommanderProtos$PagingInfo/newBuilder)
                          (.setLimit 10)
                          .build))

  (.eventById client (-> (com.capitalone.commander.grpc.CommanderProtos$UUID/newBuilder)
                         (.setValue "8f599a70-821f-11e6-8fff-2063287b86c9")
                         .build))

  (def event-stream
    (.eventStream client
                  (.build
                    (com.capitalone.commander.grpc.CommanderProtos$StreamRequest/newBuilder))))

  )
