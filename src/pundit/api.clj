(ns pundit.api
  (:require [delayed-map.core :refer :all]
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

(defn- value->obj [value]
  (cond
    (map? value) (case (:__type value)
                   "Pointer" (delayed-ptr value)
                   value)
    (sequential? value) (mapv value->obj value)
    :default value))

(defn- result->obj [klass m]
  (reduce
    (fn [acc [k v]] (assoc acc k (value->obj v)))
    {:class-name klass}
    m))

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
       (map #(result->obj klass %))))

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
  ([class-name object-id]
   {:__type "Pointer"
    :class-name class-name
    :object-id object-id})
  ([{:keys [class-name object-id]}]
   (ptr class-name object-id)))

(defn ptrs
  "Creates pointers"
  ([objs]
   (map ptr objs))
  ([klass ids]
   (map #(ptr klass %) ids)))

(defn date [s]
  {:__type "Date"
   :iso s})

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
  (result->obj
    klass
    (GET ["classes" klass id] *auth*)))

(defn reload [{:keys [class-name object-id]}]
  (retrieve class-name object-id))

; Creating an object

(defn create
  "Creates item on Parse"
  ([klass obj]
   (result->obj
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
   (PUT ["classes" klass id] *auth* delta))
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
