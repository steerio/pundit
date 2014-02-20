(ns pundit.model
  (:require [pundit.api :refer :all]
            [pundit.string :refer :all]))

(defmacro defmodel
  "Defines model, i.e. defines a set of getter and finder functions in the
   namespace of the caller."
  [parse-class & {:keys [singular plural]}]
  (let [singular (or singular
                     (kebabize parse-class))
        plural (or plural
                   (str singular \s))]
  `(do
     (defn ~(symbol (str "get-" singular)) [id#]
       (retrieve ~parse-class id#))
     (def ~(symbol (str "find-one-" singular)) (partial find-one ~parse-class))
     (def ~(symbol (str "find-" plural)) (partial query ~parse-class)))))
