(ns gtfve.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react])
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

(defn viewport
  "Component to show google map"
  []
  (reagent/create-class
   {:component-did-mount (fn [this]
                           (let [opts default-opts
                                 node (.getDOMNode this)]
                             (reset! gmap (Maps.Map. node (clj->js opts)))))
    :component-function (fn [] [:div {:id :map-canvas
                                      :style {:height "100%"
                                              :margin 0
                                              :padding 0}}])}))

(defn home-page []
  [:div [viewport]])

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
  (hook-browser-navigation!)
  (reagent/render [current-page] (.getElementById js/document "app")))
