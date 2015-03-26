(ns gtfve.maps
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [gtfve.data :as data])
  (:import goog.History))

(defonce Maps google.maps)

(defonce map-types (js->clj Maps.MapTypeId :keywordize-keys true))

(defonce default-map-opts {:center {:lat 14.653386
                                    :lng 121.032520}
                           :mapTypeId (:ROADMAP map-types)
                           :zoom 15})

(defonce default-marker-opts {:icon "http://www.google.com/intl/en_us/mapfiles/ms/micons/blue-dot.png"})

(defn snap-to-road [path]
  (data/GET "https://roads.googleapis.com/v1/snapToRoads"
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
      :component-function (fn [opts]
                            [:noscript])})))

(defn r-map [state]
  (let [gmap (atom nil)]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [opts (:opts @state)
                                   node (.getDOMNode this)]
                               (reset! gmap (Maps.Map. node (clj->js opts)))))
      :component-function (fn [state]
                            [:div {:id :map-canvas
                                   :style {:height "100%"
                                           :margin 0
                                           :padding 0}}
                             (doall
                              (for [[id marker-opts] (:markers @state)]
                                ^{:key id}
                                [r-marker
                                 (r/wrap marker-opts swap! state assoc-in [:markers id])
                                 @gmap]))])})))
