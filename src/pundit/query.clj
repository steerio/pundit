(ns pundit.query)

(defmacro ^:private defadd [n & {:keys [merge-fn key]}]
  `(defn ~n [q# value#]
     (.add q#
           {~(or key (keyword n)) value#}
           ~(if merge-fn
              `#(merge-with ~merge-fn % %2)
              merge))))

(defn- combine [o n]
  (case [(sequential? o) (sequential? n)]
    [true true] (into (vec o) n)
    [true false] (conj (vec o) n)
    [false true] (into [o] n)
    [false false] [o n]))

(defn- merge-where [om nm]
  (merge-with
    (fn [o n]
      (cond
        (not (map? o)) o
        (not (map? n)) n
        :default (merge o n)))
    om nm))

(defadd include :merge-fn combine)

(defadd limit)

(defadd only :key :keys)

(defadd order :merge-fn combine)

(defadd order! :key :order)

(defadd skip)

(defadd where :merge-fn merge-where)

(defadd where! :key :where)
