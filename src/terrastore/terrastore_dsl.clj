(ns terrastore.terrastore-dsl
  (:use terrastore.terrastore-cloj )
  (:use terrastore.terrastore-ops)
  (import [java.net URLEncoder]))

(def *server* "http://localhost:8000/")

(defmacro clj-to-js [more]
  (.replaceAll (str (second more) (first  more) (last more)) "=" "=="))


(def type-json {"Content-Type" "applucation/json"})

(defn- create-get* [bucket key predicate]
  (str   *server* bucket "/" key "?predicate=js:" (URLEncoder/encode predicate)))

(defmacro predicate [query]
  `{"predicate" (str "js:" (clj-to-js ~query))})

(defmacro get-str [key & {query :when bucket :from}] 
  `(create-get*  ~bucket ~key (clj-to-js~query)))

(defmacro get [key & {query :when bucket :from}] 
  `(conditionally-get-value *server* ~bucket ~key (predicate ~query)))

(defmacro put [document & {query :when bucket :into key :with}] 
  `(conditionally-put-value *server*  ~bucket ~key ~document (predicate ~query)))

(defmacro query [bucket & {query :where}]
  `(do-predicate-query *server* ~bucket (predicate ~query)))

(defmacro range-query [bucket & { from :from to :to query :when}]
  `(do-range-query *server* ~bucket {"predicate" (str "js:" (clj-to-js ~query)) "startKey" ~from "endKey" ~to}))

