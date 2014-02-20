(ns pundit.string
  (:require [clojure.string :as s]))

(defn kebabize [n]
  (s/lower-case
    (s/replace n #"(.)([A-Z])" "$1-$2")))

(defn camelize [n]
  (s/replace
    n
    #"(^|-)(.)"
    (fn [m] (s/upper-case (last m)))))

(defn half-camelize [n]
  (s/replace
    n
    #"-(.)"
    (fn [m] (s/upper-case (last m)))))

(defn order-camelize [n]
  (if (= (first n) \-)
    (str "-" (half-camelize (subs n 1)))
    (half-camelize n)))
