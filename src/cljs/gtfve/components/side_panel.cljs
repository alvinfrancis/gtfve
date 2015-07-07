(ns gtfve.components.side-panel
  (:require [clojure.string :refer [join]]
            [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [gtfve.async :refer [raise!]]
            [gtfve.state :as state]
            [gtfve.utils :as utils]))

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

(defn ^:private tab-class [tab key]
  (join " "
        ["tab-pane" "fade" "side"
         (when (= tab key)
           "active in")]))

(defn stop-panel [{:keys [panel data]} owner]
  (reify
    om/IDisplayName (display-name [_] "Side Panel")
    om/IRender
    (render [_]
      (let [tab (:tab panel)
            query (:stops-query panel)]
        (html
         [:div {:className (tab-class tab :stops)}
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
          ])))))

(defn route-panel [{:keys [panel data]} owner]
  (reify
    om/IDisplayName (display-name [_] "Routes Panel")
    om/IRender
    (render [_]
      (let [tab (:tab panel)]
        (html
         [:div {:className (tab-class tab :routes)}
          [:p "Routes Panel"]])))))

(defn trip-panel [{:keys [panel data]} owner]
  (reify
    om/IDisplayName (display-name [_] "Trips Panel")
    om/IRender
    (render [_]
      (let [tab (:tab panel)]
        (html
         [:div {:className (tab-class tab :trips)}
          [:p "Trips Panel"]])))))

(defn side-panel [{:keys [panel data]} owner]
  (reify
    om/IDisplayName (display-name [_] "Side Panel")
    om/IRenderState
    (render-state [_ {:keys [active? in?]}]
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
               [:div.tab-content
                ;; stops
                (om/build stop-panel {:panel panel :data (:stops data)})
                ;; routes
                (om/build route-panel {:panel panel :data (:routes data)})
                ;; trips
                (om/build trip-panel {:panel panel :data (:trips data)})]])))))
