(ns gtfve.data.validation
  (:require [clojure.string :as string :refer [split join trim]]
            [clojure.pprint :as pp :refer [pprint]]
            [clj-time.format :as tf]
            [clj-time.coerce :refer [to-date]]
            [clojure.instant :as instant :refer [read-instant-date]]
            [datomic.api :as d]
            [clojure.set :as set :refer [union]]
            [gtfve.data.connection :refer [conn uri feed]]))

(defn- collect-consecutive-stops
  "Return stops from a trip that are consecutively reused."
  [trip]
  (let [stops (->> (:trip/stops trip)
                   (sort-by :stop-time/stop-sequence))]
    (->> (reduce (fn [{:keys [last] :as acc} stop-time]
                   (let [{last-stop :stop-time/stop} last
                         {curr-stop :stop-time/stop} stop-time]
                     (if (= curr-stop last-stop)
                       (-> acc
                           (update-in [:coll] union #{last stop-time})
                           (assoc :last stop-time))
                       (assoc acc :last stop-time))))
                 {:coll #{}
                  :last nil}
                 stops)
         :coll)))

(defn find-consecutive-stops
  "Return all trip stops that are consecutively reused."
  [db]
  (->> (d/q '[:find [?t ...]
              :where [?t :trip/id]]
            db)
       (map (partial d/entity db))
       (reduce (fn [acc c]
                 (->> (collect-consecutive-stops c)
                      (sort-by :stop-time/stop-sequence)
                      (into acc)))
               [])))
