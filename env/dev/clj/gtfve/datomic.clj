(ns gtfve.datomic
  (:import datomic.Peer)
  (:require [clojure.string :as string :refer [split join trim]]
            [clojure.pprint :as pp :refer [pprint]]
            [clj-time.format :as tf]
            [clj-time.coerce :refer [to-date]]
            [clojure.instant :as instant :refer [read-instant-date]]
            [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]))

;; Datomic ;;
;;;;;;;;;;;;;

(def uri "datomic:dev://localhost:4334/sakay")

(def feed "resources/public/gtfs/")

(def conn (d/connect uri))

(defn load-schema! []
  (d/transact conn (read-string (slurp "resources/gtfs-schema.edn"))))

;; Utilities ;;
;;;;;;;;;;;;;;;

(defn- drop-empty [hmap]
  (->> hmap
       (filter (fn [[_ v]]
                 (try
                   (not (empty? v))
                   (catch IllegalArgumentException e
                     true))))
       (into {})))

(defn- assoc-temp-id [m]
  (assoc m :db/id (d/tempid :db.part/user)))

(defn csv->maps
  "Create a transaction from csv found in path. header-map is a map of strings
  to keywords defining column to keyword mappings for the generated transaction
  map. transform-vals is a map of keywords to functions defining function
  transformations on values in the generated transaction map.

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
  {:shape/id (fn [x] nil)
   :shape.point/sequence read-string
   :shape/distance-traveled (comp double read-string)
   :shape.point/latitude (comp double read-string)
   :shape.point/longitude (comp double read-string)})

(defn load-shapes! [path conn]
  (csv->map shapes-header-keys
          shapes-val-transforms
          path))

;; Frequencies ;;
;;;;;;;;;;;;;;;;;

(def frequencies-header-keys
  {"trip_id" :delete/trip/id
   "start_time" :trip.frequency/start-time
   "end_time" :trip.frequency/end-time
   "headway_secs" :trip.frequency/headway-seconds
   "exact_times" :trip.frequency/exact-times?})

(def frequencies-val-transforms
  {:delete/trip/id (fn [x] nil)
   :trip.frequency/headway-seconds read-string
   :trip.frequency/exact-times? (comp not zero? read-string)})

(defn load-frequencies! [path conn]
  (d/transact conn (csv->map frequencies-header-keys
                           frequencies-val-transforms
                           path)))

;; Main ;;
;;;;;;;;;;

(defn load-feed! [path conn]
  (load-agency! (str path "agency.txt") conn)
  (load-routes! (str path "routes.txt") conn)
  (load-stops! (str path "stops.txt") conn)
  (load-calendar! (str path "calendar.txt") conn)
  (load-shapes! (str path "shapes.txt") conn)
  (load-frequencies! (str path "frequencies.txt") conn))
