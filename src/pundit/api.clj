(ns pundit.api
  (:require [delayed-map.core :refer :all]
            [clj-time.format :as tf]
            [pundit.string :refer :all]
            [pundit.http :refer :all])
  (:import (clojure.lang IPersistentMap) (pundit Query)))

(def ^:dynamic *auth* [])

(defn connect! [auth]
  (def ^:dynamic *auth* auth))

(defmacro with-auth [auth & body]
  `(binding [*auth* ~auth] ~@body))

; Pointer

(declare retrieve)

(defn- load-pointer [{:keys [class-name object-id]}]
  (retrieve class-name object-id))

(defn- delayed-ptr [m]
  (delayed-map
    (select-keys m [:class-name :object-id])
    load-pointer))

; Value transformation

(defmulti ^:private transform-map :__type)

(defn- transform-value [value]
  (cond
    (map? value) (transform-map value)
    (sequential? value) (mapv transform-value value)
    :default value))

(defn- transform-object
  ([raw]
   (reduce
     (fn [acc [k v]] (assoc acc k (transform-value v)))
     (reduce
       (fn [acc k]
         (if-let [v (get acc k)]
           (assoc acc k (tf/parse v))
           acc))
       (select-keys raw [:object-id :class-name :created-at :updated-at])
       [:created-at :updated-at])
     (dissoc raw :object-id :class-name :created-at :updated-at)))
  ([klass raw]
   (assoc (transform-object raw)
          :class-name
          klass)))

(defmethod transform-map "Pointer" [x]
  (delayed-ptr x))

(defmethod transform-map "Date" [x]
  (tf/parse (:iso x)))

(defmethod transform-map "Object" [x]
  (transform-object (dissoc x :__type)))

(defmethod transform-map :default [x] x)

; Querying

(def ^:dynamic *query-window* 100)

(defn- next-query [q skip limit]
  (merge-with +
              (if limit
                (assoc q :limit (- limit *query-window*))
                q)
              {:skip *query-window*}))

(defn- get-query [klass auth {:keys [order] :as q}]
  (->> (GET ["classes" klass] auth q)
       :results
       (map #(transform-object klass %))))

(defn- load-query [klass auth {:keys [skip limit] :as q}]
  ; Auth is passed so that a query can be realized
  ; outside of a dynamic binding.
  (if (and limit (<= limit *query-window*))
    (get-query klass auth q)
    (let [win (get-query klass auth (assoc q :limit *query-window*))]
      (if (< (count win) *query-window*)
        win
        (let [buf (chunk-buffer *query-window*)]
          (doseq [i win] (.add buf i))
          (chunk-cons
            (chunk buf)
            (lazy-seq
              (load-query klass auth (next-query q skip limit)))))))))

;;; Public API

(defn ptr
  "Creates a pointer to an object"
  [class-name object-id]
  {:__type "Pointer"
   :class-name class-name
   :object-id object-id})

(defn ptrs
  "Creates pointers to objects"
  [klass ids]
  (map #(ptr klass %) ids))

(definline id [obj] `(:object-id ~obj))

; Querying

(defn query
  "Creates a query object. The actual API call and the pulling of results will
   only happen upon seqification, without retaining the head. Pagination is
   taking place automatically in the background."
  ([klass]
   (Query. klass load-query *auth*))
  ([klass & {:as q}]
   (Query. klass load-query *auth* q)))

(defn find-one
  "Shorthand function to eagerly return the first hit of a query."
  [klass & {:as q}]
  (first
    (load-query klass *auth* (assoc q :limit 1))))

(defn find-all
  "Shorthand function to eagerly retrieve ALL hits of a query."
  [klass & {:as q}]
  (seq (load-query klass *auth* q)))

(defn- remote-count* [klass q]
  (->> (merge q {:count 1 :limit 0})
       (GET ["classes" klass] *auth*)
       :count))

(defn remote-count
  "Remotely counts the items that the given Parse query would return"
  [klass-or-query & {:as q}]
  (if (instance? Query klass-or-query)
    (remote-count* (.getParseClass klass-or-query)
                   (merge (.getQuery klass-or-query) q))
    (remote-count* klass-or-query q)))

; Retrieving one object

(defn retrieve
  "Retrieves an object by ID from Parse"
  [klass id]
  (transform-object
    klass
    (GET ["classes" klass id] *auth*)))

(defn reload [{:keys [class-name object-id]}]
  (retrieve class-name object-id))

; Creating an object

(defn create
  "Creates item on Parse"
  ([klass obj]
   (transform-object
     klass
     (let [ret (POST ["classes" klass]
                     *auth*
                     (dissoc obj :class-name :object-id))]
       (-> obj
           (merge ret)
           (assoc :updated-at (:created-at ret))))))
  ([obj] (create (:class-name obj) obj)))

; Updating an object

(defn update
  "Updates object on Parse"
  ([klass id delta]
   (transform-object
     klass
     (PUT ["classes" klass id] *auth* delta)))
  ([{:keys [class-name object-id]} delta]
   (update class-name object-id delta)))

; Deleting an object

(defn delete
  "Deletes item on Parse"
  ([klass object-id]
   (DELETE ["classes" klass object-id] *auth*))
  ([{:keys [class-name object-id]}]
   (delete class-name object-id)))

; Calling a cloud function

(defn call
  "Calls cloud function on Parse"
  ([cloud-fn params]
   (:result
     (POST ["functions" cloud-fn]
           *auth* params)))
  ([cloud-fn]
   (call cloud-fn {})))
