(ns gtfve.data.queries
  (:require [datomic.api :as d]
            [gtfve.data.connection :refer [conn uri feed]]))

(defn bounded?
  ([[[x1 y1] [x2 y2]]]
   (fn [[px py]]
     (and (> px x1) (> py y1)
          (< px x2) (< py y2))))
  ([[[x1 y1] [x2 y2]] [px py]]
   (and (> px x1) (> py y1)
        (< px x2) (< py y2))))

(defn pull
  "Use the Datomic Pull API to query the database."
  [spec e]
  (let [db (d/db conn)]
    (d/pull db spec e)))

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

(defn bounded-stops
  [bbox pull]
  (let [db (d/db conn)
        xf (comp
            (map :e)
            (map (partial d/entity db))
            (filter (fn [e]
                      (let [point ((juxt :stop/latitude :stop/longitude) e)]
                        (bounded? bbox point))))
            (map :db/id)
            (map (partial d/pull db pull)))]
    (transduce xf conj (d/datoms db :aevt :stop/id))))

(defn viewport
  [bbox & {:keys [stops-pull]
           :or {stops-pull [:stop/id :stop/latitude :stop/longitude]}}]
  (bounded-stops bbox stops-pull))

(defn stops-search
  ([name]
   (stops-search name [:stop/id :stop/name]))
  ([name p]
   (let [db (d/db conn)
         pattern (re-pattern (str "(?i)" name))
         match? #(not (nil? (re-find pattern %)))]
     (d/q '[:find [(pull ?s p) ...]
            :in $ ?pattern p
            :where
            [?s :stop/name ?name]
            [(re-find ?pattern ?name)]]
          db pattern p))))

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
          (map #(select-keys % [:route/id
                                :route/short-name
                                :route/long-name
                                :route/description
                                :route/text-color])))))
  ([id]
   (let [db (d/db conn)]
     (->> (d/datoms db :avet :route/id id)
          first
          :e
          (d/entity db)
          (d/touch)))))

(defn haversine
  [{lon1 :longitude lat1 :latitude} {lon2 :longitude lat2 :latitude}]
  (let [R 6372.8
        dlat (Math/toRadians (- lat2 lat1))
        dlon (Math/toRadians (- lon2 lon1))
        lat1 (Math/toRadians lat1)
        lat2 (Math/toRadians lat2)
        a (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2))) (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2)) (Math/cos lat1) (Math/cos lat2)))]
    (* R 2 (Math/asin (Math/sqrt a)))))
