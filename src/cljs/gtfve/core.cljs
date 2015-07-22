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
            [gtfve.state :as state]
            [om-i.core :as omi]
            [om-i.hacks :as omihacks])
  (:import goog.History))

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

(defonce api-ch (chan))

(def debug-state nil)

(defn app-state []
  (atom (assoc (state/initial-state)
               :comms {:controls controls-ch
                       :errors errors-ch
                       :api api-ch
                       :controls-mult (async/mult controls-ch)
                       :errors-mult (async/mult errors-ch)
                       :api-mult (async/mult api-ch)})))

;; -------------------------
;; Initialize app

(defn find-app-container []
  (. js/document (getElementById "app")))

(defonce omi-setup (omi/setup-component-stats!))

(defn install-om! [state container comms]
  (om/root
   app/app
   state
   {:target container
    :shared {:comms comms
             :cursors (state/create-cursors state)}
    :instrument (fn [f cursor m]
                  (om/build* f cursor
                             (assoc m
                                    :descriptor omi/instrumentation-methods)))}))

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
