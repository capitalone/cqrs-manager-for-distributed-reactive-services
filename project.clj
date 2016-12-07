;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(defproject com.capitalone/cmdr "0.3.0-alpha"
  :min-lein-version "2.0.0"
  :description "Command handler in REST+CQRS+ES architecture."
  :url         "http://capitalone.com/"
  :scm         {:url "https://github.com/capitalone/cqrs-manager-for-distributed-reactive-services"}
  :license     {"License Name" "Apache License, Version 2.0"}
  :dependencies [ ;; Base
                 [org.clojure/clojure "1.9.0-alpha12"]
                 [org.clojure/core.async "0.2.385"]
                 [com.stuartsierra/component "0.3.1"]
                 [prismatic/schema "1.1.3"]
                 [meta-merge "1.0.0"]
                 [environ "1.1.0"]
                 [danlentz/clj-uuid "0.1.6"]

                 ;; Web
                 [io.pedestal/pedestal.service "0.5.1"]
                 [io.pedestal/pedestal.jetty "0.5.1"]
                 [pedestal-api "0.3.0"]
                 [metosin/ring-swagger "0.22.10"]
                 [cheshire "5.6.3"]
                 [hiccup "1.0.5"]

                 ;; gRPC
                 [com.google.protobuf/protobuf-java "3.0.2"]
                 [io.grpc/grpc-netty "1.0.0"]
                 [io.grpc/grpc-protobuf "1.0.0"]
                 [io.grpc/grpc-stub "1.0.0"]

                 ;; Database
                 [org.clojure/java.jdbc "0.6.2-alpha3"]
                 [org.postgresql/postgresql "9.4.1210"]
                 [ragtime/ragtime.jdbc "0.6.3"]

                 ;; Fressian
                 [org.fressian/fressian "0.6.6"]
                 [org.clojure/data.fressian "0.2.1"]

                 ;; Kafka
                 [org.apache.kafka/kafka-clients "0.10.0.1"]

                 ;; Kinesis
                 [com.amazonaws/aws-java-sdk-kinesis "1.11.60"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.60"]
                 [com.amazonaws/amazon-kinesis-client "1.7.2"]

                 ;; Logging
                 [ch.qos.logback/logback-classic "1.1.7"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.21"]
                 [org.slf4j/jcl-over-slf4j "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]]
  :default {:file-pattern #"\.(clj|cljs|cljc)$"}
  :java-source-paths ["generated/main/java" "generated/main/grpc"]
  :javac-options     ["-target" "1.8" "-source" "1.8"]
  :profiles {:debug         {:debug      true
                             :injections [(prn (into {} (System/getProperties)))]}
             :dev           [:project/dev :profiles/dev]
             :test          [:project/test :profiles/test]
             :uberjar       {:target-path  "target/%s/"
                             :aot          :all
                             :omit-source  true
                             :uberjar-name "cmdr-standalone.jar"}
             :repl          {:source-paths ["dev"]
                             :repl-options {:init-ns user}}
             :profiles/dev  {}
             :profiles/test {}
             :project/dev   {:dependencies [[reloaded.repl "0.2.2"]
                                            [org.clojure/tools.namespace "0.2.11"]
                                            [org.clojure/tools.nrepl "0.2.12"]
                                            [eftest "0.1.1"]
                                            [org.clojure/test.check "0.9.0"]]
                             :plugins      [[lein-environ "1.0.2"]
                                            [lein-auto "0.1.2"]]
                             :env          {#_:index-type       #_"dynamodb"
                                            :index-table-name "commander-dev-index"
                                            :database-uri     "jdbc:postgresql://localhost/commander?user=commander&password=commander"
                                            #_:log-type         #_"kinesis"
                                            :commands-topic   "commander-dev-commands"
                                            :events-topic     "commander-dev-events"
                                            :kafka-servers    "localhost:9092"
                                            :indexer-group-id "commander-dev-indexer"
                                            :rest-group-id    "commander-dev-rest"}}
             :project/test  {:env {:database-uri     "jdbc:postgresql://localhost/commander?user=commander&password=commander"
                                   :kafka-servers    "localhost:9092"
                                   :indexer-group-id "commander-dev-indexer"
                                   :rest-group-id    "commander-dev-rest"}}})
