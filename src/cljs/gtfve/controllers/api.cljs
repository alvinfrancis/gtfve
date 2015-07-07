(ns gtfve.controllers.api
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [cljs.core.async :as async :refer [put! <! close!]]))

(defmulti api-event
  (fn [event status args state cursors] [event status]))

(defmethod api-event :default
  [event status args state cursors]
  (.apply (.-error js/console) js/console (clj->js [event status args])))

(defmethod api-event [:stops :success] [event status [args] state cursors]
  (let [data (:data cursors)
        results (:response args)]
    (om/update! (data) :stops results)))
