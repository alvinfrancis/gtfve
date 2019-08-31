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
                              :get "/data"
                              :response-format :edn
                              :params {:query (pr-str {[:app/stops-search query]
                                                       [:db/id
                                                        :stop/id
                                                        :stop/name
                                                        :stop/latitude
                                                        :stop/longitude]})}))]
          (<! (async/timeout 1000))
          (put! (:api comms) [:stops-search (:status api-result)
                              api-result query])))))

(defmethod control-event :stops-search-result-clicked [_ {:keys [stop]} state cursors]
  (let [editor (:editor cursors)
        lat (:stop/latitude stop)
        lng (:stop/longitude stop)]
    (om/update! (editor) :update-render? true)
    (om/update! (editor) [:map-options :center] {:lat lat :lng lng})))

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

(defmethod control-event :maps-bounds-changed
  [_ {:keys [bbox update-render?]
      :or {update-render? false}}
   state cursors]
  (let [editor (:editor cursors)
        comms (get-in state [:comms])
        [sw ne] bbox
        [[x1 y1] [x2 y2]] bbox
        bbox-width (- x2 x1)
        bbox-height (- y2 y1)
        expanded-bbox [[(- x1 bbox-width)
                        (- y1 bbox-height)]
                       [(+ x2 bbox-width)
                        (+ y2 bbox-height)]]]
    (om/update! (editor) [:map-state :bounds] {:sw (vec sw) :ne (vec ne)})
    (go (let [api-result (<! (ajax/managed-ajax
                              :get "/viewport"
                              :response-format :edn
                              :params {:bbox (pr-str expanded-bbox)
                                       :pulls (pr-str [:stops-pull [:db/id :stop/id :stop/latitude :stop/longitude]])}))]
          (<! (async/timeout 1000))
          (put! (:api comms) [:stops-view (:status api-result) api-result])))
    ))

(defmethod control-event :maps-center-changed
  [_ {:keys [lat lng update-render?] :or {update-render? false}} _ cursors]
  (let [editor (:editor cursors)]
    (when update-render?
      (om/update! (editor) :update-render? true))
    (om/update! (editor) [:map-options :center] {:lat lat :lng lng})))

(defmethod control-event :change-added
  [_ change _ cursors]
  (let [data (:data cursors)]
    (om/transact! (data) :changes #(conj % change))))

(defmethod control-event :change-retracted
  [_ pos _ cursors]
  (let [data (:data cursors)]
    (om/transact! (data) :changes
                  #(vec (concat
                         (subvec % 0 pos)
                         (subvec % (inc pos)))))))

(defmethod control-event :changes-cleared
  [_ _ _ cursors]
  (let [data (:data cursors)]
    (om/update! (data) :changes [])))
