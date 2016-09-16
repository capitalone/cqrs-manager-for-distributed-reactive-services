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
  {:http           {:port          3000
                    :resource-path "/public"}
   :api            {:commands-topic  "commands"
                    :events-topic    "events"
                    :sync-timeout-ms 5000}
   :kafka-producer {:timeout-ms 2000}})

(def environ
  {:http           {:port (some-> env ^String (:port) Integer.)}
   :api            {:commands-topic        (:commands-topic env)
                    :events-topic          (:events-topic env)
                    :sync-timeout-ms       (some-> env ^String (:sync-timeout-ms) Integer.)
                    :kafka-consumer-config {"bootstrap.servers" (:kafka-servers env)
                                            "group.id"          (:rest-group-id env)}}
   :database       {:connection-uri (:database-uri env)}
   :kafka-producer {:servers (:kafka-servers env)}})
