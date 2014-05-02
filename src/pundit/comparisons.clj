(ns pundit.comparisons
  (:require [pundit.string :refer :all]))

(defmacro ^:private defcomp [operator]
  `(def ^:const ~(symbol (str operator))
     ~(str (camelize operator))))

(defcomp $lt)
(defcomp $lte)
(defcomp $gt)
(defcomp $gte)

(defcomp $ne)

(defcomp $in)
(defcomp $nin)
(defcomp $exists)
(defcomp $select)
(defcomp $dont-select)

(defcomp $all)

(defcomp $in-query)

(defcomp $related-to)

(defn related-to [obj k]
  {:$related-to
   {:object obj
    :key k}})

(defn earlier-than [d]
  {:$lt d})

(defn later-than [d]
  {:$gt d})

(defn older-than [d]
  {:created-at (earlier-than d)})

(defn newer-than [d]
  {:created-at (later-than d)})
