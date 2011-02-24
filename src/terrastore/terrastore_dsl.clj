(ns terrastore.terrastore-dsl
  (:use terrastore.terrastore-ops)
  (:require [clojure.contrib.json :as json])
  (import [java.net URLEncoder]))

(def *server* "http://localhost:8000/")


(defn convert-element [a]
  (if (list? a)
    (str "(" (convert-element (second a)) " " (first  a) " " (convert-element (last a)) ")" )
    a))

(defmacro clj-to-js [more]
  (if (list? more)
    (.replaceAll (str (convert-element (second more)) " "
                      (first  more) " "
                      (convert-element (last more)))
                 "=" "==")
    (str more)))


(defn as-map [json]
  (json/read-json json))


(defmacro predicate [query]
  `{"predicate" (str "js:" (clj-to-js ~query))})


(defmacro get [key & {query :when bucket :from}]
  `(as-map (if ~query
                     (conditionally-get-value *server* ~bucket ~key (predicate ~query))
                     (get-value *server* ~bucket ~key))))

(defmacro put [document & {query :when bucket :into key :with}]
   (if query
    `(conditionally-put-value *server*  ~bucket ~key ~document (predicate ~query))
    `(put-value *server* ~bucket ~key ~document)))

(defmacro query [bucket & {query :where}]
  `(as-map (do-predicate-query *server* ~bucket (predicate ~query))))

(defmacro range-query [bucket & { from :from to :to query :when}]
  `(as-map (do-range-query *server* ~bucket {"predicate" (str "js:" (clj-to-js ~query)) "startKey" ~from "endKey" ~to})))


