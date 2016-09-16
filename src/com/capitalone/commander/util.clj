;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.util
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [com.capitalone.clojure.runtime :as runtime])
  (:import [java.nio ByteBuffer]))

(set! *warn-on-reflection* true)

;; HT: https://github.com/ztellman/byte-streams
(defn buf->bytes
  [^ByteBuffer buf]
  (if (.hasArray buf)
    (if (== (alength (.array buf)) (.remaining buf))
      (.array buf)
      (let [ary (byte-array (.remaining buf))]
        (doto buf
          .mark
          (.get ary 0 (.remaining buf))
          .reset)
        ary))
    (let [^bytes ary (byte-array (.remaining buf))]
      (doto buf .mark (.get ary) .reset)
      ary)))

(defn run-system!
  [system]
  (runtime/set-default-uncaught-exception-handler!
   (fn [thread e]
     (log/error :message "Uncaught exception, system exiting." :exception e :thread thread)
     (System/exit 1)))
  (runtime/add-shutdown-hook! ::stop-system #(do (log/info :message "System exiting, running shutdown hooks.")
                                                 (component/stop system)))
  (component/start system)
  @(promise))
