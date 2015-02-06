(ns pundit.test.datatypes
  (:import (org.joda.time DateTime))
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [pundit.test.helper :refer :all]
            [pundit.api :as pa]))

(defn- create-records [fun]
  (let [foo (pa/create! Foo (assoc test-id :time (t/now)))]
    (pa/create! Bar (assoc test-id :foo foo)))
  (fun))

(use-fixtures :once
              authenticate
              create-records
              (cleaner-for Foo)
              (cleaner-for Bar))

(deftest timestamps
  (let [foo (pa/find-one Foo :where {:time {:$exists true}})
        t (t/now)]
    (is (instance? DateTime (:time foo)))
    (is (instance? DateTime (:created-at foo)))
    (pa/update! foo {:time t})
    (is (= t (:time (pa/reload foo))))))

(deftest pointers
  (let [bar (pa/find-one Bar :where {:foo {:$exists true}} :include :foo)
        foo (:foo bar)]
    (is (map? foo))
    (is (= foo (pa/reload foo)))))
