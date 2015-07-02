(ns gtfve.state
  (:require [om.core :as om]))

(def ^:dynamic *state* nil)

(defn initial-state []
  {:error-message nil
   :environment :development
   :editor {:mode :view}
   :panel {:tab :stops}
   :data {:stops []
          :stop-times []
          :routes []}})

(defn panel-cursor []
  (om/ref-cursor (:panel (om/root-cursor *state*))))

(defn editor-cursor []
  (om/ref-cursor (:editor (om/root-cursor *state*))))

(defn data-cursor []
  (om/ref-cursor (:data (om/root-cursor *state*))))
