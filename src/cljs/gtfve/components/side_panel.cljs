(ns gtfve.components.side-panel
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]))

(defn side-panel [_ _]
  (reify
    om/IDisplayName (display-name [_] "Side Panel")
    om/IRender
    (render [_]
      (html [:div.side-panel
             [:div
              [:form.form-horizontal
               [:fieldset
                [:div.form-group
                 [:label.col-sm-2.control-label {:for "inputSearch"} "Stop"]
                 [:div.col-sm-10
                  [:input.form-control#inputSearch {:type "text"
                                                    :placeholder "Search"}]]]]]
              [:table.table.table-striped.table-hover
               [:thead
                [:tr
                 [:th "#"]
                 [:th "Code"]
                 [:th "Name"]
                 [:th "Description"]
                 [:th "Lat/Lng"]]]
               [:tbody]]]]))))
