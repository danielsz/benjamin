(ns benjamin.predicates
  (:require  [detijd.core :as time]))

(def unique? #(some? %))
(def today? #(if-let [date (first (vals %))]
               (time/today? date)
               false))
(def last-3-days? #(if-let [date (first (vals %))]
                     (time/last-days? date 3)
                     false))
(def last-3-months? #(if-let [date (first (vals %))]
                       (time/last-months? date 3)
                       false))

(def always? (constantly false))
