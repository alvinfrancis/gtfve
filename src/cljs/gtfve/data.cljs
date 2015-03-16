(ns gtfve.data
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [put! chan <! close!]]
            [goog.labs.format.csv :as csv]
            [goog.net.XhrIo :as xhr]))

(defonce feed "gtfs/")

(defn- GET
  "Use XHR to get URL resource.  Returns a channel for getting response text."
  [url]
  (let [ch (chan 1)]
    (xhr/send url
              (fn [event]
                (let [res (-> event .-target .getResponseText)]
                  (go (>! ch res)
                      (close! ch)))))
    ch))

(defn csv->map [csv]
  (letfn [(drop-empty [hmap]
            (->> hmap
                 (filter (fn [[_ v]]
                           (not (empty? v))))
                 (into {})))]
    (let [headers (->> (first csv)
                       (map keyword))
          rows (rest csv)]
      (->> rows
           (map (partial zipmap headers))
           (map drop-empty)))))

(go
  (defonce shapes
    (->> (<! (GET (str feed "shapes.txt")))
         (csv/parse)
         (csv->map)))

  (defonce routes
    (->> (<! (GET (str feed "routes.txt")))
         (csv/parse)
         (csv->map)))

  (defonce stops
    (->> (<! (GET (str feed "stops.txt")))
         (csv/parse)
         (csv->map)))

  (defonce stop-times
    (->> (<! (GET (str feed "stop_times.txt")))
         (csv/parse)
         (csv->map)))

  (defonce trips
    (->> (<! (GET (str feed "trips.txt")))
         (csv/parse)
         (csv->map))))
