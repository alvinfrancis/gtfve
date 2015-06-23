(ns gtfve.handler
  (:require [compojure.core :refer [GET POST PUT ANY defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [liberator.core :refer [defresource]]
            [gtfve.data.queries :as q]))

(defn- begin-of-last-minute []
  (-> (System/currentTimeMillis)
      (/ 60000)
      (int)
      (* 60000)))

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

(defroutes routes
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?)}))
  (ANY "/trips" [] gtfs-trips)
  (ANY "/routes" [] gtfs-routes)
  (ANY "/routes/:id" [id] (gtfs-route id))
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults routes site-defaults)]
    (if (env :dev?) (wrap-exceptions handler) handler)))
