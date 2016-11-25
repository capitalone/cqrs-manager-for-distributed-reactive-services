;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.rest.system
  (:require [com.stuartsierra.component :as component]
            [meta-merge.core :refer [meta-merge]]
            [io.pedestal.log :as log]
            [com.capitalone.commander.rest.endpoint.commander :refer [construct-commander-rest-endpoints]]
            [com.capitalone.commander.rest.component.routes :refer [construct-routes]]
            [com.capitalone.commander.rest.component.pedestal :refer [construct-pedestal-server]]
            [com.capitalone.commander.grpc :refer [construct-grpc-server]]
            [com.capitalone.commander.index.jdbc :refer [construct-jdbc-db]]
            [com.capitalone.commander.log.kafka :refer [construct-producer construct-consumer]]
            [com.capitalone.commander.api :refer [construct-commander-api]]))

(set! *warn-on-reflection* true)

(def base-config
  {:http {:env    :dev
          :router :linear-search
          :type   :jetty}})

(defn new-system [config]
  (let [config (meta-merge config base-config)]
    (log/info :msg "Creating system" :config config)
    (-> (component/system-map
         :rest-endpoints (construct-commander-rest-endpoints)
         :grpc-server    (construct-grpc-server (:grpc config))
         :http           (construct-pedestal-server (:http config))
         :routes         (construct-routes)
         :index          (construct-jdbc-db  (:index config))
         :log-consumer   (construct-consumer (:kafka-consumer config))
         :log-producer   (construct-producer (:kafka-producer config))
         :api            (construct-commander-api (:api config)))
        (component/system-using
         {:http           [:routes]
          :routes         [:rest-endpoints]
          :rest-endpoints [:api]
          :grpc-server    [:api]
          :api            [:log-producer :log-consumer :index]}))))
