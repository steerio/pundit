(ns pundit.test.batch
  (:require [clojure.test :refer :all]
            [pundit.test.helper :refer :all]
            [pundit.api :as pa]))

(def ^:private example
  (assoc test-id :txt "Batch!"))

(use-fixtures :once authenticate (cleaner-for Foo))

(deftest all-batch-ops
  (pa/execute!
    (map
      #(pa/create Foo (assoc example :num %))
      (range 3)))
  (let [objs (pa/find-all Foo :where example)]
    ; Create
    (is (= 3 (count objs)))
    (is (= (range 3) (sort (map :num objs))))
    ; Update
    (let [delta {:num 123}]
      (pa/execute! (map #(pa/update % delta) objs))
      (is (= 3 (pa/remote-count Foo :where (merge example delta)))))
    ; Delete
    (pa/execute! (map pa/delete objs))
    (is (zero? (pa/remote-count Foo :where example)))))
