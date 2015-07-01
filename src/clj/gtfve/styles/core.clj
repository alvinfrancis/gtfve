(ns gtfve.styles.core
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px percent]]
            [garden.color :as color :refer [rgba rgb hsl]]))

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
                  :padding (px 20)
                  :height (percent 100)}]
   [:.maps-toolbar {:background-color "#1976d2"
                    :min-height "48px"}]
   [:.maps-panel {:height (percent 100)
                  :background-color "#e3f2f5"}]])
