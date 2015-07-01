(ns gtfve.styles.core
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px percent]]))

(defstyles main
  [["@font-face" {:font-family "Roboto"
                  :src "url(\"../font/Roboto/Roboto-Regular.ttf\")"}]
   [:body {:padding-top (px 64)}]
   [:.side-panel-container {:padding-left 0
                            :padding-right 0}]
   [:.main-panel-container {:padding-left 0
                            :padding-right 0}]
   [:.side-panel {:background-color "#e3f2fd"
                  :padding (px 20)
                  :height (percent 100)}]])
