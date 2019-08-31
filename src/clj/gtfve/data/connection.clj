(ns gtfve.data.connection
  (:require [datomic.api :as d]))

;; (def uri "datomic:dev://localhost:4334/sakay")
(def uri "datomic:sql://sakay?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic")
(def feed "resources/public/gtfs/")

(def conn (d/connect uri))
