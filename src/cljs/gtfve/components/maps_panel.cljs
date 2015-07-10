(ns gtfve.components.maps-panel
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [gtfve.async :refer [raise!]]
            [gtfve.utils :as utils]
            [goog.events :as gevents]))

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
      {:opts default-map-opts
       :gmap nil})
    om/IDisplayName (display-name [_] "Maps View")
    om/IDidMount
    (did-mount [_]
      (let [opts (om/get-state owner :opts)
            node (om/get-node owner "gmap")
            google-map (Maps.Map. node (clj->js opts))]
        (om/set-state! owner :gmap (Maps.Map. node (clj->js opts)))))
    om/IRenderState
    (render-state [_ state]
      (html
       [:div.maps-viewport
        [:div.maps-canvas {:ref "gmap"}]]))))

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

(defn maps-toolbar [editor _]
  (reify
    om/IDisplayName (display-name [_] "Maps Toolbar")
    om/IRender
    (render [_]
      (html [:div.maps-toolbar
             (om/build maps-buttons (:modes editor))]))))

(defn maps-panel [editor _]
  (reify
    om/IDisplayName (display-name [_] "Maps Panel")
    om/IRender
    (render [_]
      (html [:div.maps-panel
             (om/build maps-toolbar editor)
             (om/build maps-canvas nil)]))))
