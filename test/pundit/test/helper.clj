(ns pundit.test.helper
  (import (clojure.lang ExceptionInfo))
  (require [clojure.test :refer :all]
           [pundit.api :as pa]
           [pundit.comparisons :refer :all]))

(def test-id
  {:test
   (-> (java.util.Date.)
       .getTime
       str)})

; Private stuff

(def props
  (if-let [props (-> (Thread/currentThread)
                   .getContextClassLoader
                   (.getResourceAsStream "test.properties"))]
    (into {} (doto (java.util.Properties.) (.load props)))
    (throw (Exception. "Properties file not found, see README"))))

(def ^:private auth
  {:app (props "parse.app")
   :api-key (props "parse.api-key")
   :master-key (props "parse.master-key")})

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
    (fun)
    (pa/execute!
      (map pa/delete (pa/query klass :where test-id)))))

(defmacro http-error? [status & body]
  `(try
     ~@body
     false
     (catch ExceptionInfo e#
       (= ~status
          (-> e# .getData :status)))))
