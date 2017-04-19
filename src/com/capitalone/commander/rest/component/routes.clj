;
; Copyright 2016 Capital One Services, LLC
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and limitations under the License.
;
; SPDX-Copyright: Copyright (c) Capital One Services, LLC
; SPDX-License-Identifier: Apache-2.0
;

(ns com.capitalone.commander.rest.component.routes
  (:require [com.stuartsierra.component :as c]
            [schema.core :as s]
            [io.pedestal.log :as log]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor.helpers :refer [handler]]
            [route-swagger.doc :as doc]
            [pedestal-api.core :as api]))

(def commander-doc
  {:info {:title       "Commander"
          :description "Manage Commands for a CQRS-in-the-large via REST system."
          :version     "2.0"}
   :tags [{:name         "commands"
           :description  "Create and inspect commands (in the CQRS sense)."
           :externalDocs {:description "See the Commander source code"
                          :url         "https://github.kdc.capitalone.com/commander/commander"}}
          {:name         "events"
           :description  "Inspect events (in the CQRS sense) created by business logic command handlers."
           :externalDocs {:description "See the Commander source code"
                          :url         "https://github.kdc.capitalone.com/commander/commander"}}]})

(def health-check
  (handler
   ::health-check
   (fn [request]
     {:status 200 :body "healthy"})))

(defn build-routes
  [rest-endpoints doc]
  (log/debug :fn ::build-routes :rest-endpoints rest-endpoints :doc doc)
  (s/with-fn-validation
    (-> [[(conj rest-endpoints
                ["/health" {:get health-check}]
                ["/swagger.json" ^:interceptors [(api/negotiate-response)]
                 {:get api/swagger-json}]
                ["/ui/*resource" {:get api/swagger-ui}])]]
        route/expand-routes
        (doc/with-swagger doc))))

(defrecord Routes [rest-endpoints routes]
  c/Lifecycle
  (start [this]
    (assoc this :routes (build-routes (:routes rest-endpoints) commander-doc)))
  (stop  [this] (dissoc this :routes)))

(defn construct-routes
  []
  (map->Routes {}))
