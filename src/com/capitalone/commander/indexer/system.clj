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

(ns com.capitalone.commander.indexer.system
  (:require [com.stuartsierra.component :as component]
            [meta-merge.core :refer [meta-merge]]
            [io.pedestal.log :as log]
            [com.capitalone.commander.database :refer [construct-jdbc-db]]
            [com.capitalone.commander.kafka :refer [construct-consumer]]
            [com.capitalone.commander.indexer.component.indexer :refer [construct-indexer]]))

(set! *warn-on-reflection* true)

(def base-config
  {:indexer {:kafka-consumer-config {"enable.auto.commit" false}}})

(defn new-system [config]
  (let [config (meta-merge config base-config)]
    (log/info :msg "Creating system" :config config)
    (-> (component/system-map
         :consumer (construct-consumer (:kafka-consumer config))
         :database (construct-jdbc-db (:database config))
         :indexer  (construct-indexer (:indexer config)))
        (component/system-using
          {:indexer {:database       :database
                     :kafka-consumer :consumer}}))))
