;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

;; Inspired by duct: https://github.com/weavejester/duct
;; Inspired by Stuart Sierra: https://stuartsierra.com/2015/05/27/clojure-uncaught-exceptions
(ns com.capitalone.clojure.runtime)

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
