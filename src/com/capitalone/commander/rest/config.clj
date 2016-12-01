;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.rest.config
  (:require [environ.core :refer [env]]))

(set! *warn-on-reflection* true)

(def defaults
  {:http         {:port          3000
                  :resource-path "/public"}
   :grpc         {:port 8980}
   :api          {:commands-topic  "commands"
                  :events-topic    "events"
                  :sync-timeout-ms 5000}
   :index        {:type :jdbc}
   :log-producer {:type       :kafka
                  :timeout-ms 2000}
   :log-consumer {:type      :kafka
                  :client-id "commander-rest-consumer"}})

(def environ
  {:http         {:port (some-> env ^String (:port) Integer.)}
   :grpc         {:port (some-> env ^String (:grpc-port) Integer.)}
   :api          {:commands-topic  (:commands-topic env)
                  :events-topic    (:events-topic env)
                  :sync-timeout-ms (some-> env ^String (:sync-timeout-ms) Integer.)}
   :log-consumer {:type     (some-> env :log-type keyword)
                  :servers  (:kafka-servers env)
                  :group-id (:rest-group-id env)}
   :index        {:type    (some-> env :index-type keyword)
                  :connection-uri (:database-uri env)}
   :log-producer {:type    (some-> env :log-type keyword)
                  :servers (:kafka-servers env)}})
