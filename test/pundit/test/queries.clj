(ns pundit.test.queries
  (:require [clojure.test :refer :all]
            [pundit.test.helper :refer :all]
            [pundit.api :as pa]
            [pundit.query :as q]
            [pundit.comparisons :as c]))

(defn- create-records [fun]
  (pa/create Foo {:num 999
                  :txt "Find me"})
  (fun))

(use-fixtures :once authenticate create-records (cleaner-for Foo))

(deftest where-chaining
  (let [-get #(:where (.getQuery %))
        exact (pa/query Foo :where {:num 123})
        compd (pa/query Foo :where {:num {:$lt 200}})]
    ; Verifying if `a` and `b` have the same query
    (are [a b] (= (-get a) (-get b))
         ; Adding the same key to an already exact query on a key: no effect
         exact
         (q/where exact {:num {:$gt 100}})
         ; Updating a comparison shall work
         (q/where compd {:num {:$lt 300}})
         (pa/query Foo :where {:num {:$lt 300}})
         ; Adding another comparison: AND
         (q/where compd {:num {:$gt 100}})
         (pa/query Foo :where {:num {:$gt 100 :$lt 200}}))
    ; Verifying if the keys in the where query match our expectation
    (are [expected q] (= expected (-> q -get keys set))
         ; Adding a completely different key: AND
         #{:num :txt} (q/where exact {:txt "Lol"})
         #{:num :txt} (q/where compd {:txt "Hello"})
         ; Forced where: complete takeover
         #{:txt} (q/where! exact {:txt "Lol"})
         #{:txt} (q/where! compd {:txt "Hello"}))))

(deftest order-chaining
  (let [-get #(:order (.getQuery %))
        scalar (pa/query Foo :order :num)
        vektor (pa/query Foo :order [:num])]
    ; In every combination (adding scalar to scalar, scalar to vector, etc.)
    ; the result should be the same.
    (are [q ord] (= #{:num :txt} (-> q (q/order ord) -get set))
         scalar :txt
         scalar [:txt]
         vektor :txt
         vektor [:txt])
    ; Forced order: complete takeover
    (are [q ord] (= ord (-> q (q/order! ord) -get))
         scalar :txt
         scalar [:txt]
         vektor :txt
         vektor [:txt])))

(deftest include-chaining
  (let [-get #(:include (.getQuery %))
        scalar (pa/query Foo :include :num)
        vektor (pa/query Foo :include [:num])]
    ; In every combination (adding scalar to scalar, scalar to vector, etc.)
    ; the result should be the same.
    (are [q ord] (= #{:num :txt} (-> q (q/include ord) -get set))
         scalar :txt
         scalar [:txt]
         vektor :txt
         vektor [:txt])))

(deftest equivalence
  (are [kw fun value] (= (.getQuery (pa/query Foo kw value))
                         (.getQuery (fun (pa/query Foo) value)))
       :include q/include :txt
       :keys    q/only    :arr
       :limit   q/limit   10
       :order   q/order   :created-at
       :skip    q/skip    10))

(deftest top-level-chaining
  (is (= #{:include :limit :keys :order :skip :where}
         (-> (pa/query Foo)
             (q/include :txt)
             (q/limit 10)
             (q/only :arr)
             (q/order :created-at)
             (q/skip 10)
             (q/where {:num 123})
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
