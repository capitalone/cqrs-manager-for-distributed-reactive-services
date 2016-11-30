;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.indexer.system
  (:require [com.stuartsierra.component :as component]
            [meta-merge.core :refer [meta-merge]]
            [io.pedestal.log :as log]
            [com.capitalone.commander.index.jdbc :refer [construct-jdbc-db]]
            [com.capitalone.commander.log :refer [construct-consumer]]
            com.capitalone.commander.log.kafka
            com.capitalone.commander.log.kinesis
            [com.capitalone.commander.indexer.component.indexer :refer [construct-indexer]]))

(set! *warn-on-reflection* true)

(def base-config
  {})

(defn new-system [config]
  (let [config (meta-merge config base-config)]
    (log/info :msg "Creating system" :config config)
    (-> (component/system-map
         :consumer (construct-consumer (:log-consumer config))
         :index    (construct-jdbc-db (:index config))
         :indexer  (construct-indexer (:indexer config)))
        (component/system-using
         {:indexer {:index        :index
                    :log-consumer :consumer}}))))
