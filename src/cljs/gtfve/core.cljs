(ns gtfve.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [gtfve.maps :as m]
            [gtfve.data :as data]
            [gtfve.components.app :as app]
            [gtfve.state :as state])
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
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" [])

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
;; App State

(defonce controls-ch (chan))

(defonce errors-ch (chan))

(def debug-state nil)

(defn app-state []
  (atom (assoc (state/initial-state)
               :comms {:controls controls-ch
                       :errors errors-ch
                       :controls-mult (async/mult controls-ch)
                       :errors-mult (async/mult errors-ch)})))

;; -------------------------
;; Initialize app

(defn find-app-container []
  (. js/document (getElementById "app")))

(defn install-om! [state container comms]
  (om/root
   app/app
   state
   {:target container
    :shared {:comms comms
             :cursors (state/create-cursors state)}}))

(defn reinstall-om! []
  (install-om! debug-state (find-app-container) (:comms @debug-state)))

(defn main [state]
  (let [comms (:comms @state)
        container (find-app-container)]
    (install-om! state container comms)))

(defn setup! []
  (let [state (app-state)]
    (main state)
    (set! debug-state state)))
