(ns pundit.test.helper
  (import (clojure.lang ExceptionInfo))
  (require [clojure.test :refer :all]
           [pundit.api :as pa]
           [pundit.comparisons :refer :all]))

; Private stuff

(def ^:private props
  (if-let [props (-> (Thread/currentThread)
                   .getContextClassLoader
                   (.getResourceAsStream "test.properties"))]
    (into {} (doto (java.util.Properties.) (.load props)))
    (throw (Exception. "Properties file not found, see README"))))

(def ^:private auth
  [(props "parse.app") (props "parse.key")])

; Shorthand class names

(def Foo "PunditFoo")

(def Bar "PunditBar")

; Public helpers

(defmacro isnt [form & more]
  `(is (not ~form) ~@more))

(defn authenticate [fun]
  (pa/with-auth auth (fun)))

(defn cleaner-for [klass]
  (fn [fun]
    (let [ts (:created-at (pa/find-one klass :order :-created-at))]
      (fun)
      (doseq [obj (pa/query klass :where (newer-than ts))]
        (pa/delete obj)))))

(defmacro http-error? [status & body]
  `(try
     ~@body
     false
     (catch ExceptionInfo e#
       (= ~status
          (-> e# .getData :object :status)))))
