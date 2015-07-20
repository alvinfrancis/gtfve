(ns gtfve.utils.google-maps
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [put! chan <! close!]]))

(defonce Maps google.maps)

(defonce Data Maps.Data)

(defonce map-types (js->clj Maps.MapTypeId :keywordize-keys true))

(defonce default-map-opts {:center {:lat 14.653386
                                    :lng 121.032520}
                           :mapTypeId (:ROADMAP map-types)
                           :zoom 15})
(extend-type Maps.LatLng
  IIndexed
  (-nth
    ([this n] (nth (seq this) n))
    ([this n not-found] (nth (seq this) n not-found)))
  ILookup
  (-lookup
    ([this key] (get (js->clj this) key))
    ([this key not-found] (or (get (js->clj this) key) not-found)))
  ISeqable
  (-seq [this] (list (.lat this) (.lng this)))
  IEncodeClojure
  (-js->clj [this options] {:lat (.lat this) :lng (.lng this)}))

(extend-type Data.Point
  IIndexed
  (-nth
    ([this n] (nth (seq (.get this)) n))
    ([this n not-found] (nth (seq (.get this)) n not-found)))
  ILookup
  (-lookup
    ([this key] (get (js->clj this) key))
    ([this key not-found] (or (get (js->clj this) key) not-found)))
  ISeqable
  (-seq [this] (seq (.get this)))
  IEncodeClojure
  (-js->clj [this options] (apply js->clj (.get this) options)))

(extend-type Maps.LatLngBounds
  IIndexed
  (-nth
    ([this n] (nth (seq this) n))
    ([this n not-found] (nth (seq this) n not-found)))
  ILookup
  (-lookup
    ([this key] (get {:sw (.getSouthWest this)
                      :ne (.getNorthEast this)} key))
    ([this key not-found] (or (get {:sw (.getSouthWest this)
                                    :ne (.getNorthEast this)} key)
                              not-found)))
  ISeqable
  (-seq [this] (list (.getSouthWest this) (.getNorthEast this)))
  IEncodeClojure
  (-js->clj [this options] {:sw (js->clj (.getSouthWest this))
                            :ne (js->clj (.getNorthEast this))}))

(defn info-window []
  (Maps.InfoWindow.))

(defn size [x y]
  (Maps.Size. 0 -30))

(defn latlng [x y]
  (Maps.LatLng. x y))

(defn data-point [x y]
  (Data.Point. (latlng x y)))

(defn marker
  [[lat lng] & {:as options}]
  (Maps.Marker.
   (clj->js (merge {:position (latlng lat lng)
                    :icon {:path Maps.SymbolPath.CIRCLE
                           :strokeColor "#FF0000"
                           :scale 3}}
                   options))))

(defn data-listen
  ([data type]
   (data-listen data type (chan)))
  ([data type c]
   (.. data (addListener type (fn [e] (put! c (if e e (js/Date.))))))
   c))

(defn listen
  ([gmap type]
   (listen gmap type (chan)))
  ([gmap type c]
   (.. Maps -event (addListener gmap type (fn [e] (put! c (if e e (js/Date.))))))
   c))
