(ns gtfve.demand
  (:require [liberator.core :refer [defresource]]
            [gtfve.data.queries :as q]
            [clojure.core.match :refer [match]]
            [clojure.edn :as edn]))

(defn pull-data [query args]
  (match [query]
         [{:app/stops-search pull}] (q/stops-search (:q args) pull)
         :else (throw
                (ex-info "Cannot match query"
                         {:query query
                          :args args}))))

(defresource data [query args]
  :available-media-types ["text/html"
                          "application/edn"
                          "application/json"]
  :allowed-methods [:get]
  :handle-ok (fn [_] (pull-data query args)))
