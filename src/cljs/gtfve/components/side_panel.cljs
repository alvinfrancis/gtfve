(ns gtfve.components.side-panel
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.string :refer [join]]
            [cljs.core.async :as async :refer [put! chan <! close! >!]]
            [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [gtfve.async :refer [raise!]]
            [gtfve.state :as state]
            [gtfve.utils :as utils])
  (:import [goog.events EventType]))

(defn side-panel-tab [{:keys [key label tab]} owner]
  (reify
    om/IDisplayName (display-name [_] "Side Panel Tab")
    om/IRender
    (render [_]
      (html [:li {:class (when (= tab key) "active")}
             [:a {:href "#"
                  :onClick (fn [e]
                             (raise! owner [:side-panel-changed {:key key}])
                             (.preventDefault e))} label]]))))

(defn ^:private tab-class [active? in?]
  (join " "
        ["tab-pane" "fade" "side"
         (when active? "active")
         (when in? "in")]))

(defn stop-panel [{:keys [panel data active? in?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Stop Panel")
    om/IRender
    (render [_]
      (let [tab (:tab panel)
            query (:stops-query panel)]
        (html
         [:div {:className (tab-class active? in?)}
          [:div.tab-content-wrapper
           [:form.form-horizontal
            {:on-submit #(do
                           (raise! owner [:stops-search-submitted {:query query}])
                           (.preventDefault %))}
            [:fieldset
             [:div.form-group
              [:label.col-sm-2.control-label {:for "inputSearch"} "Stop"]
              [:div.col-sm-10
               [:input.form-control#inputSearch
                {:type "text"
                 :placeholder "Search"
                 :value query
                 :on-change #(utils/edit-input owner :input-stops-search %)}]]]]]
           (into [:div.list-group]
                 (map (fn [stop]
                        [:a.list-group-item {:key (:db/id stop)
                                             :href "#"}
                         [:h5.list-group-item-heading (:stop/name stop)]
                         [:p.list-group-item-text (str (:stop/latitude stop)
                                                       "/"
                                                       (:stop/longitude stop))]])
                      data))
           [:button.btn.btn-default.btn-block {:href "#"} "Load More"]]])))))

(defn route-panel [{:keys [panel data active? in?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Routes Panel")
    om/IRender
    (render [_]
      (let [tab (:tab panel)]
        (html
         [:div {:className (tab-class active? in?)}
          [:p "Routes Panel"]])))))

(defn trip-panel [{:keys [panel data active? in?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Trips Panel")
    om/IRender
    (render [_]
      (let [tab (:tab panel)]
        (html
         [:div {:className (tab-class active? in?)}
          [:p "Trips Panel"]])))))

(defn side-panel [{:keys [panel data]} owner]
  (reify
    om/IDisplayName (display-name [_] "Side Panel")
    om/IInitState
    (init-state [_]
      (let [tab (:tab panel)]
        {:active tab
         :in tab
         :toggle-ch (chan 1)
         :kill-ch (chan 1)}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [toggle-ch kill-ch]} (om/get-state owner)
            transition-ch (utils/listen
                           (om/get-node owner)
                           EventType.TRANSITIONEND
                           (chan 1 (comp
                                    (filter #(and (= "opacity" (.. % -event_ -propertyName))
                                                  (let [classList (.. % -target -classList)]
                                                    (and
                                                     (.contains classList "tab-pane")
                                                     (.contains classList "fade")))))
                               )))
            active-in-ch (chan 1)]
        (go-loop []
          (let [[v c] (alts! [active-in-ch toggle-ch kill-ch])]
            (if (= c kill-ch)
              ::done
              (do
                (condp = c
                  toggle-ch (let [{:keys [active in]} (om/get-state owner)]
                              (om/set-state! owner :in nil)
                              (<! transition-ch)
                              (om/set-state! owner :active v)
                              (>! active-in-ch v)
                              )
                  active-in-ch (om/set-state! owner :in v))
                (recur)))))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [kill-ch]} (om/get-state owner)]
        (put! kill-ch (js/Date.))))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [transition? toggle-ch active in]} (om/get-state owner)
            tab (:tab panel)]
        (when (and (not (nil? in))
                   (not= in tab))
          (put! toggle-ch (:tab panel)))))
    om/IRenderState
    (render-state [_ {:keys [active in]}]
      (let [tab (:tab panel)]
        (html [:div.side-panel
               [:ul.nav.nav-tabs
                (om/build side-panel-tab {:key :stops
                                          :label "Stops"
                                          :tab tab})
                (om/build side-panel-tab {:key :routes
                                          :label "Routes"
                                          :tab tab})
                (om/build side-panel-tab {:key :trips
                                          :label "Trips"
                                          :tab tab})]
               [:div.tab-content {:ref "tab-content"}
                ;; stops
                (om/build stop-panel {:panel panel :data (:stops data)
                                      :active? (= active :stops) :in? (= in :stops)})
                ;; routes
                (om/build route-panel {:panel panel :data (:routes data)
                                       :active? (= active :routes) :in? (= in :routes)})
                ;; trips
                (om/build trip-panel {:panel panel :data (:trips data)
                                      :active? (= active :trips) :in? (= in :trips)})]])))))
