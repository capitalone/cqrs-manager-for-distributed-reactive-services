;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.rest.endpoint.commander
  (:require [clojure.core.async :as a]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [clj-uuid :as uuid]
            [schema.core :as s]
            [cheshire.core :as json]
            [io.pedestal.log :as log]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.helpers :refer [defbefore]]
            [io.pedestal.http.route :refer [url-for]]
            #_[io.pedestal.http.jetty.websockets :as ws]
            [io.pedestal.http.sse :as sse]
            [pedestal-api
             [core :as papi]
             [helpers :refer [defhandler]]]
            [ring.util.response :as ring-resp]
            [com.capitalone.commander.api :as api]
            [com.capitalone.commander.rest.hiccup :as h]))

(set! *warn-on-reflection* true)

;; TODO: tighten this up
(s/defschema RequestError
  s/Any)

;; TODO: generate via api/validate-command + specs in API
;; TODO: tighten up :user and :system -- do we require or not?  What is the proper schema for :user and :system?
;; TODO: tighten up Command validation per registered Command schemas
(s/defschema CommandParams
  {:action                  s/Keyword
   :data                    {s/Keyword s/Any}
   (s/optional-key :user)   s/Uuid
   (s/optional-key :system) s/Uuid})

;; TODO: use api/validate-command-params
(def action->schema
  (constantly {s/Keyword s/Any}))

;; TODO: use api/validate-command-params
(defn validate-body-params
  "Returns truthy if given parsed body is a valid command."
  [body]
  (or (s/check CommandParams body)
      (let [{:keys [action data]} body
            schema (action->schema action)]
        (s/check schema data))))

(s/defschema Command
  (assoc CommandParams
         :id s/Uuid
         :timestamp s/Int
         :topic s/Str
         :partition s/Int
         :offset s/Int

         (s/optional-key :children) [s/Uuid]))

(s/defschema Event
  (assoc Command :parent s/Uuid))

;; TODO: hypermedia
(defn display-command
  [c]
  c)

