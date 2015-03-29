(ns gtfve.data
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [gtfve.macros :refer [<?]])
  (:require [cljs.core.async :as async :refer [put! chan <! close! >!]]
            [clojure.set :as s]
            [ajax.core :as ajax]
            [goog.labs.format.csv :as csv]
            [goog.net.XhrIo :as xhr]))

(defonce feed "gtfs/")

(defn GET
  ([url] (GET url {}))
  ([url args]
   (let [ch (chan 1)
         default-opts {:error-handler (fn [{:keys [status status-text] :as err}]
                                        (go
                                          (put! ch (js/Error. (pr-str err)))))
                       :handler (fn [res]
                                  (go
                                    (put! ch res)))}
         opts (merge default-opts args)]
     (apply ajax/GET url (vector opts))
     ch)))

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

(defonce db (atom {}))

(defn init! []
  "Initialise data"
  (go
    (let [shapes (->> (<! (GET (str feed "shapes.txt")))
                      (csv/parse)
                      (csv->map))
          routes (->> (<! (GET (str feed "routes.txt")))
                      (csv/parse)
                      (csv->map))
          stops (->> (<! (GET (str feed "stops.txt")))
                     (csv/parse)
                     (csv->map))
          stop-times (->> (<! (GET (str feed "stop_times.txt")))
                          (csv/parse)
                          (csv->map))
          trips (->> (<! (GET (str feed "trips.txt")))
                     (csv/parse)
                     (csv->map))]
      (swap! db assoc
             :shapes shapes
             :routes routes
             :stops stops
             :stop-times stop-times
             :trips trips))))

(defn GET-edn [url & {:as args}]
  (GET url (merge args {:response-format :edn})))

(defn connect [rest-api-url storage db-name]
  {:url rest-api-url
   :db/alias (str storage "/" db-name)})

(defn q
  [query conn & args]
  (let [args (into [(select-keys conn [:db/alias])] args)]
    (GET-edn (str (:url conn) "/api/query")
             :params {:q (pr-str query)
                      :args (pr-str args)})))

(def conn (connect "http://localhost:8001" "dev" "sakay"))
