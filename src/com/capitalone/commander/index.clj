;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.index
  (:require [clojure.spec :as s]
            [io.pedestal.log :as log]
            [com.capitalone.commander :as commander]))

(set! *warn-on-reflection* true)

(defprotocol CommonDataAccess
  (-find-latest-partition-offset [index topic partition]
    "Finds and returns the highest seen offset for the given topic partition."))

(s/def ::CommonDataAccess (partial satisfies? CommonDataAccess))

(defn find-latest-partition-offset
  [index topic partition]
  (log/debug ::find-latest-commands-offset [index topic partition])
  (-find-latest-partition-offset index topic partition))

(s/fdef find-latest-partition-offset
        :args (s/cat :index     ::CommonDataAccess
                     :topic     ::commander/topic
                     :partition ::commander/partition)
        :ret (s/nilable ::commander/offset))

(defprotocol CommandDataAccess
  (-fetch-commands [index limit offset]
    "Fetches commands from the given index component, returning a map of
      - :commands a vector of command maps
      - :limit the limit passed to the query
      - :offset the offset passed to the query
      - :total the total count of commands")
  (-fetch-command-by-id [index id]
    "Fetches and returns a single command from the given index component, identified by its UUID.")
  (-insert-commands! [index commands]
    "Inserts the given command maps into the given index component.
    Returns true if insert succeeded, false otherwise."))

(s/def ::CommandDataAccess (partial satisfies? CommandDataAccess))

(defn fetch-commands
  "Fetches commands from the given index component, returning a map of
    - :commands a vector of command maps
    - :limit the limit passed to the query
    - :offset the offset passed to the query
    - :total the total count of commands"
  ([index]
   (fetch-commands index 0 0))
  ([index limit offset]
   (log/debug ::fetch-commands [index limit offset])
   (let [limit  (or limit 0)
         offset (or offset 0)]
     (-fetch-commands index limit offset))))

(s/fdef fetch-commands
        :args (s/cat :index  ::CommandDataAccess
                     :offset (s/int-in 0 Long/MAX_VALUE)
                     :limit  (s/int-in 0 Long/MAX_VALUE))
        :ret (s/every ::commander/command)
        :fn #(let [limit (-> % :args :limit)]
               (if (pos? limit)
                 (= (-> % :ret count) limit)
                 true)))

(defn fetch-command-by-id
  "Fetches and returns a single command from the given index
  component, identified by its UUID.  Includes all decendent events of
  the given command."
  [index id]
  (log/debug ::fetch-command-by-id [index id])
  (-fetch-command-by-id index id))

(s/fdef fetch-command-by-id
        :args (s/cat :index ::CommandDataAccess
                     :id    ::commander/id)
        :ret ::commander/command)

(defn insert-commands!
  "Inserts the given command maps into the given index
  component. Returns true if insert succeeded, false otherwise."
  [index commands]
  (log/debug ::insert-commands! [index commands])
  (if (sequential? commands)
    (-insert-commands! index commands)
    (insert-commands! index [commands])))

(s/fdef insert-commands!
        :args (s/cat :index ::CommandDataAccess
                     :command (s/or :single ::commander/command
                                    :collection (s/every ::commander/command)))
        :ret boolean?)

(defprotocol EventDataAccess
  (-fetch-events [index limit offset]
    "Fetches events from the given index component, returning a map of
      - :events a vector of event maps
      - :limit the limit passed to the query
      - :offset the offset passed to the query
      - :total the total count of events")
  (-fetch-event-by-id [index id]
    "Fetches and returns a single event from the given index component, identified by its UUID.")
  (-insert-events! [index events]
    "Inserts the given event maps into the given index component.
    Returns true if insert succeeded, false otherwise."))

(s/def ::EventDataAccess (partial satisfies? EventDataAccess))

(defn fetch-events
  "Fetches events from the given index component, returning a map of
    - :events a vector of event maps
    - :limit the limit passed to the query
    - :offset the offset passed to the query
    - :total the total count of events"
  ([index]
   (fetch-events index nil nil))
  ([index limit offset]
   (log/debug ::fetch-events [index limit offset])
   (let [limit  (or limit 0)
         offset (or offset 0)]
     (-fetch-events index limit offset))))

(s/fdef fetch-events
        :args (s/cat :index ::EventDataAccess
                     :offset (s/int-in 0 Long/MAX_VALUE)
                     :limit  (s/int-in 0 Long/MAX_VALUE))
        :ret (s/every ::commander/event)
        :fn #(let [limit (-> % :args :limit)]
               (if (pos? limit)
                 (= (-> % :ret count) limit)
                 true)))

(defn fetch-event-by-id
  "Fetches and returns a single event from the given index
  component, identified by its UUID.  Includes all decendent events of
  the given event."
  [index id]
  (log/debug ::fetch-event-by-id [index id])
  (-fetch-event-by-id index id))

(s/fdef fetch-event-by-id
        :args (s/cat :index ::EventDataAccess
                     :id  ::commander/id)
        :ret ::commander/event)

(defn insert-events!
  "Inserts the given event maps into the given index
  component. Returns true if insert succeeded, false otherwise."
  [index events]
  (log/debug ::insert-events! [index events])
  (if (sequential? events)
    (-insert-events! index events)
    (insert-events! index [events])))

(s/fdef insert-events!
        :args (s/cat :index ::EventDataAccess
                     :event (s/or :single ::commander/event
                                  :collection (s/every ::commander/event)))
        :ret boolean?)

(defmulti construct-index :type)