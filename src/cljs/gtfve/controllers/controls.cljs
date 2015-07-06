(ns gtfve.controllers.controls
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [cljs.core.async :as async :refer [put! <! close!]]))

(defmulti control-event
  (fn [event args state cursors] event))

(defmethod control-event :default [_]
  (println "Unknown control"))

(defmethod control-event :side-panel-changed [_ {:keys [key]} _ cursors]
  (let [panel (:panel cursors)]
    (om/update! (panel) :tab key)))

(defmethod control-event :stops-search-submitted [_ {:keys [query]} _ cursors]
  (println query))

(declare control-event-input)
(defmethod control-event :edited-input [_ {:keys [key value]} state cursors]
  (control-event-input key value state cursors))

(defmulti control-event-input
  (fn [key value state cursors] key))

(defmethod control-event-input :default [_]
  (println "Unknown control"))

(defmethod control-event-input :input-stops-search [_ value _ cursors]
  (let [panel (:panel cursors)]
    (om/update! (panel) :stops-query value)))
