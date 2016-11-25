;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.index.jdbc
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as j]
            ragtime.jdbc
            ragtime.repl
            [clojure.data.fressian :as fressian]
            [io.pedestal.http.route :as route]
            [io.pedestal.log :as log]
            [com.stuartsierra.component :as component]
            [com.capitalone.commander.util :as util]
            [com.capitalone.commander.index :as index]))

(set! *warn-on-reflection* true)

(defn event-from-select [c]
  (cond-> c
    (:action c) (update-in [:action] keyword)
    (:data c) (update-in [:data] fressian/read)))

(def command-from-select event-from-select)

(defn- event-for-insert
  [event]
  (-> event
      (update-in [:action] util/keyword->string)
      (update-in [:data] #(-> %
                              (fressian/write :footer? true)
                              util/buf->bytes))))

(defn- command-for-insert
  [command]
  (-> command
      event-for-insert
      (assoc :command true)))

(defrecord JdbcIndex [db-spec connection init-fn]
  component/Lifecycle
  (start [component]
    (let [conn (or connection (j/get-connection db-spec))
          _    (when init-fn (init-fn db-spec))]
      (assoc component :connection conn)))
  (stop [component]
    (when connection (.close ^java.lang.AutoCloseable connection))
    (assoc component :connection nil))

  index/CommonDataAccess
  (-find-latest-partition-offset [index topic part]
    (first (j/query index
                    ["SELECT max(commander.offset) FROM commander WHERE topic = ? AND partition = ?" topic part]
                    {:row-fn :max})))


  index/CommandDataAccess
  (-fetch-commands [index limit offset]
    (let [commands-query (if (pos? limit)
                           ["SELECT id, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = true ORDER BY timestamp ASC LIMIT ? OFFSET ?"
                            limit
                            offset]
                           ["SELECT id, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = true ORDER BY timestamp ASC OFFSET ?"
                            offset])]
      (j/with-db-transaction [tx index {:read-only? true}]
        {:commands (map command-from-select (j/query index commands-query))
         :offset   offset
         :limit    limit
         :total    (first (j/query index
                                   ["SELECT count(id) FROM commander WHERE command = true"]
                                   {:row-fn :count}))})))
  (-fetch-command-by-id [index id]
    (some-> (j/query index
                     ["SELECT id, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = true AND id = ?" id])
            first
            command-from-select))
  (-insert-commands! [index commands]
    (j/insert-multi! index :commander
                     (map command-for-insert commands)
                     {:entities     (j/quoted \")
                      :transaction? false}))

  index/EventDataAccess
  (-fetch-events [index limit offset]
    (let [events-query (if (pos? limit)
                         ["SELECT id, parent, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = false ORDER BY timestamp ASC LIMIT ? OFFSET ?"
                          limit
                          offset]
                         ["SELECT id, parent, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = false ORDER BY timestamp ASC OFFSET ?"
                          offset])]
      (j/with-db-transaction [tx index {:read-only? true}]
        {:events (map event-from-select (j/query index events-query))
         :offset offset
         :limit  limit
         :total  (first (j/query index
                                 ["SELECT count(id) FROM commander WHERE command = false"]
                                 {:row-fn :count}))})))
  (-fetch-event-by-id [index id]
    (some-> (j/query index
                     ["SELECT id, parent, action, data, timestamp, topic, partition, \"offset\" FROM commander WHERE command = false AND id = ?" id])
            first
            event-from-select))
  (-insert-events! [index events]
    (j/insert-multi! index :commander
                     (map event-for-insert events)
                     {:entities (j/quoted \")
                      :transaction? false})))

(defn construct-jdbc-db
  ([db-spec]
   (map->JdbcIndex {:db-spec db-spec}))
  ([db-spec init-fn]
   (map->JdbcIndex {:db-spec db-spec :init-fn init-fn})))

;;;; Index Bootstrap & Migrations

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
