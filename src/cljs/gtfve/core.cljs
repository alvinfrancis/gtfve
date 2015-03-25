(ns gtfve.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
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

(defn home-page []
  [:div [m/r-map]])

(defn about-page []
  [:div [:h2 "About gtfve"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

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
  (data/init!)
  (hook-browser-navigation!)
  (reagent/render [current-page] (.getElementById js/document "app")))
