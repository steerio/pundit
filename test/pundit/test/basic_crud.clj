(ns pundit.test.basic-crud
  (:import (clojure.lang ExceptionInfo))
  (:require [clojure.test :refer :all]
            [pundit.test.helper :refer :all]
            [pundit.api :as pa]))

(def ^:private example
  {:foo 123
   :bar "Some text"
   :baz ["Hello" 12.34]})

(use-fixtures :once authenticate (cleaner-for Foo))

(deftest creating-retrieving
  (let [created (pa/create Foo example)
        retrieved (pa/retrieve Foo (pa/id created))]
    (is (= created retrieved))
    (is (= example
           (select-keys retrieved [:foo :bar :baz])
           (select-keys created [:foo :bar :baz])))
    (is (:created-at created))
    (is (= (:created-at created)
           (:updated-at created)))))

(deftest retrieving-non-existent
  (is (http-error? 404
                   (pa/retrieve Foo "x"))))

(deftest updating
  (let [created (pa/create Foo example)
        up1 (pa/update created
                       {:bar "Different text"})
        up2 (pa/update (:class-name created) (pa/id created)
                       {:foo 234})
        proof (pa/retrieve Foo (pa/id created))]
    (isnt (= (:updated-at up1)
             (:updated-at up2)))
    (isnt (= (:updated-at created)
             (:updated-at up1)))
    (isnt (= (:updated-at created)
             (:updated-at up2)))
    (is (= (:updated-at up2)
           (:updated-at proof)))
    (is (= (:bar proof) "Different text"))
    (is (= (:foo proof) 234))))

(deftest destroying
  (let [obj (pa/create Foo {:bar "Delete me"})
        id (pa/id obj)]
    (isnt (http-error? 404 (pa/retrieve Foo id)))
    (pa/delete obj)
    (is (http-error? 404 (pa/retrieve Foo id)))))
