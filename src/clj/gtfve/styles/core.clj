(ns gtfve.styles.core
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px percent]]
            [garden.color :as color :refer [rgba rgb hsl]]
            [garden.stylesheet :refer [at-media]]
            [garden.arithmetic :as a]))

(defstyles main
  [["@font-face" {:font-family "Roboto"
                  :src "url(\"../font/Roboto/Roboto-Regular.ttf\")"}]
   [:body {:padding-top (px 64)}]
   [:.side-panel-container {:padding-left 0
                            :padding-right 0
                            :border :none
                            :z-index 1020
                            :box-shadow [[0 (px 1) (px 2) (rgba 0 0 0 0.3)]]}]
   [:.main-panel-container {:padding-left 0
                            :padding-right 0}]
   [:.side-panel {:background-color "#e3f2fd"
                  :height (percent 100)}]
   (at-media {:max-width (px 768)}
             [:.side-panel-container {:display :none}])
   [:.maps-toolbar {:background-color "#1976d2"
                    :height (px 36)}
    [:+
     [:.maps-canvas {:height "calc(100% - 36px)"}]]]
   [:.maps-panel {:height (percent 100)
                  :background-color "#e3f2f5"}]
   [:.maps-button {:background-color "#1976d2"
                   :border-radius 0
                   :border :none
                   :box-shadow :none}
    [:&.active {:background-color "#0d47a1"
                :border "1px"}]]
   [:.nav.nav-tabs
    [:+
     [:.tab-content {:height "calc(100% - 43px)"}]]]
   [:.tab-content {:overflow :auto}
    [:.tab-content-wrapper {:padding (px 20)}]]])
