(ns gtfve.components.maps-panel
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]))

(defn maps-toolbar [_ _]
  (reify
    om/IDisplayName (display-name [_] "Maps Toolbar")
    om/IRender
    (render [_]
      (html [:div.maps-toolbar]))))

(defn maps-panel [_ _]
  (reify
    om/IDisplayName (display-name [_] "Maps Panel")
    om/IRender
    (render [_]
      (html [:div.maps-panel
             (om/build maps-toolbar nil)
             [:div
              [:p "Maps Panel"]]]))))
