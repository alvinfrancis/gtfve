(ns gtfve.controllers.controls
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [cljs.core.async :as async :refer [put! <! close!]]))

(defmulti control-event
  (fn [event args state cursors] event))

(defmethod control-event :default [_]
  (println "Unknown control"))

(defmethod control-event :side-panel-changed [_ [key] state cursors]
  (let [panel (:panel cursors)]
    (om/update! (panel) :tab key)))
