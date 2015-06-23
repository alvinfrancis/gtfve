(ns gtfve.maps
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r :refer [atom]]
            [secretary.core :as secretary :include-macros true]
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

(defn r-map-object [class opts gmap]
  (let [obj (class. (clj->js @opts))]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [[_ _ opts gmap] (r/argv this)
                                   m-opts (merge @opts {:map gmap})]
                               (.setOptions obj (clj->js m-opts))))
      :component-did-update (fn [this]
                              (let [[_ _ opts gmap] (r/argv this)
                                    m-opts (merge @opts {:map gmap})]
                                (.setOptions obj (clj->js m-opts))))
      :component-will-unmount (fn [this]
                                (.setMap obj nil))
      :component-function (fn [class opts gmap]
                            [:noscript])})))

(defn r-marker [opts gmap]
  (let [marker (Maps.Marker. (clj->js @opts))]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [[_ opts gmap] (r/argv this)
                                   m-opts (merge @opts {:map gmap})]
                               (.setOptions marker (clj->js m-opts))))
      :component-did-update (fn [this]
                              (let [[_ opts gmap] (r/argv this)
                                    m-opts (merge @opts {:map gmap})]
                                (.setOptions marker (clj->js m-opts))))
      :component-will-unmount (fn [this]
                                (.setMap marker nil))
      :component-function (fn [opts gmap]
                            [:noscript])})))

(defn r-polyline [opts gmap]
  (let [polyline (Maps.Polyline. (clj->js @opts))]
    (r/create-class
     {:component-function (fn [opts gmap]
                            [:noscript])})))

(defn map-listen
  "Return a channel listening on events of type in obj."
  [obj type]
  (let [out (chan)]
    (-> Maps .-event
        (.addListener obj type (fn [e]
                                 (put! out (if e e :msg)))))
    out))

(defn r-map [state]
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
                                           :margin 0
                                           :padding 0}}
                             (doall
                              (for [[id marker-opts] (:markers @state)]
                                ^{:key id}
                                [r-map-object Maps.Marker
                                 (r/wrap marker-opts swap! state assoc-in [:markers id])
                                 @gmap]))])})))

(declare route trip stop-time stop)
(defn route [route gmap]
  (let [{trips :route/trips} @route]
    (for [{id :trip :as trip-data} trips]
      ^{:key id}
      [trip
       (r/wrap trip-data swap! route assoc-in [:route/trips id])
       gmap])))

(defn trip [trip gmap]
  (let [stop-times (->> @trip
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
                                           :margin 0
                                           :padding 0}}
                             (doall
                              (for [{id :route/id
                                     :as route-data} (:routes @state)]
                                ^{:key id}
                                [route route-data @gmap]))])})))
