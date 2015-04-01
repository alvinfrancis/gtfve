(ns gtfve.data.load
  (:require [clojure.string :as string :refer [split join trim]]
            [clojure.pprint :as pp :refer [pprint]]
            [clj-time.format :as tf]
            [clj-time.coerce :refer [to-date]]
            [clojure.instant :as instant :refer [read-instant-date]]
            [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [gtfve.data.connection :refer [conn uri feed]]))

;; Utilities ;;
;;;;;;;;;;;;;;;

(defn load-schema! []
  (d/transact conn (read-string (slurp "resources/gtfs-schema.edn"))))

(defn- drop-empty [hmap]
  (->> hmap
       (remove (fn [[_ v]]
                 (try
                   (empty? v)
                   (catch IllegalArgumentException e
                     false))))
       (into {})))

(defn- assoc-temp-id [m]
  (assoc m :db/id (d/tempid :db.part/user)))

(defn csv->maps
  "Create a sequence of maps from csv found in path. header-map is a map of
  strings to keywords defining column to keyword mappings for the generated
  transaction map. transform-vals is a map of keywords to functions defining
  function transformations on values in the generated transaction map.

  a,b,c  -> {:a 1 :b 2 :c 3}
  1,2,3"
  [header-map val-transforms path]
  (let [[headers & body] (csv/parse-csv (slurp path))
        header-keys (map #(header-map %) headers)]
    (letfn [(zipheaders [header]
              (zipmap header-keys header))
            (transform-vals [row]
              (reduce (fn [acc [k v]]
                        (if-let [f (val-transforms k)]
                          (assoc acc k (f v))
                          (assoc acc k v)))
                      {} row))]
      (->> body
           (map zipheaders)
           (map transform-vals)
           (map drop-empty)))))

;; Agency ;;
;;;;;;;;;;;;

(def agency-header-keys
  {"agency_id" :agency/id
   "agency_name" :agency/name
   "agency_url" :agency/url
   "agency_timezone" :agency/timezone
   "agency_lang" :agency/lang
   "agency_phone" :agency/phone})

(defn load-agency! [path conn]
  (->> path
       (csv->maps agency-header-keys {})
       (map assoc-temp-id)
       (d/transact conn)))

;; Routes ;;
;;;;;;;;;;;;

(def routes-header-keys
  {"agency_id" :route/agency
   "route_short_name" :route/short-name
   "route_long_name" :route/long-name
   "route_desc" :route/description
   "route_type" :route/type
   "route_url" :route/url
   "route_color" :route/color
   "route_text_color" :route/text-color
   "route_id" :route/id})

(def routes-val-transforms
  {:route/agency #(vector :agency/id %)})

(defn load-routes! [path conn]
  (->> path
       (csv->maps routes-header-keys routes-val-transforms)
       (map assoc-temp-id)
       (d/transact conn)))

;; Stops ;;
;;;;;;;;;;;

(def stops-header-keys
  {"stop_id" :stop/id
   "stop_code" :stop/code
   "stop_name" :stop/name
   "stop_desc" :stop/description
   "stop_lat" :stop/latitude
   "stop_lon" :stop/longitude
   "zone_id" :stop/zone-id ; TODO: not yet implemented fare_rules for this ref
   "stop_url" :stop/url
   "location_type" :stop/location-type
   "parent_station" :stop/parent-station
   "wheelchair_boarding" :stop/wheelchair-boarding})

(def stops-val-transforms
  {:stop/latitude (comp double read-string)
   :stop/longitude (comp double read-string)
   :stop/wheelchair-boarding read-string
   :stop/location-type read-string})

(defn load-stops! [path conn]
  (->> path
       (csv->maps stops-header-keys stops-val-transforms)
       (map assoc-temp-id)
       (d/transact conn)))

;; Calendar ;;
;;;;;;;;;;;;;;

(def calendar-days
  [:calendar.day/monday :calendar.day/tuesday
   :calendar.day/wednesday :calendar.day/thursday
   :calendar.day/friday :calendar.day/saturday
   :calendar.day/sunday])

(def calendar-header-keys
  {"service_id" :service/id
   "monday" :calendar.day/monday
   "tuesday" :calendar.day/tuesday
   "wednesday" :calendar.day/wednesday
   "thursday" :calendar.day/thursday
   "friday" :calendar.day/friday
   "saturday" :calendar.day/saturday
   "sunday" :calendar.day/sunday
   "start_date" :service/start-date
   "end_date" :service/end-date})

(def ^:dynamic *service-date-formatter* (tf/formatter "yyyyMMdd"))

(def calendar-val-transforms
  (merge
   {:service/start-date #(to-date (tf/parse *service-date-formatter* %))
    :service/end-date #(to-date (tf/parse *service-date-formatter* %))}
   (into {} (for [d calendar-days]
              [d (partial read-string)]))))

(defn- process-calendar-map [days m]
  (let [schedule (select-keys m days)
        agg (->> schedule
                 (filter (fn [[_ bool]] (not (zero? bool))))
                 (keys)
                 (into []))]
    (merge
     (apply dissoc m (keys schedule))
     {:service/days agg})))

(defn load-calendar! [path conn]
  (->> path
       (csv->maps calendar-header-keys calendar-val-transforms)
       (map (partial process-calendar-map calendar-days))
       (map assoc-temp-id)
       (d/transact conn)))

;; Shapes ;;
;;;;;;;;;;;;

(def shapes-header-keys
  {"shape_id" :shape/id
   "shape_pt_sequence" :shape.point/sequence
   "shape_dist_traveled" :shape.point/distance-traveled
   "shape_pt_lat" :shape.point/latitude
   "shape_pt_lon" :shape.point/longitude})

(def shapes-val-transforms
  {:shape.point/sequence read-string
   :shape/distance-traveled (comp double read-string)
   :shape.point/latitude (comp double read-string)
   :shape.point/longitude (comp double read-string)})

(defn load-shapes! [path conn]
  (->> (csv->maps shapes-header-keys shapes-val-transforms path)
       (group-by :shape/id)
       (reduce (fn [acc [id attrs]]
                 (let [new-attrs (mapv #(dissoc % :shape/id) attrs)]
                   (->> new-attrs
                        (assoc {:shape/id id} :shape/points)
                        (assoc-temp-id)
                        (conj acc))))
               [])
       (d/transact conn)))

;; Trips ;;
;;;;;;;;;;;

(def trips-header-keys
  {"route_id" :trip/route
   "service_id" :trip/service
   "trip_short_name" :trip/short-name
   "trip_headsign" :trip/headsign
   "direction_id" :trip/direction
   "block_id" :trip/block
   "shape_id" :trip/shape
   "trip_id" :trip/id})

(def trips-val-transforms
  {:trip/service #(vector :service/id %)
   :trip/shape #(if (not (empty? %))
                  (vector :shape/id %)
                  nil)
   :trip/direction #(if (not (empty? %))
                      (->> % read-string zero? not)
                      nil)
   :trip/wheelchair-accessible? read-string})

(defn load-trips! [path conn]
  (->> path
       (csv->maps trips-header-keys trips-val-transforms)
       (group-by :trip/route)
       (reduce (fn [acc [id attrs]]
                 (let [new-attrs (mapv #(dissoc % :trip/route) attrs)]
                   (->> new-attrs
                        (assoc {:route/id id} :route/trips)
                        (assoc-temp-id)
                        (conj acc))))
               [])
       (d/transact conn)))

;; Frequencies ;;
;;;;;;;;;;;;;;;;;

(def frequencies-header-keys
  {"trip_id" :trip/id
   "start_time" :trip.frequency/start-time
   "end_time" :trip.frequency/end-time
   "headway_secs" :trip.frequency/headway-seconds
   "exact_times" :trip.frequency/exact-times?})

(def frequencies-val-transforms
  {:trip.frequency/headway-seconds read-string
   :trip.frequency/exact-times? (comp not zero? read-string)})

(defn load-frequencies! [path conn]
  (->> (csv->maps frequencies-header-keys
                  frequencies-val-transforms
                  path)
       (map #(assoc % :db/id [:trip/id (:trip/id %)]))
       (map #(dissoc % :trip/id))
       (d/transact conn)))

;; Stop Times ;;
;;;;;;;;;;;;;;;;

(def stop-times-header-keys
  {"trip_id" :stop-time/trip
   "stop_sequence" :stop-time/stop-sequence
   "stop_id" :stop-time/stop
   "arrival_time" :stop-time/arrival-time
   "departure_time" :stop-time/departure-time
   "stop_headsign" :stop-time/stop-headsign
   "pickup_type" :stop-time/pickup-type
   "drop_off_type" :stop-time/drop-off-type
   "shape_dist_traveled" :stop-time/shape-distance-traveled})

(def stop-times-val-transforms
  {:stop-time/stop-sequence read-string
   :stop-time/stop (partial vector :stop/id)
   :stop-time/pickup-type read-string
   :stop-time/drop-off-type read-string})

(defn load-stop-times! [path conn]
  (->> path
       (csv->maps stop-times-header-keys stop-times-val-transforms)
       (group-by :stop-time/trip)
       (reduce (fn [acc [id attrs]]
                 (->> attrs
                      (map assoc-temp-id)
                      (mapv #(dissoc % :stop-time/trip))
                      (assoc {:db/id [:trip/id id]} :trip/stops)
                      (conj acc)))
               [])
       #_(map #(assoc % :db/id [:trip/id (:stop-time/trip %)]))
       #_(map #(dissoc % :stop-time/trip))
       (d/transact-async conn)))

;; Main ;;
;;;;;;;;;;

(defn load-feed! [path conn]
  (load-agency! (str path "agency.txt") conn)
  (load-routes! (str path "routes.txt") conn)
  (load-stops! (str path "stops.txt") conn)
  (load-calendar! (str path "calendar.txt") conn)
  (load-shapes! (str path "shapes.txt") conn)
  (load-trips! (str path "trips.txt") conn)
  (load-frequencies! (str path "frequencies.txt") conn)
  (load-stop-times! (str path "stop_times.txt") conn))
