(ns gtfve.data.connection
  (:require [datomic.api :as d]))

(def uri "datomic:dev://localhost:4334/sakay")

(def feed "resources/public/gtfs/")

(def conn (d/connect uri))
