(ns gtfve.data.transact
  (:require [datomic.api :as d]
            [gtfve.data.connection :refer [conn uri feed]]))

(def ^:dynamic *user* :dev)

;; TODO: add provenance schema :audit/user
(defn transact [datoms]
  (d/transact conn
              (conj datoms
                    {:db/id (d/tempid :db.part/tx) :audit/user *user*})))
