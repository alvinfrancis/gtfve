(ns gtfve.data.queries
  (:require [datomic.api :as d]
            [gtfve.data.connection :refer [conn uri feed]]))

(defn touch-all
  "Touches entity e and all other reachable Entities of e"
  [e]
  (cond
    (instance? datomic.query.EntityMap e)
    (reduce (fn [m [k v]]
              (assoc m k (touch-all v)))
            {}
            (d/touch e))
    (set? e) (into #{} (map touch-all e))
    :else e))

(defn trips []
  (let [db (d/db conn)]
    (->> (d/datoms db :aevt :trip/id)
         (map :e)
         (map (partial d/entity db))
         (map d/touch))))

(defn routes
  ([]
   (let [db (d/db conn)]
     (->> (d/datoms db :aevt :route/id)
          (map :e)
          (map (partial d/entity db))
          (map d/touch))))
  ([id]
   (let [db (d/db conn)]
     (->> (d/datoms db :avet :route/id id)
          first
          :e
          (d/entity db)
          touch-all))))

(defn haversine
  [{lon1 :longitude lat1 :latitude} {lon2 :longitude lat2 :latitude}]
  (let [R 6372.8
        dlat (Math/toRadians (- lat2 lat1))
        dlon (Math/toRadians (- lon2 lon1))
        lat1 (Math/toRadians lat1)
        lat2 (Math/toRadians lat2)
        a (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2))) (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2)) (Math/cos lat1) (Math/cos lat2)))]
    (* R 2 (Math/asin (Math/sqrt a)))))
