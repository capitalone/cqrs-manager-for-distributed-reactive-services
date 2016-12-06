;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.api
  (:require [clojure.spec :as s]
            [clojure.core.async :as a]
            [com.stuartsierra.component :as c]
            [io.pedestal.log :as log]
            [clj-uuid :as uuid]
            [com.capitalone.commander :as commander]
            [com.capitalone.commander.index :as index]
            [com.capitalone.commander.log :as l]))

(set! *warn-on-reflection* true)

(defprotocol CommandService
  (-create-command [this command-params]
    "Creates a command from the command-params and records to the
    Log.  Returns the created command")
  (-create-command-sync [this command-params sync-timeout-ms]
    "Creates a command from the command-params and records to the Log.
    Returns the newly created command, with a :children key whose
    value is a vector containing the completion event id if
    successful.  If ")
  (-list-commands [this limit offset]
    "Returns a map of :commands, :limit, :offset, and :total,
    where :commands is `limit` indexed commands, starting at `offset`.
    If limit is 0, returns all indexed commands starting with
    offset. :total is the total count of all commands.")
  (-get-command-by-id [this id]
    "Returns the indexed command with the given id, or nil if none
    found.")
  (-commands-ch [this ch]
    "Returns ch, the given core.async channel that will convey all
    commands (from time of call onward)."))

(defprotocol CommandValidator
  (-validate-command-params [this command-params]
    "Returns true if valid, map of errors otherwise"))

(defprotocol EventService
  (-list-events [this limit offset]
    "Returns a map of :events, :limit, :offset, and :total,
    where :events is `limit` indexed events, starting at `offset`.
    If limit is 0, returns all indexed events starting with
    offset. :total is the total count of all events.")
  (-get-event-by-id [this id]
    "Returns the indexed event with the given id, or nil if none
    found.")
  (-events-ch [this ch]
    "Returns ch, the given core.async channel that will convey all
    events (from time of call onward)."))

(defn create-command
  "Creates a command by recording to the Log. If sync? is false (the
  default if not given), writes to the Log and returns immediately.
  If sync? is true, writes to the Log and waits for the command's
  corresponding completion event to arrive on the Log before
  returning. Returns the newly created command in either case."
  ([api command-params]
   (create-command api command-params false))
  ([api command-params sync?]
   (log/info ::create-command [api command-params sync?])
   (if sync?
     (-create-command-sync api command-params (:sync-timeout-ms api))
     (-create-command api command-params))))

(s/def ::CommandService (partial satisfies? CommandService))

(s/fdef create-command
        :args (s/cat :api ::CommandService
                     :command-params ::commander/command-params
                     :sync? (s/? (s/nilable boolean?)))
        :ret ::commander/command
        :fn (s/and #(= (-> % :ret :action) (-> % :args :command-params :action))
                   #(= (-> % :ret :data)   (-> % :args :command-params :data))))

(defn list-commands
  "Returns a map of :commands, :limit, :offset, and :total,
  where :commands is `limit` (defaults to 100) indexed commands,
  starting at `offset`, and
  :total is the total count of all commands."
  ([api] (list-commands api nil))
  ([api limit] (list-commands api limit nil))
  ([api limit offset]
   (log/info ::list-commands [api limit offset])
   (-list-commands api limit offset)))

(s/def ::commands (s/every ::commander/command))
(s/def ::total (s/int-in 0 Long/MAX_VALUE))

(s/fdef list-commands
        :args (s/cat :api ::CommandService
                     :limit (s/? (s/nilable ::index/limit))
                     :offset (s/? (s/nilable ::index/offset)))
        :ret (s/keys :req-un [::commands ::commander/limit ::commander/offset ::total])
        :fn #(let [limit (-> % :args :limit)]
               (if (pos? limit)
                 (= (-> % :ret count) limit)
                 true)))

(defn get-command-by-id
  "Returns the indexed command with the given id, or nil if none
  found."
  [api id]
  (log/info ::get-command-by-id [api id])
  (-get-command-by-id api id))

(s/fdef get-command-by-id
        :args (s/cat :api ::CommandService
                     :id  ::commander/id)
        :ret ::commander/command)

(defn command-map
  [{:keys [key value topic partition offset timestamp] :as command}]
  (log/debug ::command-map [command])
  (let [{:keys [action data]} value]
    {:id        key
     :action    action
     :data      data
     :timestamp timestamp
     :topic     topic
     :partition partition
     :offset    offset}))

(defn commands-ch
  "Returns a core.async channel (ch if given, a sliding-buffer channel
  of size 10 otherwise) that will convey all commands arriving from
  the time of the call onward."
  ([api]
   (commands-ch api (a/chan (a/sliding-buffer 10))))
  ([api ch]
   (log/info ::commands-ch [api ch])
   (-commands-ch api ch)
   ch))

(defn validate-command-params
  "Returns true if valid, a map of errors otherwise."
  [api command-params]
  (-validate-command-params api command-params))

(s/fdef validate-command-params
        :args (s/cat :api ::CommandService
                     :command-params ::commander/command-params)
        :ret (s/or :valid true?
                   :invalid (s/keys)))

(s/def ::EventService (partial satisfies? EventService))

(defn list-events
  "Returns a map of :events, :limit, :offset, and :total,
   where :events is `limit` indexed events, starting at `offset`.
   If limit is 0, returns all indexed events starting with
   offset. :total is the total count of all events."
  ([api] (list-events api nil))
  ([api limit] (list-events api limit nil))
  ([api limit offset]
   (log/info ::list-events [api limit offset])
   (-list-events api limit offset)))

(s/def ::events (s/every ::commander/event))
(s/fdef list-events
        :args (s/cat :api ::EventService
                     :limit (s/? (s/nilable ::index/limit))
                     :offset (s/? (s/nilable ::index/offset)))
        :ret (s/keys :req-un [::events ::commander/limit ::commander/offset ::total])
        :fn #(let [limit (-> % :args :limit)]
               (if (pos? limit)
                 (= (-> % :ret count) limit)
                 true)))

(defn get-event-by-id
  "Returns the indexed event with the given id, or nil if none
  found."
  [api id]
  (log/info ::get-event-by-id [api id])
  (-get-event-by-id api id))

(s/fdef get-event-by-id
        :args (s/cat :api ::EventService
                     :id  ::commander/id)
        :ret ::commander/event)

(defn event-map
  [{:keys [key value topic partition offset timestamp] :as event}]
  (log/debug ::event-map [event])
  (let [{:keys [action data parent]} value]
    {:id        key
     :parent    parent
     :action    action
     :data      data
     :timestamp timestamp
     :topic     topic
     :partition partition
     :offset    offset}))

(defn events-ch
  "Returns a core.async channel (ch if given, a sliding-buffer channel
  of size 10 otherwise) that will convey all events arriving from
  the time of the call onward."
  ([api]
   (events-ch api (a/chan (a/sliding-buffer 10))))
  ([api ch]
   (log/info ::events-ch [api ch])
   (-events-ch api ch)
   ch))

(defn- command-record
  [topic id command]
  {:topic topic
   :key   id
   :value command})

(defn- send-command-and-await-result!
  [log-producer command-topic id command]
  (let [record (command-record command-topic id command)
        ch (l/send! log-producer record)]
    (if-some [ret (a/<!! ch)]
      (if (instance? Exception ret)
        (throw (ex-info "Error writing to log" {:record record} ret))
        ret)
      (throw (ex-info "Error writing to log: send response channel closed" {:record record})))))

(defrecord Commander [index
                      log-producer
                      commands-topic
                      events-topic
                      log-consumer
                      ch
                      pub
                      commands-ch
                      commands-mult
                      events-ch
                      events-pub
                      events-mult
                      sync-timeout-ms]
  CommandService
  (-create-command [this command-params]
    (let [id     (uuid/v1)
          result (send-command-and-await-result! log-producer commands-topic id command-params)]
      (assoc command-params
             :id        id
             :timestamp (:timestamp result)
             :topic     (:topic result)
             :partition (:partition result)
             :offset    (:offset result))))
  (-create-command-sync [this command-params sync-timeout-ms]
    (let [id     (uuid/v1)
          rch    (a/promise-chan)
          _      (a/sub events-pub id rch)
          result (send-command-and-await-result! log-producer commands-topic id command-params)
          base   (assoc command-params
                        :id        id
                        :timestamp (:timestamp result)
                        :topic     (:topic result)
                        :partition (:partition result)
                        :offset    (:offset result))]
      (try
        (a/alt!!
          rch
          ([v] (assoc base :children [(:key v)]))

          (a/timeout sync-timeout-ms)
          ([v] (assoc base :error "Timed out waiting for completion event.")))
        (finally
          (a/close! rch)
          (a/unsub events-pub id rch)))))
  (-list-commands [_ limit offset]
    (index/fetch-commands index limit offset))
  (-get-command-by-id [_ id]
    (index/fetch-command-by-id index id))
  (-commands-ch [_ ch]
    (let [int (a/chan 1 (map command-map))]
      (a/pipe int ch)
      (a/tap commands-mult int)
      ch))

  CommandValidator
;;; TODO
  (-validate-command-params [this command-params] true)

  EventService
  (-list-events [_ limit offset]
    (index/fetch-events index limit offset))
  (-get-event-by-id [_ id]
    (index/fetch-event-by-id index id))
  (-events-ch [_ ch]
    (let [int (a/chan 1 (map event-map))]
      (a/pipe int ch)
      (a/tap events-mult int)
      ch))

  c/Lifecycle
  (start [this]
    (let [ch             (a/chan 1)
          pub            (a/pub ch :topic)

          events-ch      (a/chan 1)
          events-mult    (a/mult events-ch)

          events-ch-copy (a/chan 1)
          events-pub     (a/pub events-ch-copy (comp :parent :value))

          commands-ch    (a/chan 1)
          commands-mult  (a/mult commands-ch)]
      (a/sub pub commands-topic commands-ch)
      (a/sub pub events-topic events-ch)
      (a/tap events-mult events-ch-copy)

      (l/consume-onto-channel! log-consumer [commands-topic events-topic] ch)

      (assoc this
             :ch            ch
             :pub           pub
             :events-ch     events-ch
             :events-mult   events-mult
             :events-pub    events-pub
             :commands-ch   commands-ch
             :commands-mult commands-mult)))
  (stop [this]
    (when ch (a/close! ch))
    (when pub (a/unsub-all pub))
    (when events-ch  (a/close! events-ch))
    (when events-pub (a/unsub-all events-pub))
    (when commands-ch (a/close! commands-ch))
    (dissoc this :events-ch :events-pub :commands-ch)))

(defn construct-commander-api
  [{:keys [commands-topic events-topic sync-timeout-ms]
    :as config}]
  (map->Commander {:commands-topic  commands-topic
                   :events-topic    events-topic
                   :sync-timeout-ms sync-timeout-ms}))
