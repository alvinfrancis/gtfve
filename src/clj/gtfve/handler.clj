(ns gtfve.handler
  (:require [compojure.core :refer [GET POST PUT ANY defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [liberator.core :refer [defresource]]
            [gtfve.data.queries :as q]
            [clojure.edn :as edn]))

(defn- begin-of-last-minute []
  (-> (System/currentTimeMillis)
      (/ 60000)
      (int)
      (* 60000)))

(defresource gtfs-pull [spec e]
  :available-media-types ["text/plain"
                          "text/html"
                          "application/edn"
                          "application/json"]
  :allowed-methods [:get]
  :last-modified (fn [_] (begin-of-last-minute))
  :handle-ok (fn [_] (q/pull spec e))
  )

(defresource gtfs-routes
  :available-media-types ["text/plain"
                          "text/html"
                          "application/edn"
                          "application/json"]
  :allowed-methods [:get]
  :last-modified (fn [_] (begin-of-last-minute))
  :handle-ok (fn [_] (q/routes)))

(defresource gtfs-route [id]
  :available-media-types ["text/plain"
                          "text/html"
                          "application/edn"
                          "application/json"]
  :allowed-methods [:get]
  :last-modified (fn [_] (begin-of-last-minute))
  :handle-ok (fn [_] (q/routes id)))

(defresource gtfs-trips
  :available-media-types ["text/html"
                          "application/edn"
                          "application/json"]
  :allowed-methods [:get]
  :last-modified (fn [_] (begin-of-last-minute))
  :handle-ok (fn [_] (q/trips)))

(defresource gtfs-stops-search [query pull]
  :available-media-types ["text/html"
                          "application/edn"
                          "application/json"]
  :allowed-methods [:get]
  :last-modified (fn [_] (begin-of-last-minute))
  :handle-ok (fn [_] (if pull
                       (q/stops-search query pull)
                       (q/stops-search query))))

(defresource viewport [bbox pulls]
  :available-media-types ["text/html"
                          "application/edn"
                          "application/json"]
  :allowed-methods [:get]
  :last-modified (fn [_] (begin-of-last-minute))
  :handle-ok (fn [_] (apply q/viewport bbox pulls)))

(defroutes routes
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?)}))
  (ANY "/pull" {{spec :spec e :e} :params} (gtfs-trips spec e))
  (ANY "/trips" [] gtfs-trips)
  (ANY "/routes" [] gtfs-routes)
  (ANY "/routes/:id" [id] (gtfs-route id))
  (GET "/stops-search" [query pull] (gtfs-stops-search query pull))
  (GET "/viewport" [bbox pulls] (viewport (edn/read-string bbox)
                                          (edn/read-string pulls)))
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (-> routes
                    (wrap-defaults site-defaults))]
    (if (env :dev?) (wrap-exceptions handler) handler)))
