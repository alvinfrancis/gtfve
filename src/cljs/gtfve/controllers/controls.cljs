(ns gtfve.controllers.controls
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [cljs.core.async :as async :refer [put! <! close!]]
            [gtfve.utils.ajax :as ajax]))

(defmulti control-event
  (fn [event args state cursors] event))

(defmethod control-event :default [_]
  (println "Unknown control"))

(defmethod control-event :side-panel-changed [_ {:keys [key]} _ cursors]
  (let [panel (:panel cursors)]
    (om/update! (panel) :tab key)))

(defmethod control-event :stops-search-submitted [_ {:keys [query]} state cursors]
  (let [comms (get-in state [:comms])
        stops-panel (:stops-panel cursors)]
    (om/update! (stops-panel) :loading? true)
    (go (let [api-result (<! (ajax/managed-ajax
                              :get "/stops-search"
                              :response-format :edn
                              :params {:query query
                                       :pull (pr-str [:stop/id
                                                      :stop/name
                                                      :stop/latitude
                                                      :stop/longitude])}))]
          (<! (async/timeout 1000))
          (put! (:api comms) [:stops-search (:status api-result)
                              api-result query])))))

(defmethod control-event :stops-editor-toggled [_ _ _ cursors]
  (let [editor (:editor cursors)]
    (om/transact! (editor) [:modes :stops?] not)))

(declare control-event-input)
(defmethod control-event :edited-input [_ {:keys [key value]} state cursors]
  (control-event-input key value state cursors))

(defmulti control-event-input
  (fn [key value state cursors] key))

(defmethod control-event-input :default [_]
  (println "Unknown control"))

(defmethod control-event-input :input-stops-search [_ value _ cursors]
  (let [panel (:panel cursors)]
    (om/update! (panel) [:stops :query] value)))

(defmethod control-event :maps-updated-render [_ _ _ cursors]
  (let [editor (:editor cursors)]
    (om/update! (editor) :update-render? false)))
