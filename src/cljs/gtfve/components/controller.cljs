(ns gtfve.components.controller
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [om.core :as om]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer [put! chan <! close!]]
            [gtfve.controllers.controls :as controls-con]))

(defn- control-handler [v app cursors]
  (controls-con/control-event (first v) (second v) app cursors))

(defn controller [app owner]
  (reify
    om/IDisplayName (display-name [_] "Controller")
    om/IInitState
    (init-state [_]
      {:kill-ch (chan)})
    om/IWillMount
    (will-mount [_]
      (let [kill-ch (om/get-state owner :kill-ch)
            comms (om/get-shared owner [:comms])
            cursors (om/get-shared owner [:cursors])
            controls-mult (:controls-mult comms)
            controls-tap (chan)]
        (async/tap controls-mult controls-tap)
        (om/set-state! owner :controls-tap controls-tap)
        (go-loop []
          (let [[v c] (alts! [kill-ch controls-tap])]
            (if (= c kill-ch)
              :done
              (do
                (condp = c
                  controls-tap (control-handler v app cursors))
                (recur))))
          (async/untap controls-mult controls-tap))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [kill-ch controls-tap]} (om/get-state owner)]
        (when kill-ch (put! kill-ch (js/Date.)))))
    om/IRender
    (render [_]
      (html [:noscript]))))
