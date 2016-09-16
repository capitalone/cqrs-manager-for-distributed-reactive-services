;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.database
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.spec :as s]
            [clojure.data.fressian :as fressian]
            [clojure.java.jdbc :as j]
            ragtime.jdbc
            ragtime.repl
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [io.pedestal.http.route :as route]
            [com.capitalone.commander.util :as util]
            [com.capitalone.commander :as commander]))

(set! *warn-on-reflection* true)

(defprotocol CommandDataAccess
  (-find-latest-commands-offset [database topic part]
    "Finds and returns the highest seen offset from the commands
    topic.")
  (-fetch-commands [database limit offset]
    "Fetches commands from the given database component, returning a map of
      - :commands a vector of command maps
      - :limit the limit passed to the query
      - :offset the offset passed to the query
      - :count the total count of commands")
  (-fetch-command-by-id [database id]
    "Fetches and returns a single command from the given database component, identified by its UUID.")
  (-insert-commands! [database commands]
    "Inserts the given command maps into the given database component.
    Returns true if insert succeeded, false otherwise."))

(defn find-latest-commands-offset
  [database topic partition]
  (log/debug ::find-latest-commands-offset [database topic partition])
  (or (-find-latest-commands-offset database topic partition)
      0))

(s/def ::CommandDataAccess (partial satisfies? CommandDataAccess))
(s/fdef find-latest-commands-offset
        :args (s/cat :database ::CommandDataAccess
                     :topic ::commander/topic
                     :partition ::commander/partition)
        :ret ::commander/offset)

(defn fetch-commands
  "Fetches commands from the given database component, returning a map of
    - :commands a vector of command maps
    - :limit the limit passed to the query
    - :offset the offset passed to the query
    - :count the total count of commands"
  ([database]
   (fetch-commands database 0 0))
  ([database limit offset]
   (log/debug ::fetch-commands [database limit offset])
   (let [limit  (or limit 0)
         offset (or offset 0)]
     (-fetch-commands database limit offset))))

(s/fdef fetch-commands
        :args (s/cat :database ::CommandDataAccess
                     :offset (s/int-in 0 Long/MAX_VALUE)
                     :limit  (s/int-in 0 Long/MAX_VALUE))
        :ret (s/every ::commander/command)
        :fn #(let [limit (-> % :args :limit)]
              (if (pos? limit)
                (= (-> % :ret count) limit)
                true)))

(defn fetch-command-by-id
  "Fetches and returns a single command from the given database
  component, identified by its UUID.  Includes all decendent events of
  the given command."
  [database id]
  (log/debug ::fetch-command-by-id [database id])
  (-fetch-command-by-id database id))

(s/fdef fetch-command-by-id
        :args (s/cat :database ::CommandDataAccess
                     :id  ::commander/id)
        :ret ::commander/command)

(defn insert-commands!
  "Inserts the given command maps into the given database
  component. Returns true if insert succeeded, false otherwise."
  [database commands]
  (log/debug ::insert-commands! [database commands])
  (if (sequential? commands)
    (-insert-commands! database commands)
    (insert-commands! database [commands])))

(s/fdef insert-commands!
        :args (s/cat :database ::CommandDataAccess
                     :command (s/or :single ::commander/command
                                    :collection (s/and sequential?
                                                       (s/every ::commander/command
                                                          :kind vector?))))
        :ret boolean?)

(defprotocol EventDataAccess
  (-find-latest-events-offset [database topic part]
    "Finds and returns the highest seen offset for the given topic partition.")
  (-fetch-events [database limit offset]
    "Fetches events from the given database component, returning a map of
      - :events a vector of event maps
      - :limit the limit passed to the query
      - :offset the offset passed to the query
      - :count the total count of events")
  (-fetch-event-by-id [database id]
    "Fetches and returns a single event from the given database component, identified by its UUID.")
  (-insert-events! [database events]
    "Inserts the given event maps into the given database component.
    Returns true if insert succeeded, false otherwise."))

(defn find-latest-events-offset
  [database topic part]
  (log/debug ::find-latest-events-offset [database topic part])
  (-find-latest-events-offset database topic part))

(s/def ::EventDataAccess (partial satisfies? EventDataAccess))
(s/fdef find-latest-events-offset
        :args (s/cat :database ::EventDataAccess
                     :topic ::commander/topic
                     :partition ::commander/partition)
        :ret ::commander/offset)

(defn fetch-events
  "Fetches events from the given database component, returning a map of
    - :events a vector of event maps
    - :limit the limit passed to the query
    - :offset the offset passed to the query
    - :count the total count of events"
  ([database]
   (fetch-events database nil nil))
  ([database limit offset]
   (log/debug ::fetch-events [database limit offset])
   (let [limit  (or limit 0)
         offset (or offset 0)]
     (-fetch-events database limit offset))))

(s/fdef fetch-events
        :args (s/cat :database ::EventDataAccess
                     :offset (s/int-in 0 Long/MAX_VALUE)
                     :limit  (s/int-in 0 Long/MAX_VALUE))
        :ret (s/every ::commander/event)
        :fn #(let [limit (-> % :args :limit)]
              (if (pos? limit)
                (= (-> % :ret count) limit)
                true)))

(defn fetch-event-by-id
  "Fetches and returns a single event from the given database
  component, identified by its UUID.  Includes all decendent events of
  the given event."
  [database id]
  (log/debug ::fetch-event-by-id [database id])
  (-fetch-event-by-id database id))

(s/fdef fetch-event-by-id
        :args (s/cat :database ::EventDataAccess
                     :id  ::commander/id)
        :ret ::commander/event)

(defn insert-events!
  "Inserts the given event maps into the given database
  component. Returns true if insert succeeded, false otherwise."
  [database events]
  (log/debug ::insert-events! [database events])
  (if (sequential? events)
    (-insert-events! database events)
    (insert-events! database [events])))

(s/fdef insert-events!
        :args (s/cat :database ::EventDataAccess
                     :event (s/or :single ::commander/event
                                  :collection (s/every ::commander/event)))
        :ret boolean?)

(defn- event-for-insert
  [event]
  (-> event
      (update-in [:action] str)
      (update-in [:data] #(-> %
                              (fressian/write :footer? true)
                              util/buf->bytes))))

(defn- command-for-insert
  [command]
  (-> command
      event-for-insert
      (assoc :command true)))

(defrecord JdbcDatabase [db-spec connection init-fn]
  component/Lifecycle
  (start [component]
    (let [conn (or connection (j/get-connection db-spec))
          _    (when init-fn (init-fn db-spec))]
      (assoc component :connection conn)))
  (stop [component]
    (when connection (.close ^java.lang.AutoCloseable connection))
    (assoc component :connection nil))

  CommandDataAccess
  (-find-latest-commands-offset [database topic part]
    (first (j/query database
                    ["SELECT max(commander.offset) FROM commander WHERE command = true AND topic = ? AND partition = ?" topic part]
                    {:row-fn :max})))
  (-fetch-commands [database limit offset]
    (let [commands-query (if (pos? limit)
                           ["SELECT id, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = true ORDER BY timestamp ASC LIMIT ? OFFSET ?"
                            limit
                            offset]
                           ["SELECT id, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = true ORDER BY timestamp ASC OFFSET ?"
                            offset])]
      (j/with-db-transaction [tx database {:read-only? true}]
        {:commands (j/query database commands-query)
         :offset   offset
         :limit    limit
         :count    (first (j/query database
                                   ["SELECT count(id) FROM commander WHERE command = true"]
                                   {:row-fn :count}))})))
  (-fetch-command-by-id [database id]
    (first (j/query database
                    ["SELECT id, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = true AND id = ?" id])))
  (-insert-commands! [database commands]
    (j/insert-multi! database :commander
                     (map command-for-insert commands)
                     {:entities     (j/quoted \")
                      :transaction? false}))

  EventDataAccess
  (-find-latest-events-offset [database topic part]
    (first (j/query database
                    ["SELECT max(commander.offset) FROM commander WHERE command = false AND topic = ? AND partition = ?" topic part]
                    {:row-fn :max})))
  (-fetch-events [database limit offset]
    (let [events-query (if (pos? limit)
                         ["SELECT id, parent, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = false ORDER BY timestamp ASC LIMIT ? OFFSET ?"
                          limit
                          offset]
                         ["SELECT id, parent, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = false ORDER BY timestamp ASC OFFSET ?"
                          offset])]
      (j/with-db-transaction [tx database {:read-only? true}]
        {:events (j/query database events-query)
         :offset offset
         :limit  limit
         :count  (first (j/query database
                                 ["SELECT count(id) FROM commander WHERE command = false"]
                                 {:row-fn :count}))})))
  (-fetch-event-by-id [database id]
    (first (j/query database
                    ["SELECT id, parent, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = false AND id = ?" id])))
  (-insert-events! [database events]
    (j/insert-multi! database :commander
                     (map event-for-insert events)
                     {:entities (j/quoted \")
                      :transaction? false})))

(defn construct-jdbc-db
  ([db-spec]
   (map->JdbcDatabase {:db-spec db-spec}))
  ([db-spec init-fn]
   (map->JdbcDatabase {:db-spec db-spec :init-fn init-fn})))

;;;; Database Bootstrap & Migrations

(defn ragtime-config
  [database]
  {:datastore  (ragtime.jdbc/sql-database database)
   :migrations (ragtime.jdbc/load-resources "migrations")})

(defn ensure-user!
  [database username password]
  (try
    (log/info ::ensure-user! [username "password-REDACTED"])
    (j/db-do-commands database (str "CREATE USER " username " WITH PASSWORD '" password "'"))
    (log/info ::ensure-user! "Success")
    (catch java.sql.SQLException e
      (log/error ::ensure-user! "Failed to create user"
                 :username username
                 :exception (.getNextException e)))))

(defn ensure-database!
  [database db-name username]
  (try
    (log/info ::ensure-database! [db-name username])
    (j/db-do-commands database false (str "CREATE DATABASE " db-name))
    (log/info ::ensure-database! "Success")
    (catch java.sql.SQLException e
      (log/error ::ensure-database! "Failed to create database"
                 :db-name db-name
                 :username username
                 :exception (.getNextException e))))
  (try
    (log/info ::ensure-database! "GRANT")
    (j/db-do-commands database (str "GRANT ALL PRIVILEGES ON DATABASE " db-name " TO " username))
    (log/info ::ensure-database! "Success")
    (catch java.sql.SQLException e
      (log/error ::ensure-user! (str "Failed to GRANT ALL PRIVILEGES ON " db-name " TO " username)
                 :username username
                 :exception (.getNextException e)))))

(defn ensure-table!
  [database username]
  (try
    (log/info ::ensure-table! [username])
    (j/db-do-commands database "CREATE TABLE IF NOT EXISTS commander (id uuid CONSTRAINT commander_primary_key PRIMARY KEY) WITH ( OIDS=FALSE )")
    (log/info ::ensure-table! "Success")
    (catch java.sql.SQLException e
      (log/error ::ensure-table! "Failed to create table"
                 :username username
                 :exception (.getNextException e)))))

(defn migrate-database!
  [database]
  (log/info ::migrate-database! "Applying ragtime migrations...")
  (ragtime.repl/migrate (ragtime-config database))
  (log/info ::migrate-database! "Success"))

(defn rollback-database!
  [database]
  (log/info ::rollback-database! "Rolling back ragtime migrations...")
  (ragtime.repl/rollback (ragtime-config database))
  (log/info ::rollback-database! "Success"))

(defn- query-params->query-string
  [query-params]
  (str \?
       (string/join \& (map (fn [[k v]]
                              (str (route/encode-query-part (name k))
                                   \=
                                   (route/encode-query-part (str v))))
                            query-params))))

(defn- user-uri
  [command-uri db-name username password]
  (let [uri          (java.net.URI. command-uri)
        postgres     (-> uri .getSchemeSpecificPart (java.net.URI.))
        query-params (route/parse-query-string (.getQuery postgres))]
    (str (.getScheme uri) ":" (.getScheme postgres) "://" (.getAuthority postgres)
         "/" db-name (-> query-params
                         (assoc :user username :password password)
                         query-params->query-string))))

(defn -main
  "Bootstraps the database given the command-uri (i.e a JDBC URL using
  root/admin credentials) by 1) create a user with the given username
  and password, 2) creating a database named db-name and making the
  new user the owner of that database, and 3) by creating a skeleton
  table and then running the migrations."
  [& [command-uri db-name username password]]
  (j/with-db-connection [database {:connection-uri command-uri}]
    (log/debug ::-main database)
    (ensure-user! database username password)
    (ensure-database! database db-name username))
  (j/with-db-connection [database {:connection-uri (user-uri command-uri db-name username password)}]
    (ensure-table! database username)
    (migrate-database! database)))
