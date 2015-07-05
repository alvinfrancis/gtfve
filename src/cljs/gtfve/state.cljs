(ns gtfve.state
  (:require [om.core :as om]))

(defn initial-state []
  {:error-message nil
   :environment :development
   :ui {:editor {:mode :view}
        :panel {:tab :stops
                :active? true
                :in? true}}
   :data {:stops []
          :stop-times []
          :routes []}})

(defn create-cursors [state]
  (let [root (om/root-cursor state)]
    {:data   #(om/ref-cursor (:data root))
     :ui     #(om/ref-cursor (:ui root))
     :panel  #(om/ref-cursor (get-in root [:ui :panel]))
     :editor #(om/ref-cursor (get-in root [:ui :editor]))}))
