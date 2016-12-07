;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander
  (:require [clojure.spec :as s]))

(s/def ::action keyword?)
(s/def ::data (s/keys))
(s/def ::id uuid?)
(s/def ::timestamp (s/int-in 0 Long/MAX_VALUE))
(s/def ::topic string?)
(s/def ::partition (s/or :kafka   (s/int-in 0 Integer/MAX_VALUE)
                         :kinesis string?))
(s/def ::offset (s/or :kafka   (s/int-in 0 Long/MAX_VALUE)
                      :kinesis string?))
(s/def ::children (s/every uuid?))

;; TODO: event type/schema registry
(s/def ::command-params
  (s/keys :req-un [::action ::data]))

(s/def ::command (s/keys :req-un [::id ::action ::data ::timestamp ::topic ::partition ::offset]
                         :opt-un [::children]))

(s/def ::parent uuid?)
(s/def ::event (s/merge ::command (s/keys :req-un [::parent])))
