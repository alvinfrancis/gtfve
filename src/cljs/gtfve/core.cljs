(ns gtfve.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [gtfve.maps :as m]
            [gtfve.data :as data])
  (:import goog.History))

;; -------------------------
;; Maps

(defonce Maps google.maps)

(defonce map-types (js->clj Maps.MapTypeId :keywordize-keys true))

(defonce default-opts {:center {:lat 14.653386
                                :lng 121.032520}
                       :mapTypeId (:ROADMAP map-types)
                       :zoom 15})

(defonce gmap (atom nil))

;; -------------------------
;; Views

(defn home-page [data owner]
  (om/component
   (html [:div [:h2 "Home Page"]
          [:div [:a {:href "#/about"} "go to the about page"]]])))

(defn about-page [data owner]
  (om/component
   (html [:div [:h2 "About gtfve"]
          [:div [:a {:href "#/"} "go to the home page"]]])))

(defmulti page #(:current-page %))

(defmethod page nil
  [data] home-page)

(defmethod page :home
  [state] home-page)

(defmethod page :about
  [state] about-page)

(defn current-page [data owner]
  (om/component
   (om/build (page (om/value data)) data)))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/swap! assoc
                 :current-page :home
                 :map {:opts m/default-map-opts}))

(secretary/defroute "/about" []
  (session/put! :current-page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn init! []
  (hook-browser-navigation!)
  (om/root current-page
           session/state
           {:target (. js/document (getElementById "app"))}))
