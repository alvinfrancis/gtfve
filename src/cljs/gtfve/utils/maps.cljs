(ns gtfve.utils.maps
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [put! chan <! close!]]))

(defonce Maps google.maps)

(defonce Data Maps.Data)

(defonce map-types (js->clj Maps.MapTypeId :keywordize-keys true))

(defonce default-map-opts {:center {:lat 14.653386
                                    :lng 121.032520}
                           :mapTypeId (:ROADMAP map-types)
                           :zoom 15})

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
