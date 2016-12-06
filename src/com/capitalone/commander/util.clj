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
            [io.pedestal.log :as log])
  (:import [java.nio ByteBuffer]))

(set! *warn-on-reflection* true)

(def ^:private shutdown-hooks (atom {}))

(defonce ^:private init-shutdown-hook
  (delay (.addShutdownHook (Runtime/getRuntime)
                           (Thread.
                            #(doseq [f (vals @shutdown-hooks)]
                               (f))))))

(defn add-shutdown-hook! [k f]
  @init-shutdown-hook
  (swap! shutdown-hooks assoc k f))

(defn remove-shutdown-hook! [k]
  (swap! shutdown-hooks dissoc k))

(defn set-default-uncaught-exception-handler!
  "Adds a exception handler, which is a 2-arity function of thread
  name and exception."
  [f]
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (f thread ex)))))

(defn unset-default-uncaught-exception-handler!
  []
  (Thread/setDefaultUncaughtExceptionHandler nil))

(defn ^String keyword->string
  [kw]
  (let [sb (StringBuffer.)]
    (when-let [ns (namespace kw)]
      (.append sb ns)
      (.append sb "/"))
    (.append sb (name kw))
    (str sb)))

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
  (set-default-uncaught-exception-handler!
   (fn [thread e]
     (log/error :message "Uncaught exception, system exiting." :exception e :thread thread)
     (System/exit 1)))
  (add-shutdown-hook! ::stop-system #(do (log/info :message "System exiting, running shutdown hooks.")
                                         (component/stop system)))
  (component/start system)
  @(promise))
