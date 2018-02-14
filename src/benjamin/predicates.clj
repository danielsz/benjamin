(ns benjamin.predicates
  (:require  [detijd.core :as time]))

(def unique? #(some? %))

(def last-minute? #(if-let [date (first (vals %))]
                       (time/last-minutes? date 1)
                       false))

(defn last-minutes [x]
  #(if-let [date (first (vals %))]
     (time/last-minutes? date x)
     false))

(def last-hour? #(if-let [date (first (vals %))]
                       (time/last-hours? date 1)
                       false))

(defn last-hours? [x]
  #(if-let [date (first (vals %))]
     (time/last-hours? date x)
     false))

(def today? #(if-let [date (first (vals %))]
               (time/today? date)
               false))

(def last-3-days? #(if-let [date (first (vals %))]
                     (time/last-days? date 3)
                     false))

(def last-days? [x]
  #(if-let [date (first (vals %))]
     (time/last-days? date x)
     false))

(def last-3-months? #(if-let [date (first (vals %))]
                       (time/last-months? date 3)
                       false))

(defn last-months? [x]
  #(if-let [date (first (vals %))]
     (time/last-months? date x)
     false))

(def always? (constantly false))


