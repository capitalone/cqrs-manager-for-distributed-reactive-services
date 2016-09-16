;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.indexer.config
  (:require [environ.core :refer [env]]))

(set! *warn-on-reflection* true)

(def defaults
  {:indexer {:commands-topic     "commands"
              :commands-partition 0
              :events-topic       "events"
              :events-partition   0}})

(def environ
  {:indexer  {:commands-topic        (:commands-topic env)
              :commands-partition    (:commands-partition env)
              :events-topic          (:events-topic env)
              :events-partition      (:events-partition env)
              :kafka-consumer-config {"bootstrap.servers" (:kafka-servers env)
                                      "group.id"          (:indexer-group-id env)}}
   :database {:connection-uri (:database-uri env)}})
