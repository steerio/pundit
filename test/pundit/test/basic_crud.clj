(ns pundit.test.basic-crud
  (:require [clojure.test :refer :all]
            [pundit.test.helper :refer :all]
            [pundit.api :as pa]))

(def ^:private example
  (merge
    test-id
    {:num 123
     :txt "Some text"
     :arr ["Hello" 12.34] }))

(use-fixtures :once authenticate (cleaner-for Foo))

(deftest creating-retrieving
  (let [created (pa/create Foo example)
        retrieved (pa/retrieve Foo (pa/id created))]
    (is (= created retrieved))
    (is (= example
           (select-keys retrieved [:num :txt :arr :test])
           (select-keys created [:num :txt :arr :test])))
    (is (:created-at created))
    (is (= (:created-at created)
           (:updated-at created)))))

(deftest retrieving-non-existent
  (is (http-error? 404
                   (pa/retrieve Foo "x"))))

(deftest updating
  (let [created (pa/create Foo example)
        up1 (pa/update created
                       {:txt "Different text"})
        up2 (pa/update (:class-name created) (pa/id created)
                       {:num 234})
        proof (pa/retrieve Foo (pa/id created))]
    (isnt (= (:updated-at up1)
             (:updated-at up2)))
    (isnt (= (:updated-at created)
             (:updated-at up1)))
    (isnt (= (:updated-at created)
             (:updated-at up2)))
    (is (= (:updated-at up2)
           (:updated-at proof)))
    (is (= (:txt proof) "Different text"))
    (is (= (:num proof) 234))))

(deftest destroying
  (let [obj (pa/create Foo {:txt "Delete me"})
        id (pa/id obj)]
    (isnt (http-error? 404 (pa/retrieve Foo id)))
    (pa/delete obj)
    (is (http-error? 404 (pa/retrieve Foo id)))))
