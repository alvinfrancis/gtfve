(ns gtfve.utils.ajax
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :as ajax]
            [cljs.core.async :as async :refer [put! chan <! close! >!]]))

(defn normalize-error-response [default-response props]
  (-> default-response
      (merge props)
      (assoc :status-code (:status default-response))
      (assoc :resp (get-in default-response [:response :resp]))
      (assoc :status :failed)))

(defn managed-ajax [method url & {:as opts}]
  (let [method-fn (case method
                    :get ajax/GET
                    :head ajax/HEAD
                    :post ajax/POST
                    :put ajax/PUT
                    :delete ajax/DELETE
                    ajax/GET)
        ch (chan 1)
        default-opts {:handler #(put! ch {:response %
                                          :status :success})
                      :error-handler #(put! ch (normalize-error-response % {:url url}))
                      :finally #(close! ch)}]
    (->> default-opts
         (merge opts)
         (vector)
         (apply method-fn url))
    ch))
