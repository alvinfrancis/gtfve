(ns gtfve.components.maps-panel
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]))

(defonce Maps google.maps)

(defonce map-types (js->clj Maps.MapTypeId :keywordize-keys true))

(defonce default-map-opts {:center {:lat 14.653386
                                    :lng 121.032520}
                           :mapTypeId (:ROADMAP map-types)
                           :zoom 15})

(defn maps-canvas [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      {:opts default-map-opts})
    om/IDisplayName (display-name [_] "Maps View")
    om/IDidMount
    (did-mount [_]
      (let [opts (om/get-state owner :opts)
            node (om/get-node owner)
            google-map (Maps.Map. node (clj->js opts))]
        (om/set-state! owner :map (Maps.Map. node (clj->js opts)))))
    om/IRenderState
    (render-state [_ state]
      (html [:div.maps-canvas {:style {:height "calc(100% - 48px)"}}]))))

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
             (om/build maps-canvas nil)]))))
