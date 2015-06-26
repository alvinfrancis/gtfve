(ns gtfve.maps
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [gtfve.data :as d])
  (:import goog.History))

(defonce Maps google.maps)

(defonce map-types (js->clj Maps.MapTypeId :keywordize-keys true))

(defonce default-map-opts {:center {:lat 14.653386
                                    :lng 121.032520}
                           :mapTypeId (:ROADMAP map-types)
                           :zoom 15})

(defonce default-polyline-opts {:geodesic true
                                :strokeColor "#0000FF"
                                :strokeOpacity 1.0
                                :strokeWeight 2})

(defonce default-marker-opts {:icon "http://www.google.com/intl/en_us/mapfiles/ms/micons/blue-dot.png"})

(defn snap-to-road [path]
  (d/GET "https://roads.googleapis.com/v1/snapToRoads"
      {:params {:interpolate true
                :key "AIzaSyBk0sVuhqBd8MH8yWyRVpU40vULs0nKlG8"
                :path (->> path
                           (map (partial clojure.string/join ","))
                           (clojure.string/join "|"))}
       :response-format :json
       :keywords? true}))

(defn create-marker [[lat lng] gmap]
  (Maps.Marker.
   (clj->js {:position (Maps.LatLng. lat lng)
             :icon "http://www.google.com/intl/en_us/mapfiles/ms/micons/blue-dot.png"
             :title (str lat "|" lng)
             :map gmap})))

(defn draw-polyline
  "Draws a polyline onto map given a collection of LAT LNG pairs"
  [coords gmap & {:keys [opts] :or {opts {:geodesic true
                                          :strokeColor "#0000FF"
                                          :strokeOpacity 1.0
                                          :strokeWeight 2}}}]
  (let [maps-coords (map (fn [[lat lng]]
                           (Maps.LatLng. lat lng))
                         coords)
        n-opts (merge {:path maps-coords} opts)
        polyline (Maps.Polyline. (clj->js n-opts))]
    (doseq [latlng coords]
      (create-marker latlng gmap))
    (.setMap polyline gmap)))

(defn draw-snapped-polyline [path gmap]
  (go
    (let [res (<! (snap-to-road path))
          path (->> (:snappedPoints res)
                    (map :location)
                    (map (fn [{:keys [latitude longitude]}]
                           [latitude longitude])))]
      (draw-polyline path gmap :opts {:geodesic true
                                      :strokeColor "#FF0000"
                                      :strokeOpacity 1.0
                                      :strokeWeight 2}))))

(defn map-object [data owner]
  (reify
    om/IRender
    (render [_]
      [:noscript])
    om/IInitState
    (init-state [_]
      (let [{:keys [class opts]} data]
        {:object (class. (clj->js opts))}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [opts gmap]} data
            object (om/get-state owner :object)]
        (->> {:map gmap}
             (merge opts)
             (clj->js)
             (.setOptions object))))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [opts gmap]} data
            object (om/get-state owner :object)]
        (->> {:map gmap}
             (merge opts)
             (clj->js)
             (.setOptions object))))
    om/IWillUnmount
    (will-unmount [_]
      (let [object (om/get-state owner :object)]
        (.setMap object nil)))))

(defn e-map [data owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [{:keys [opts]} data
            node (om/get-node owner)]
        (om/set-state! owner :gmap (Maps.Map. node (clj->js opts)))))
    om/IRenderState
    (render-state [_ state]
      (html [:div {:id :map-canvas}]))))

(defn map-listen
  "Return a channel listening on events of type in obj."
  [obj type]
  (let [out (chan)]
    (-> Maps .-event
        (.addListener obj type (fn [e]
                                 (put! out (if e e :msg)))))
    out))

(declare route trip stop-time stop)
(defn route [route gmap]
  (let [{trips :route/trips} route]
    (for [{id :trip/id :as trip-data} trips]
      ^{:key id}
      [trip trip-data gmap])))
#_ (letfn [(reset-fn [state ks id-fn id data]
             (swap! state assoc-in ks
                    (map (if (= (id-fn %) id)
                           data %)
                         (get-in state ks))))]
     [trip (r/wrap trip-data reset-fn id) gmap])

(defn trip [trip gmap]
  (let [stop-times (->> trip
                        :trip/stops
                        (sort-by :stop-time/stop-sequence))
        stops (map :stop-time/stop stop-times)
        path (->> stop-times
                  (map :stop-time/stop)
                  (map (juxt :stop/latitude
                             :stop/longitude))
                  (map (fn [[lat lng]]
                         (Maps.LatLng. lat lng))))]
    ;; TODO: add markers
    [:span
     [r-map-object Maps.PolyLine (merge default-polyline-opts {:path path}) gmap]]))

(defn stop-time [stop-time gmap]
  (let [{stop-data :stop-time/stop
         index :stop-time/stop-sequence
         arrival-time :stop-time/arrival-time
         departure-time :stop-time/departure-time} @stop-time]))

(defn stop [data gmap]
  (let [{lat :stop/latitude
         lng :stop/longitude
         name :stop/name} @data]
    [r-map-object Maps.Marker
     (merge default-marker-opts
            {:position (Maps.LatLng lat lng)
             :title name})]))

(defn route-map [state]
  (let [gmap (atom nil)]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [opts (:opts @state)
                                   node (.getDOMNode this)
                                   google-map (Maps.Map. node (clj->js opts))]
                               (reset! gmap google-map)))
      :component-function (fn [state]
                            [:div {:id :map-canvas
                                   :style {:height "100%"
                                           :width "100%"}}
                             (doall
                              (for [{id :route/id
                                     :as route-data} (:routes @state)]
                                ^{:key id}
                                [route route-data @gmap]))])})))
