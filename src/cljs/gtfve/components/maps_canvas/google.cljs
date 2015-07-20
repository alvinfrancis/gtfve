(ns gtfve.components.maps-canvas.google
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [gtfve.async :refer [raise!]]
            [gtfve.utils :as utils]
            [gtfve.utils.google-maps :as maps]
            [goog.events :as gevents]))

(defn maps-canvas-dev [data _]
  (reify
    om/IDisplayName (display-name [_] "Maps Canvas Dev")
    om/IRenderState
    (render-state [_ {:keys [gmap] :as state}]
      (html
       [:div.panel.panel-info {:style {:position "fixed"
                                       :bottom "10px"
                                       :right "10px"
                                       :z-index "10000"}}
        [:div.panel-heading "Map Info"]
        [:div.panel-body
         (when gmap
           [:div
            [:p (pr-str (js->clj (.getBounds gmap)))]
            [:p (count data) (pr-str [(:stop/latitude (last data))
                                      (:stop/longitude (last data))])]])]]))))

(defn stop-marker [data owner]
  (reify
    om/IDisplayName (display-name [_] "Stop Marker")
    om/IWillMount
    (will-mount [_]
      (let [gmap (om/get-state owner :gmap)
            marker (maps/marker [(:stop/latitude data)
                                 (:stop/longitude data)]
                                :map gmap)]
        (om/set-state! owner :marker marker)))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [marker]} (om/get-state owner)]
        (.setMap marker nil)))
    om/IRender
    (render [_]
      (html [:noscript (pr-str data)]))))


(defn search-stop-marker [data owner]
  (reify
    om/IDisplayName (display-name [_] "Search Stop Marker")
    om/IInitState
    (init-state [_]
      {:feature-options #js {:id (:stop/id data)
                             :properties #js {}
                             :geometry (let [{lat :stop/latitude
                                              lng :stop/longitude} data]
                                         (maps/data-point lat lng))}
       :kill-ch (chan)})
    om/IWillMount
    (will-mount [_]
      (let [{:keys [gmap
                    data-click-mult
                    info-window
                    feature-options kill-ch]} (om/get-state owner)
            data-click-tap (chan (async/sliding-buffer 5))
            feature (.. gmap -data (add feature-options))]
        (async/tap data-click-mult data-click-tap)
        (om/set-state! owner :feature feature)
        (go-loop []
          (let [[v ch] (alts! [kill-ch data-click-tap])]
            (if (= ch kill-ch)
              ::done
              (do
                (when (= (.-feature v) feature)
                  (doto info-window
                    (.setContent (html/render
                                  (html
                                   [:div.panel.panel-primary
                                    [:div.panel-heading (:stop/name data)]
                                    [:div.panel-body (pr-str data)]])))
                    (.setPosition (.. v -feature (getGeometry) (get)))
                    (.setOptions #js {:pixelOffset (maps/size 0 -30)})
                    (.open gmap)))
                (recur))))
          (.close info-window)
          (.. gmap -data (remove feature))
          (async/untap data-click-mult data-click-tap))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [kill-ch]} (om/get-state owner)]
        (put! kill-ch (js/Date.))))
    om/IRender
    (render [_]
      (html [:noscript (pr-str data)]))))

(defn maps-canvas [{:keys [ui data]} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:opts maps/default-map-opts
       :gmap nil
       :bounds nil
       :info-window (maps/info-window)
       :data-click-ch (chan)
       :kill-ch (chan)})
    om/IDisplayName (display-name [_] "Maps View")
    om/IDidMount
    (did-mount [_]
      (let [opts (om/get-state owner :opts)
            node (om/get-node owner "gmap")
            google-map (maps/Maps.Map. node (clj->js opts))
            data (.-data google-map)
            idle-ch           (maps/listen google-map "idle")
            bounds-changed-ch (-> (maps/listen google-map "bounds_changed")
                                  (utils/debounce 100))
            center-changed-ch (-> (maps/listen google-map "center_changed")
                                  (utils/debounce 100))
            data-click-ch (maps/data-listen data
                                            "click"
                                            (om/get-state owner :data-click-ch))
            kill-ch (om/get-state owner :kill-ch)]
        (om/set-state! owner :data-click-mult (async/mult data-click-ch))
        (om/set-state! owner :gmap google-map)
        (.setStyle data #js {:icon "http://mt.google.com/vt/icon?psize=25&font=fonts/Roboto-Bold.ttf&color=ff135C13&name=icons/spotlight/spotlight-waypoint-a.png&ax=44&ay=50&text=%E2%80%A2"})
        (go-loop []
          (let [[v ch] (alts! [kill-ch bounds-changed-ch center-changed-ch idle-ch])]
            (if (= ch kill-ch)
              ::done
              (do
                (condp = ch
                  idle-ch           (raise! owner [:maps-bounds-changed (.. google-map (getBounds))])
                  bounds-changed-ch (om/set-state! owner :bounds (.. google-map (getBounds)))
                  center-changed-ch (let [center (.getCenter google-map)
                                          lat (.lat center)
                                          lng (.lng center)]
                                      (raise! owner [:maps-center-changed {:lat lat :lng lng}])))
                (recur)))))))
    om/IWillUnmount
    (will-unmount [_]
      (when-let [kill-ch (:kill-ch (om/get-state owner))]
        (put! kill-ch (js/Date.))))
    om/IDidUpdate
    (did-update [_ prev-props _]
      (let [prev-options (-> prev-props :ui :map-options)
            options (:map-options ui)
            update-render? (:update-render? ui)
            {:keys [gmap]} (om/get-state owner)]
        (when (and update-render?
                   (not= prev-options options))
          (raise! owner [:maps-updated-render])
          (.setOptions gmap (clj->js options)))))
    om/IRenderState
    (render-state [_ {:keys [gmap info-window data-click-mult] :as state}]
      (let [stops (:stops-search-results data)]
        (html
         (->> [:div.maps-viewport
               #_(om/build maps-canvas-dev (:stops data) {:state {:gmap gmap}})
               [:div.maps-canvas {:ref "gmap"}]]
              (#(if gmap
                  (into
                   %
                   (om/build-all search-stop-marker stops {:key :stop/name
                                                           :state {:gmap gmap
                                                                   :info-window info-window
                                                                   :data-click-mult data-click-mult}}))
                  %))
              (#(if gmap
                  (into
                   %
                   (om/build-all stop-marker
                                 (:stops data)
                                 {:key :stop/id
                                  :state {:gmap gmap}}))
                  %))
              ))))))
