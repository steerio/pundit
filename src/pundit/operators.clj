(ns pundit.operators
  (:require [pundit.string :refer :all]))

(defmacro ^:private defop
  [n & {:keys [arg op]
        :or {op (camelize (name n))}}]
  `(defn ~n
     ~@(if arg
         `([arg#] {:__op ~op ~arg arg#})
         `([]     {:__op ~op}))))

(defop increment
  :arg :amount)

(defop add
  :arg :objects)

(defop add-unique
  :arg :objects)

(defop add-rel
  :arg :objects
  :op "AddRelation")

(defop remove-rel
  :arg :objects
  :op "RemoveRelation")

(defop delete)
