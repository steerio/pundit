(ns pundit.comparisons
  (:require [pundit.api :refer [date ptr]]
            [pundit.string :refer :all]))

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
   {:object (ptr obj)
    :key k}})

(defn earlier-than [s]
  {:$lt (date s)})

(defn later-than [s]
  {:$gt (date s)})

(defn older-than [s]
  {:created-at (earlier-than s)})

(defn newer-than [s]
  {:created-at (later-than s)})
