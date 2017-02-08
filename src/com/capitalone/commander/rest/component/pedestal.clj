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

(ns com.capitalone.commander.rest.component.pedestal
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as pedestal]))

(set! *warn-on-reflection* true)

(defn pedestal-config
  [config]
  (into {}
        (for [[k v] config]
          [(keyword "io.pedestal.http" (name k)) v])))

(defrecord PedestalServer [routes]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [options (assoc (dissoc component :routes) :join? false)
            server (-> options
                       (assoc :routes (:routes routes))
                       pedestal-config
                       pedestal/create-server
                       pedestal/start)]
        (assoc component :server server))))
  (stop [component]
    (if-let [server (:server component)]
      (do (pedestal/stop server)
          (dissoc component :server))
      component)))

(defn construct-pedestal-server
  [options]
  (map->PedestalServer options))
