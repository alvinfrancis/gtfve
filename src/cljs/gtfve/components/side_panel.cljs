(ns gtfve.components.side-panel
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]))

(defn side-panel [_ _]
  (reify
    om/IDisplayName (display-name [_] "Side Panel")
    om/IRender
    (render [_]
      (html [:div.side-panel {:style {:background-color "#efefef" :height "100%"}}
             [:div
              [:p.btn.btn-primary.btn-lg.btn-block "Side Panels"]]]))))
