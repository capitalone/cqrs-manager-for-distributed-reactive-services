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
  {:indexer      {:commands-topic "commands"
                  :events-topic   "events"}
   :log-consumer {:type      :kafka
                  :client-id "commander-indexer-consumer"}})

(def environ
  {:indexer      {:commands-topic (:commands-topic env)
                  :events-topic   (:events-topic env)}
   :log-consumer {:type     (some-> env :log-type keyword)
                  :servers  (:kafka-servers env)
                  :group-id (:indexer-group-id env)}
   :index        {:connection-uri (:database-uri env)}})
