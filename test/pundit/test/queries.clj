(ns pundit.test.queries
  (:require [clojure.test :refer :all]
            [pundit.test.helper :refer :all]
            [pundit.api :as pa]
            [pundit.query :as q]
            [pundit.comparisons :as c]))

(defn- create-records [fun]
  (pa/create Foo {:foo 999
                  :bar "Find me"})
  (fun))

(use-fixtures :once authenticate create-records (cleaner-for Foo))

(deftest where-chaining
  (let [-get #(:where (.getQuery %))
        exact (pa/query Foo :where {:foo 123})
        compd (pa/query Foo :where {:foo {:$lt 200}})]
    ; Verifying if `a` and `b` have the same query
    (are [a b] (= (-get a) (-get b))
         ; Adding the same key to an already exact query on a key: no effect
         exact
         (q/where exact {:foo {:$gt 100}})
         ; Updating a comparison shall work
         (q/where compd {:foo {:$lt 300}})
         (pa/query Foo :where {:foo {:$lt 300}})
         ; Adding another comparison: AND
         (q/where compd {:foo {:$gt 100}})
         (pa/query Foo :where {:foo {:$gt 100 :$lt 200}}))
    ; Verifying if the keys in the where query match our expectation
    (are [expected q] (= expected (-> q -get keys set))
         ; Adding a completely different key: AND
         #{:foo :bar} (q/where exact {:bar "Lol"})
         #{:foo :bar} (q/where compd {:bar "Hello"})
         ; Forced where: complete takeover
         #{:bar} (q/where! exact {:bar "Lol"})
         #{:bar} (q/where! compd {:bar "Hello"}))))

(deftest order-chaining
  (let [-get #(:order (.getQuery %))
        scalar (pa/query Foo :order :foo)
        vektor (pa/query Foo :order [:foo])]
    ; In every combination (adding scalar to scalar, scalar to vector, etc.)
    ; the result should be the same.
    (are [q ord] (= #{:foo :bar} (-> q (q/order ord) -get set))
         scalar :bar
         scalar [:bar]
         vektor :bar
         vektor [:bar])
    ; Forced order: complete takeover
    (are [q ord] (= ord (-> q (q/order! ord) -get))
         scalar :bar
         scalar [:bar]
         vektor :bar
         vektor [:bar])))

(deftest include-chaining
  (let [-get #(:include (.getQuery %))
        scalar (pa/query Foo :include :foo)
        vektor (pa/query Foo :include [:foo])]
    ; In every combination (adding scalar to scalar, scalar to vector, etc.)
    ; the result should be the same.
    (are [q ord] (= #{:foo :bar} (-> q (q/include ord) -get set))
         scalar :bar
         scalar [:bar]
         vektor :bar
         vektor [:bar])))

(deftest equivalence
  (are [kw fun value] (= (.getQuery (pa/query Foo kw value))
                         (.getQuery (fun (pa/query Foo) value)))
       :include q/include :bar
       :keys    q/only    :baz
       :limit   q/limit   10
       :order   q/order   :created-at
       :skip    q/skip    10))

(deftest top-level-chaining
  (is (= #{:include :limit :keys :order :skip :where}
         (-> (pa/query Foo)
             (q/include :bar)
             (q/limit 10)
             (q/only :baz)
             (q/order :created-at)
             (q/skip 10)
             (q/where {:foo 123})
             .getQuery
             keys
             set))))

(deftest delayedness
  (isnt (sequential? (pa/query Foo))))

(deftest remote-count
  (let [q (pa/query Foo)
        c (pa/remote-count q)]
    (is (integer? c))
    (is (= (count (seq q)) c))))
