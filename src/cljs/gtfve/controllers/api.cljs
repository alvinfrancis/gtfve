(ns gtfve.controllers.api
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [cljs.core.async :as async :refer [put! <! close!]]
            [gtfve.utils :as utils]))

(defmulti api-event
  (fn [event status args state cursors] [event status]))

(defmethod api-event :default
  [event status args state cursors]
  (.apply (.-error js/console) js/console (clj->js [event status args])))

(defmethod api-event [:stops-search :success] [event status [result query] state cursors]
  (let [data (:data cursors)
        stops-panel (:stops-panel cursors)
        results (:response result)]
    (om/update! (stops-panel) :last-query query)
    (om/update! (stops-panel) :loading? false)
    (om/update! (data) :stops-search-results results)))

(defmethod api-event [:stops-view :success] [event status [result] state cursors]
  (let [data (:data cursors)
        stops-panel (:stops-panel cursors)
        results (:response result)
        key-val-xf (map (fn [stop] [(:db/id stop) stop]))
        results-map (transduce key-val-xf conj {} results)]
    (om/transact! (data) :entities #(utils/deep-merge % results-map))
    (om/update! (data) :stops (keys results-map))))
