(ns gtfve.components.maps-panel
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [gtfve.async :refer [raise!]]
            [gtfve.utils :as utils]
            [gtfve.utils.maps :as maps]
            [goog.events :as gevents]))

(defonce Maps google.maps)

(defonce map-types (js->clj Maps.MapTypeId :keywordize-keys true))

(defonce default-map-opts {:center {:lat 14.653386
                                    :lng 121.032520}
                           :mapTypeId (:ROADMAP map-types)
                           :zoom 15})

(defn maps-canvas [ui owner]
(defn maps-canvas-dev [ui _]
  (reify
    om/IRenderState
    (render-state [_ {:keys [gmap] :as state}]
      (html
       [:div.panel.panel-info {:style {:position "fixed"
                                       :bottom "10px"
                                       :right "10px"}}
        [:div.panel-heading "Map Info"]
        [:div.panel-body
         (when gmap
           [:p (pr-str (:bounds state))])]]))))

  (reify
    om/IInitState
    (init-state [_]
      {:opts default-map-opts
       :gmap nil
       :bounds nil
       :kill-ch (chan)})
    om/IDisplayName (display-name [_] "Maps View")
    om/IDidMount
    (did-mount [_]
      (let [opts (om/get-state owner :opts)
            node (om/get-node owner "gmap")
            google-map (Maps.Map. node (clj->js opts))
            bounds-changed-ch (-> (maps/listen google-map "bounds_changed")
                                  (utils/debounce 100))
            kill-ch (om/get-state owner :kill-ch)]
        (om/set-state! owner :gmap google-map)
        (go-loop []
          (when-let [[v ch] (alts! [kill-ch bounds-changed-ch])]
            (if (= ch kill-ch)
              ::done
              (do
                (om/set-state! owner :bounds (.. google-map (getBounds)))
                (recur)))))))
    om/IWillUnmount
    (will-unmount [_]
      (when-let [kill-ch (:kill-ch (om/get-state owner))]
        (put! kill-ch (js/Date.))))
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
             (om/build maps-canvas ui)]))))
