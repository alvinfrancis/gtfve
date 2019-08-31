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

(defn side-panel-tab [{:keys [key label tab badge]} owner]
  (reify
    om/IDisplayName (display-name [_] "Side Panel Tab")
    om/IRender
    (render [_]
      (html [:li {:class (when (= tab key) "active")}
             [:a {:href "#"
                  :onClick (fn [e]
                             (raise! owner [:side-panel-changed {:key key}])
                             (.preventDefault e))} label
              (when (not (zero? badge))
                [:span " " [:span.badge badge]])]]))))

(defn ^:private tab-class [active? in?]
  (join " "
        ["tab-pane" "fade" "side"
         (when active? "active")
         (when in? "in")]))

(defn stop-search-result [data owner]
  (reify
    om/IDisplayName (display-name [_] "Stop Search Result")
    om/IRender
    (render [_]
      (html
       [:a.list-group-item
        {:href "#"
         :on-click #(do
                      (raise! owner [:stops-search-result-clicked {:stop data}])
                      (.preventDefault %))}
        [:h5.list-group-item-heading (:stop/name data)]
        [:p.list-group-item-text (str (:stop/latitude data)
                                      "/"
                                      (:stop/longitude data))]]))))

(defn stop-search-results [{:keys [data query loading?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Stop Search Results")
    om/IRender
    (render [_]
      (html
       (if loading?
         [:div
          [:p.label.label-primary "Loading ..."]
          [:div.progress.progress-striped.active
           [:div.progress-bar {:style {:width "100%"}}]]]
         (if (empty? query)
           nil
           (if (empty? data)
             [:div.alert.alert-danger
              [:p "No stops found matching "
               [:i query]]]
             (into [:div.list-group]
                   (om/build-all stop-search-result data {:key :db/id})))))
       #_(cond ;; cond causes react-key warnings
           loading? [:div
                     [:p.label.label-primary "Loading ..."]
                     [:div.progress.progress-striped.active
                      [:div.progress-bar {:style {:width "100%"}}]]]
           (empty? query) nil
           (empty? data) [:div.alert.alert-danger
                          [:p "No stops found matching "
                           [:i query]]]
           :else (into [:div.list-group]
                       (om/build-all stop-search-result data {:key :db/id})))))))

(defn stop-panel [{:keys [tab ui data active? in?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Stop Panel")
    om/IRender
    (render [_]
      (let [{:keys [query last-query loading?]} ui]
        (html
         [:div {:className (tab-class active? in?)}
          [:div.tab-content-wrapper
           [:form.form-horizontal
            {:on-submit #(do
                           (when (not loading?)
                             (raise! owner [:stops-search-submitted {:query query}]))
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
           (om/build stop-search-results {:data data
                                          :query last-query
                                          :loading? loading?})
           [:button.btn.btn-default.btn-block {:href "#"} "Load More"]]])))))

(defn route-panel [{:keys [tab data active? in?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Routes Panel")
    om/IRender
    (render [_]
      (html
       [:div {:className (tab-class active? in?)}
        [:p "Routes Panel"]]))))

(defn trip-panel [{:keys [tab data active? in?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Trips Panel")
    om/IRender
    (render [_]
      (html
       [:div {:className (tab-class active? in?)}
        [:p "Trips Panel"]]))))

(defn changes-panel [{:keys [tab data active? in?]} owner]
  (reify
    om/IDisplayName (display-name [_] "Changes Panel")
    om/IRender
    (render [_]
      (html
       [:div {:className (tab-class active? in?)}
        [:div.tab-content-wrapper
         [:div.row {:style {:padding-bottom "10px"}}
          [:div.col-sm-6
           [:a.btn.btn-danger.btn-block
            {:href "#"
             :on-click (fn [e]
                         (raise! owner [:changes-cleared])
                         (.preventDefault e))}
            "Clear"]]
          [:div.col-sm-6
           [:a.btn.btn-primary.btn-block
            {:href "#"
             :on-click (fn [e]
                         (raise! owner [:changes-commited])
                         (.preventDefault e))}
            "Commit"]]]
         [:div
          (map-indexed
           (fn [i [v eid changes]]
             [:div.panel.panel-default
              [:div.panel-heading eid]
              [:div.panel-body
               [:table.table {:style {:font-size "small"}}
                (into [:tbody]
                      (map (fn [[a v]]
                             [:tr
                              [:td (pr-str a)]
                              [:td v]]))
                      changes)]
               [:div.btn-toolbar {:style {:float "right"}}
                [:button.btn.btn-danger.btn-xs
                 {:on-click (fn [e]
                              (raise! owner [:change-retracted i])
                              (.preventDefault e))}
                 [:i.material-icons "delete"]]
                [:button.btn.btn-default.btn-xs [:i.material-icons "search"]]
                [:button.btn.btn-primary.btn-xs [:i.material-icons "done"]]]
               ]])
           data)]]]))))

(defn side-panel [{:keys [ui data]} owner]
  (reify
    om/IDisplayName (display-name [_] "Side Panel")
    om/IInitState
    (init-state [_]
      (let [tab (:tab ui)]
        {:active tab
         :in tab
         :toggle-ch (chan 1)
         :kill-ch (chan 1)}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [toggle-ch kill-ch]} (om/get-state owner)
            class-filter-fn   (fn [c] #(.. % -target -classList (contains c)))
            tab-pane-filter   (filter (class-filter-fn "tab-pane"))
            fade-filter       (filter (class-filter-fn "fade"))
            not-active-filter (filter (complement (class-filter-fn "active")))
            opacity-filter    (filter #(= "opacity" (.. % -event_ -propertyName)))
            mutations-ch      (utils/mutation-listen (om/get-node owner)
                                                     #js {:subtree true
                                                          :attributes true
                                                          :attributeFilter #js ["class"]}
                                                     (chan 1 (comp
                                                              tab-pane-filter
                                                              not-active-filter)))
            transition-ch     (utils/listen (om/get-node owner)
                                            EventType.TRANSITIONEND
                                            (chan 1 (comp
                                                     opacity-filter
                                                     fade-filter
                                                     tab-pane-filter)))]
        (go-loop []
          (let [[v c] (alts! [toggle-ch kill-ch])]
            (if (= c kill-ch)
              ::done
              (let [{:keys [active in]} (om/get-state owner)]
                (om/set-state! owner :in nil)
                (<! transition-ch)
                (om/set-state! owner :active v)
                (<! mutations-ch)
                (om/set-state! owner :in v)
                (recur)))))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [kill-ch]} (om/get-state owner)]
        (put! kill-ch (js/Date.))))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [transition? toggle-ch active in]} (om/get-state owner)
            tab (:tab ui)]
        (when (and (not (nil? in))
                   (not= in tab))
          (put! toggle-ch (:tab ui)))))
    om/IRenderState
    (render-state [_ {:keys [active in]}]
      (let [tab (:tab ui)]
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
                                          :tab tab})
                (om/build side-panel-tab {:key :changes
                                          :label "Changes"
                                          :tab tab
                                          :badge (count (:changes data))})]
               [:div.tab-content {:ref "tab-content"}
                ;; stops
                (om/build stop-panel {:tab tab
                                      :ui (:stops ui)
                                      :data (:stops-search-results data)
                                      :active? (= active :stops) :in? (= in :stops)})
                ;; routes
                (om/build route-panel {:tab tab
                                       :data (:routes-search-results data)
                                       :active? (= active :routes) :in? (= in :routes)})
                ;; trips
                (om/build trip-panel {:tab tab
                                      :data (:trips-search-results data)
                                      :active? (= active :trips) :in? (= in :trips)})

                ;; changes
                (om/build changes-panel {:tab tab
                                         :data (:changes data)
                                         :active? (= active :changes)
                                         :in? (= in :changes)})]])))))
