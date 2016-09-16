;; Copyright 2016 Capital One Services, LLC

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;;     http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and limitations under the License.

(ns com.capitalone.commander.rest.hiccup
  (:require [hiccup.page :refer [html5]]
            [io.pedestal.http.route :refer [url-for]]))

(set! *warn-on-reflection* true)

(defn layout
  [& body]
  (html5
   [:head {:lang "en"}
    [:meta {:charset "UTF-8"}]
    [:title "Commander"]
    [:link {:rel "stylesheet" :href "/css/pure-min-0.6.0.css"}]
    "<!--[if lt IE 9]>"
    [:script {:src "/js/html5shiv-printshiv.min.js"}]
    "<![endif]-->"
    [:style "#main {width: 75%; margin: 0px auto;}"]]
   [:body
    [:div#main
     [:div.pure-menu.pure-menu-horizontal
      [:a.pure-menu-heading.pure-menu-link
       {:href "/"} "Commander"]
      [:ul.pure-menu-list
       [:li.pure-menu-item
        [:a.pure-menu-link {:href (url-for ::all-commands)} "Commands"]]
       [:li.pure-menu-item
        [:a.pure-menu-link {:href "#"} "Updates"]]]]
     body]]))

;; TODO: pagination
(defn commands-hiccup
  [{:keys [commands limit offset count] :as commands-result} sync]
  (layout
   [:div.commands
    [:h2 "Commands"]
    [:form {:method "post"
            :class  "pure-form pure-form-aligned"}
     [:fieldset
      [:legend "Create a Command"]
      [:div.pure-control-group
       [:label {:for "command"} "Command"]
       [:textarea {:name  "command"
                   :id    "command"
                   :value nil}]]

      [:div.pure-controls
       [:label {:for "sync" :class "pure-checkbox"}
        [:input {:type    "checkbox"
                 :name    "sync"
                 :id      "sync"
                 :value   "true"
                 :checked sync}]
        "&nbsp;Synchronous request?"]
       [:button {:type "submit" :class "pure-button pure-button-primary"}
        "Submit"]]]]
    (if (seq commands)
      [:table.commands.pure-table.pure-table-bordered
       [:thead
        [:tr [:th "edn"] [:th "id"] [:th "time"]]]
       [:tbody
        (for [command commands]
          (let [id (:id command)]
            [:tr
             [:td.pr-str (pr-str command)]
             [:td.id
              [:a {:href (url-for ::get-command :params {:id id})}
               id]]
             [:td.time (:timestamp command)]]))]]
      [:p "No commands yet."])]))

(defn command-hiccup
  [{:keys [id] :as command}]
  (layout
   [:h2 "Command"]
   [:pre (pr-str command)]
   [:a.pure-button {:href (url-for ::get-command :params {:id id})}
    "View command"]
   [:a.pure-button {:href (url-for ::all-commands)}
    "Back to all commands"]))

;; TODO: pagination
(defn events-hiccup
  [{:keys [events limit offset count] :as events-result} sync]
  (layout
   [:div.events
    [:h2 "Events"]
    [:form {:method "post"
            :class  "pure-form pure-form-aligned"}
     [:fieldset
      [:legend "Create a Event"]
      [:div.pure-control-group
       [:label {:for "event"} "Event"]
       [:textarea {:name  "event"
                   :id    "event"
                   :value nil}]]

      [:div.pure-controls
       [:label {:for "sync" :class "pure-checkbox"}
        [:input {:type    "checkbox"
                 :name    "sync"
                 :id      "sync"
                 :value   "true"
                 :checked sync}]
        "&nbsp;Synchronous request?"]
       [:button {:type "submit" :class "pure-button pure-button-primary"}
        "Submit"]]]]
    (if (seq events)
      [:table.events.pure-table.pure-table-bordered
       [:thead
        [:tr [:th "edn"] [:th "id"] [:th "time"]]]
       [:tbody
        (for [event events]
          (let [id (:id event)]
            [:tr
             [:td.pr-str (pr-str event)]
             [:td.id
              [:a {:href (url-for ::get-event :params {:id id})}
               id]]
             [:td.time (:timestamp event)]]))]]
      [:p "No events yet."])]))

(defn event-hiccup
  [event]
  (layout
   [:h2 (str "Event " (:id event))]
   [:pre (pr-str event)]
   [:a.pure-button {:href (url-for ::all-events)}
    "Back to all events"]))