;;; TODO: authorization
(defhandler all-commands
  {:summary    "Get all commands"
   :parameters {:query-params {(s/optional-key :sync)   s/Bool
                               (s/optional-key :limit)  s/Int
                               (s/optional-key :offset) s/Int}}
   :responses  {200 {:body {:commands [Command]
                            :limit    s/Int
                            :offset   s/Int
                            :total    s/Int}}}}
  [{:keys [component] :as request}]
  (let [limit           (get-in request [:query-params :limit])
        offset          (get-in request [:query-params :offset])
        commands-result (-> component
                            :api
                            (api/list-commands offset limit)
                            (update-in [:commands] #(mapv display-command %)))
        sync            (get-in request [:query-params :sync])

        ;; TODO: fix content negotiation
        media-type      (get-in request [:headers "accept"])]
    {:status 200
     :body   (if (= media-type "text/html")
               (h/commands-hiccup commands-result sync)
               commands-result)}))

;;; TODO: authorization
(defhandler all-events
  {:summary    "Get all events"
   :parameters {:query-params {(s/optional-key :sync)   s/Bool
                               (s/optional-key :limit)  s/Int
                               (s/optional-key :offset) s/Int}}
   :responses  {200 {:body {:events [Event]
                            :limit  s/Int
                            :offset s/Int
                            :total  s/Int}}}}
  [{:keys [component] :as request}]
  (let [limit         (get-in request [:query-params :limit])
        offset        (get-in request [:query-params :offset])
        events-result (-> component
                          :api
                          (api/list-events offset limit)
                          (update-in [:events] #(mapv display-command %)))
        sync          (get-in request [:query-params :sync])

        ;; TODO: fix content negotiation
        media-type    (get-in request [:headers "accept"])]
    {:status 200
     :body   (if (= media-type "text/html")
               (h/events-hiccup events-result sync)
               events-result)}))

(defbefore ensure-processable
  [{:keys [request] :as context}]
  (log/debug ::ensure-processable (:body-params request))
  (if (= :post (:request-method request))
    (if-let [errors (-> request
                        :body-params
                        validate-body-params)]
      (assoc context :response {:status  422
                                :headers {}
                                :body    errors})
      context)
    context))

;;; TODO: authorization
(defhandler create-command
  {:summary    "Create a command"
   :parameters {:body-params  CommandParams
                :query-params {(s/optional-key :sync) s/Bool}}
   :responses  {201 {:body Command}
                202 {:body Command}
                409 {:body RequestError}
                422 {:body RequestError}}}
  [{:keys [component] :as request}]
  (let [sync?          (get-in request [:query-params :sync])
        command-params (:body-params request)
        command        (-> component
                           :api
                           (api/create-command command-params sync?)
                           display-command)

        ;; TODO: fix content negotiation
        media-type     (get-in request [:headers "accept"])]
    {:status  (if sync? (if (:error command) 409 201) 202)
     :headers {"Location" (url-for ::get-command :params {:id (:id command)} :absolute? true)}
     :body    (if (= media-type "text/html")
                (h/command-hiccup command)
                command)}))

(defhandler get-command
  {:summary    "Get a command by id"
   :parameters {:path-params {:id s/Uuid}}
   :responses  {200 {:body Command}
                404 {:body s/Str}}}
  [{:keys [component] :as request}]
  (let [command    (api/get-command-by-id (:api component)
                                          (-> request :path-params :id))

        ;; TODO: fix content negotiation
        media-type (get-in request [:headers "accept"])]
    (log/info :endpoint ::get-command :command command :media-type media-type)
    ;; TODO: handle 404 case more gracefully
    (when command
      {:status 200
       :body   (if (= media-type "text/html")
                 (h/command-hiccup command)
                 (display-command command))})))

(defhandler get-event
  {:summary    "Get a event by id"
   :parameters {:path-params {:id s/Uuid}}
   :responses  {200 {:body Event}
                404 {:body s/Str}}}
  [{:keys [component] :as request}]
  (let [event    (api/get-event-by-id (:api component)
                                      (-> request :path-params :id))

        ;; TODO: fix content negotiation
        media-type (get-in request [:headers "accept"])]
    (log/info :endpoint ::get-event :event event :media-type media-type)
    ;; TODO: handle 404 case more gracefully
    (when event
      {:status 200
       :body   (if (= media-type "text/html")
                 (h/event-hiccup event)
                 (display-command event))})))

(defn component-interceptor
  [component]
  (interceptor {:name  ::component-interceptor
                :enter (fn [context]
                         (log/debug :interceptor ::component-interceptor :phase :enter)
                         (assoc-in context [:request :component] component))}))

;;; TODO: authentication
(def authentication-interceptor
  (interceptor {:name ::authentication-interceptor
                :enter (fn [context]
                         (log/debug :interceptor ::authentication-interceptor :phase :enter)
                         context)}))

;; TODO Websocket sub-protocols for commands as JSON, transit, edn, etc.
#_(defn build-websocket-routes
    [component]
    {"/updates/commands" {:on-connect (ws/start-ws-connection new-ws-client)
                          :on-text (fn [msg] (log/info :msg (str "A client sent - " msg)))
                          :on-binary (fn [payload offset length] (log/info :msg "Binary Message!" :bytes payload))
                          :on-error (fn [t] (log/error :msg "WS Error happened" :exception t))
                          :on-close (fn [num-code reason-text]
                                      (log/info :msg "WS Closed:" :reason reason-text))}
     "/updates/events"  {:on-connect (ws/start-ws-connection new-ws-client)
                         :on-text    (fn [msg] (log/info :msg (str "A client sent - " msg)))
                         :on-binary  (fn [payload offset length] (log/info :msg "Binary Message!" :bytes payload))
                         :on-error   (fn [t] (log/error :msg "WS Error happened" :exception t))
                         :on-close   (fn [num-code reason-text]
                                       (log/info :msg "WS Closed:" :reason reason-text))}})

(defn sse-xf
  [event-type user-id]
  (comp (filter (constantly true)) ;; TODO filter for user-id authorization here
        (map (fn [event]
               (log/spy {:id   (-> event :key str)
                         :name (-> event-type name str)
                         :data (some-> event :value json/generate-string)})))))

(defn pipeline-to-sse
  [ctx event-type src-ch dest-ch]
  (let [user-id       (get-in ctx [:request :headers "user-id"])
        last-event-id (get-in ctx [:request :headers "last-event-id"])

        ;; TODO: based on last-event-id (if present), find the next event id
        start-value   0]
    (log/debug ::pipeline-to-sse {:src-ch        src-ch
                                  :dest-ch       dest-ch
                                  :event-type    event-type
                                  :user-id       user-id
                                  :last-event-id last-event-id
                                  :start-value   start-value})
    (a/pipeline 10 dest-ch (sse-xf event-type user-id) src-ch)))

(defn commands-stream-ready
  "Starts sending user-filtered command updates to client."
  [event-ch ctx]
  (pipeline-to-sse ctx
                   :command
                   (api/commands-ch (get-in ctx [:request :component :api]))
                   event-ch))

(defn events-stream-ready
  "Starts sending user-filtered result updates to client."
  [event-ch ctx]
  (pipeline-to-sse ctx
                   :result
                   (api/events-ch (get-in ctx [:request :component :api]))
                   event-ch))

(defn build-routes
  [component]
  ["/" ^:interceptors [(component-interceptor component)]
   ["/commands" ^:interceptors [papi/error-responses
                                (papi/negotiate-response)
                                (papi/body-params)
                                papi/common-body
                                (papi/coerce-request)
                                (papi/validate-response)
                                (papi/doc {:tags ["commands"]})
                                authentication-interceptor
                                ensure-processable]
    {:get  all-commands
     :post create-command}
    ["/updates" {:get [::command-updates (sse/start-event-stream commands-stream-ready)]}]
    ["/:id" {:get get-command}]]
   ["/events" ^:interceptors [papi/error-responses
                              (papi/negotiate-response)
                              (papi/body-params)
                              papi/common-body
                              (papi/coerce-request)
                              (papi/validate-response)
                              (papi/doc {:tags ["events"]})
                              authentication-interceptor
                              ensure-processable]
    {:get  all-events}
    ["/events/updates" {:get [::event-updates (sse/start-event-stream events-stream-ready)]}]

    ["/:id" {:get get-event}]]])

(defrecord Endpoints [api routes websocket-routes]
  component/Lifecycle
  (start [this] (assoc this :routes (build-routes this)))
  (stop [this] (dissoc this :routes)))

(defn construct-commander-rest-endpoints
  []
  (map->Endpoints {}))
