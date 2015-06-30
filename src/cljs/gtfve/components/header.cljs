(ns gtfve.components.header
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]))

(defn header [_ _]
  (reify
    om/IDisplayName (display-name [_] "Header")
    om/IRender
    (render [_]
      (html [:nav.navbar.navbar-default.navbar-fixed-top.site-header
             [:div.container
              [:header.navbar-header
               [:a.navbar-brand {:href "#"} "GTFVE"]]]]))))
