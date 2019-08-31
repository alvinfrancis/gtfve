(ns gtfve.data.export
  (:require [datomic.api :as d]
            [clojure-csv.core :as csv]
            [clojure.java.io :as io]
            [gtfve.data.connection :refer [conn uri feed]]))

(comment
  (defn maps->csv [maps ks path]
    (with-open [wrtr (io/writer out)]
      (.write wrtr (apply str (interpose "," ks)))
      (for [m maps]
        (let [row (->> ks
                       (map #(get m %))
                       (map #(if (nil? %) "" (str %)))
                       (vector)
                       (write-csv))])
        (.write wrtr row)))))
