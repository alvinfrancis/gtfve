(ns gtfve.components.maps-canvas.google
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [clojure.data :as d]
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
    om/IInitState
    (init-state [_]
      {:kill-ch (chan)})
    om/IWillMount
    (will-mount [_]
      (let [gmap (om/get-state owner :gmap)
            marker (maps/marker [(:stop/latitude data)
                                 (:stop/longitude data)]
                                :draggable true)]
        (om/set-state! owner :marker marker)))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [kill-ch marker gmap]} (om/get-state owner)
            position-changed-ch (-> (maps/listen marker "position_changed")
                                    (utils/debounce 1000))]
        (.setMap marker gmap)
        (go-loop []
          (let [[v ch] (alts! [position-changed-ch kill-ch])]
            (if (= ch kill-ch)
              ::done
              (let [[lat lng] (.getPosition marker)]
                (raise! owner [:change-added [:stop (:db/id data)
                                              {:stop/latitude lat
                                               :stop/longitude lng}]])
                (recur)))))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [kill-ch marker]} (om/get-state owner)]
        (.setMap marker nil)
        (put! kill-ch (js/Date.))))
    om/IRender
    (render [_]
      (html [:noscript (pr-str data)]))))

(defn ^:private stop-marker-fn [owner gmap kill-ch kill-mult]
  (fn [[eid stop]]
    (let [marker (maps/marker [(:stop/latitude stop)
                               (:stop/longitude stop)]
                              :draggable true
                              :map gmap)
          kill-tap (let [ch (chan)]
                     (async/tap kill-mult ch)
                     ch)
          position-changed-ch (-> (maps/listen marker "position_changed")
                                  (utils/debounce 100))]
      (go-loop []
        (let [[v ch] (alts! [position-changed-ch kill-tap])]
          (if (= ch kill-tap)
            ::done
            (let [[lat lng] (.getPosition marker)]
              (raise! owner [:change-added [:stop eid
                                            {:stop/latitude lat
                                             :stop/longitude lng}]])
              (recur))))
        (async/untap kill-mult kill-tap))
      [eid marker])))

(defn stops-layer
  "Takes data as a list of stops and renders it to gmap in state."
  [data owner]
  (reify
    om/IDisplayName (display-name [_] "Stops Layer")
    om/IInitState
    (init-state [_]
      (let [kill-ch (chan)]
        {:kill-ch kill-ch
         :kill-mult (async/mult kill-ch)}))
    om/IWillMount
    (will-mount [_]
      (let [{:keys [kill-ch kill-mult gmap]} (om/get-state owner)
            marker-fn (stop-marker-fn owner gmap kill-ch kill-mult)
            markers (into {} (map marker-fn) data)]
        (om/set-state! owner :markers markers)))
    om/IDidUpdate
    (did-update [_ prev-data _]
      (let [{:keys [kill-ch kill-mult markers gmap]} (om/get-state owner)
            marker-fn (stop-marker-fn owner gmap kill-ch kill-mult)
            [old new keep] (d/diff prev-data data)
            new-markers (into markers (map marker-fn) new)
            new-markers (reduce (fn [m [eid marker]]
                                  (let [old-marker (get m eid)]
                                    (if old-marker
                                      (do
                                        (.setMap old-marker nil)
                                        (dissoc m eid))
                                      m)))
                                new-markers
                                old)]
        (om/set-state! owner :markers new-markers)))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [kill-ch markers]} (om/get-state owner)]
        (doseq [[id marker] markers]
          (.setMap marker nil))
        (when kill-ch (put! kill-ch (js/Date.)))))
    om/IRender
    (render [_]
      (html [:noscript]))))

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
            kill-ch (om/get-state owner :kill-ch)
            second-xf (map second)
            bounded? (fn [gmap [k1 k2]]
                       (fn [{px k1 py k2}]
                         (let [[[x1 y1] [x2 y2]] (.getBounds gmap)
                               bbox-width (- x2 x1)
                               bbox-height (- y2 y1)
                               expanded-bbox [[(- x1 bbox-width)
                                               (- y1 bbox-height)]
                                              [(+ x2 bbox-width)
                                               (+ y2 bbox-height)]]
                               [[x1 y1] [x2 y2]] expanded-bbox]
                           (and (> px x1) (> py y1)
                                (< px x2) (< py y2)))))
            bounded-partial (partial bounded? google-map)
            filter-stops-xf (filter (bounded-partial [:stop/latitude :stop/longitude]))]
        (om/set-state! owner :stops-xf (comp second-xf filter-stops-xf))
        (om/set-state! owner :data-click-mult (async/mult data-click-ch))
        (om/set-state! owner :gmap google-map)
        (.setStyle data #js {:icon "http://mt.google.com/vt/icon?psize=25&font=fonts/Roboto-Bold.ttf&color=ff135C13&name=icons/spotlight/spotlight-waypoint-a.png&ax=44&ay=50&text=%E2%80%A2"})
        (go-loop []
          (let [[v ch] (alts! [kill-ch bounds-changed-ch center-changed-ch idle-ch])]
            (if (= ch kill-ch)
              ::done
              (do
                (condp = ch
                  idle-ch           (raise! owner [:maps-bounds-changed {:bbox (.. google-map (getBounds))}])
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
    (render-state [_ {:keys [gmap info-window data-click-mult stops-xf] :as state}]
      (let [stops (:stops-search-results data)
            modes (->> ui :modes (filter second) (map first))]
        (html
         [:div.maps-viewport
          #_(om/build maps-canvas-dev (:stops data) {:state {:gmap gmap}})
          [:div.maps-canvas {:ref "gmap"}]
          (when (and gmap
                     (some #{:stops?} modes)
                     (<= 16 (.getZoom gmap)))
            (om/build stops-layer (select-keys (:entities data)
                                               (:stops data))
                      {:state {:gmap gmap}}))
          (when gmap
            (om/build-all search-stop-marker stops {:key :stop/name
                                                    :state {:gmap gmap
                                                            :info-window info-window
                                                            :data-click-mult data-click-mult}}))])))))
