(ns gtfve.components.header
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]))

(defn header [_ _]
  (reify
    om/IDisplayName (display-name [_] "Header")
    om/IRender
    (render [_]
      (html [:header
             [:a {:href "#"} "BRAND"]]))))
