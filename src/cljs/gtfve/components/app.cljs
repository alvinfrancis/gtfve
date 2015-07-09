(ns gtfve.components.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [gtfve.components.header :as header]
            [gtfve.components.side-panel :as side-panel]
            [gtfve.components.maps-panel :as maps-panel]
            [gtfve.components.controller :as controller])
  (:import goog.History))

(defn app* [state owner]
  (reify
    om/IDisplayName (display-name [_] "App")
    om/IRender
    (render [_]
      (let [{:keys [data ui]
             {:keys [panel editor]} :ui} state]
        (html
         [:div
          (om/build controller/controller state)
          (om/build header/header nil)
          [:div.container-fluid
           [:div.row
            [:div.col-sm-3.side-panel-container
             (om/build side-panel/side-panel {:panel panel :data data})]
            [:div.col-sm-9.main-panel-container
             (om/build maps-panel/maps-panel editor)]]]])))))

(defn app [state owner opts]
  (reify
    om/IDisplayName (display-name [_] "App Wrapper")
    om/IRender (render [_] (om/build app* state opts))))
