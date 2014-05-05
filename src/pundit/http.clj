(ns pundit.http
  (:import (clojure.lang IPersistentMap IPersistentSet Keyword Sequential)
           (java.util Calendar Collection Date)
           (org.joda.time DateTime)
           (org.joda.time.base AbstractDateTime))
  (:require [pundit.string :refer :all]
            [clj-time.format :as tf]
            [clj-http.client :as http]
            [clojure.string :as s]
            [clojure.data.json :as js]))

(def ^:private ^:dynamic *base*
  "https://api.parse.com/1/")

(def ^:private headers
  {"Accept" "application/json"
   "User-Agent" (format "Pundit/%s (Clojure %s)"
                        (System/getProperty "pundit.version")
                        (clojure-version))})

; Paths

(defprotocol Path
  (path-string [this]))

(extend-protocol Path
  Sequential
  (path-string [this] (s/join \/ this))
  String
  (path-string [this] this)
  Object
  (path-string [this] (str this)))

; JSON

(def ^:dynamic *date-formatter*
  (tf/formatters :date-time))

(defmulti ^:private json-value #(type %2))

(defmethod json-value AbstractDateTime [k v]
  {:__type "Date" :iso (tf/unparse *date-formatter* v)})

(defmethod json-value Calendar [k v]
  (json-value k (DateTime. v)))

(defmethod json-value Date [k v]
  (json-value k (DateTime. v)))

(defmethod json-value IPersistentMap [k value]
  (if (and (not (:__type value))
           (:class-name value)
           (:object-id value))
    (assoc
      (select-keys value [:class-name :object-id])
      :__type "Pointer")
    value))

(defmethod json-value Collection [k value]
  (map #(json-value k %) value))

(defmethod json-value :default [k v] v)

(definline ^:private to-json [obj]
  `(js/write-str
     ~obj
     :key-fn #(half-camelize (name %))
     :value-fn json-value))

; Data transformation for queries

(defmulti ^:private to-query type)

(defmethod to-query Collection [value]
  (s/join \, (map to-query value)))

(defmethod to-query IPersistentMap [value]
  (to-json value))

(defmethod to-query Keyword [value]
  (order-camelize (name value)))

(defmethod to-query :default [value]
  value)

(defn- map->query [m]
  (reduce
    (fn [out [k v]]
      (assoc out k (to-query v)))
    {} m))

; Requests

(defn- request [uri auth req-map]
  (-> req-map
      (merge {:url (str *base* (path-string uri))
              :headers headers
              :basic-auth auth})
      http/request
      :body
      (js/read-str :key-fn #(keyword (kebabize %)))))

(defn- post-like [method uri auth data]
  (request
    uri auth
    {:body (to-json data)
     :content-type :json
     :method method}))

(defn GET
  ([uri auth params]
   (request
     uri auth
     {:method :get
      :query-params (map->query params)}))
  ([uri auth]
   (request uri auth {:method :get})))

(defn POST [uri auth data]
  (post-like :post uri auth data))

(defn PUT [uri auth data]
  (post-like :put uri auth data))

(defn DELETE [uri auth]
  (request uri auth {:method :delete}))
