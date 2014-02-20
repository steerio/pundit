(ns pundit.http
  (:import (clojure.lang Associative Sequential Keyword))
  (:require [pundit.string :refer :all]
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

(definline ^:private to-json [obj]
  `(js/write-str
    ~obj
    :key-fn #(half-camelize (name %))))

; Data transformation for queries

(defmulti ^:private to-query type)

(defmethod to-query Sequential [value]
  (s/join \,
          (map
            #(order-camelize
               (if (keyword? %) (name %) %))
            value)))

(defmethod to-query Associative [value]
  (to-json value))

(defmethod to-query Keyword [value]
  (name value))

(defmethod to-query :default [value]
  value)

(prefer-method to-query Sequential Associative)

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
