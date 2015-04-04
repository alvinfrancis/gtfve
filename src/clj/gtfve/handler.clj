(ns gtfve.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [gtfve.data.queries :as q]))

(defroutes routes
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?)}))
  (GET "/trips" []
      (fn [req]
        {:status 200 :body (pr-str (q/trips)) :headers {"Content-Type" "application/edn"}}))
  (GET "/routes" []
    {:status 200
     :headers {"Content-Type" "application/edn"}
     :body (pr-str (q/routes))})
  (GET "/routes/:id" [id]
    {:status 200
     :headers {"Content-Type" "application/edn"}
     :body (pr-str (q/routes id))})
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults routes site-defaults)]
    (if (env :dev?) (wrap-exceptions handler) handler)))
