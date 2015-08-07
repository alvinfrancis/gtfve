(ns gtfve.demand
  (:require [liberator.core :refer [defresource]]
            [gtfve.data.queries :as q]
            [clojure.core.match :refer [match]]
            [clojure.edn :as edn]))

(defmulti route (fn [[root pull]] (first root)))

(defmethod route :default [[root pull]]
  (throw
   (ex-info "Cannot match root"
            {:root root
             :pull pull})))

(defmethod route :app/stops-search [[root pull]]
  (let [[_ q] root]
    {root (q/stops-search q pull)}))

(defn pull-data [query]
  (mapv route query))

(defresource data [query]
  :available-media-types ["application/json"
                          "application/edn"]
  :allowed-methods [:get]
  :handle-ok (fn [_] (pull-data query)))
