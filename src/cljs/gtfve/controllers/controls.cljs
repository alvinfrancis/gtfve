(ns gtfve.controllers.controls
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [cljs.core.async :as async :refer [put! <! close!]]))

(defmulti control-event
  (fn [event args state] event))

(defmethod control-event :default [_]
  (println "Unknown control"))

(defmethod control-event :side-panel-changed [_ [key] state]
  (om/update! (:panel state) :tab key))
