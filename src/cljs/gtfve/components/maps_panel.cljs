(ns gtfve.components.maps-panel
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [gtfve.async :refer [raise!]]
            [gtfve.utils :as utils]
            [gtfve.utils.maps :as maps]
            [gtfve.components.maps-canvas.google :as google]
            [goog.events :as gevents]))

(defn map-toolbar-button [{:keys [link control active?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Map Toolbar Button")
    om/IRender
    (render [_]
      (html
       [:a {:className
            (clojure.string/join " "
                                 ["btn" "btn-primary" "maps-button"
                                  (when active? "active")])
            :on-click (fn [e]
                        (raise! owner [control])
                        (.preventDefault e))}
        link]))))

(defn maps-buttons [{:keys [stops?]} _]
  (reify
    om/IDisplayName (display-name [_] "Maps Buttons")
    om/IRender
    (render [_]
      (html
       [:div.btn-toolbar
        [:div.btn-group {:style {:float "right"}}
         #_(om/build map-toolbar-button {:link [:i.material-icons "add_circle"]})
         (om/build map-toolbar-button {:link [:i.material-icons "location_on"]
                                       :control :stops-editor-toggled
                                       :active? stops?})
         #_(om/build map-toolbar-button {:link [:i.material-icons "search"]})]]))))

(defn maps-toolbar [ui _]
  (reify
    om/IDisplayName (display-name [_] "Maps Toolbar")
    om/IRender
    (render [_]
      (html [:div.maps-toolbar
             (om/build maps-buttons (:modes ui))]))))

(defn maps-panel [{:keys [ui data]} _]
  (reify
    om/IDisplayName (display-name [_] "Maps Panel")
    om/IRender
    (render [_]
      (html [:div.maps-panel
             (om/build maps-toolbar ui)
             (om/build google/maps-canvas {:ui ui :data data})]))))
