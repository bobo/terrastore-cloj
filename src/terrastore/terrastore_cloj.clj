(ns terrastore.terrastore-cloj (:use clojure.contrib.json.read clojure-http.client matchure))

(defn- strip-slash [base]
  (loop [url base]
    (if (= (- (.length url) 1) (.lastIndexOf url "/"))
      (recur (.substring url 0 (- (.length url) 1)))
      url
      )
    )
  )

(defn- terrastore-error [base error]
  (if (seq error)
    (
      (def error-map (read-json error))
      ((throw (RuntimeException. (str "Message: " (error-map "message") " - Code: " (error-map "code")))))
      )
    )
  )

(defn buckets [base]
  (let [response (request (strip-slash base) "GET")]
    (cond
      (= (:code response) 200) (apply str (:body-seq response))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn values [base bucket]
  (let [url (str (strip-slash base) "/" bucket)
        response (request url "GET" {"Content-Type" "application/json"})]
    (cond
      (= (response :code) 200) (apply str (response :body-seq))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn remove-bucket [base bucket]
  (let [url (str (strip-slash base) "/" bucket)
    response (request url "DELETE")]
    (cond
      (not (= (:code response) 204)) (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn put-value [base bucket k v]
  (let [url (str (strip-slash base) "/" bucket "/" k)
    response (request url "PUT" {"Content-Type" "application/json"} {} {} v)]
    (cond
      (= (:code response) 204) (apply str (:body-seq response))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn conditionally-put-value [base bucket k v params]
  (let [url (str (strip-slash base) "/" bucket "/" k)
    response (request url "PUT" {"Content-Type" "application/json"} {} params v)]
    (cond
      (= (:code response) 204) (apply str (:body-seq response))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn get-value [base bucket k]
  (let [url (str (strip-slash base) "/" bucket "/" k)
    response (request url "GET" {"Content-Type" "application/json"})]
    (cond
      (= (response :code) 200) (apply str (response :body-seq))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn conditionally-get-value [base bucket k params]
  (let [url (str (strip-slash base) "/" bucket "/" k)
    response (request url "GET" {"Content-Type" "application/json"} {} params)]
    (cond
      (= (response :code) 200) (apply str (response :body-seq))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn remove-value [base bucket k]
  (let [url (str (strip-slash base) "/" bucket "/" k)
    response (request url "DELETE")]
    (cond
      (not (= (:code response) 204)) (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn do-export [base bucket params]
  (let [url (str (strip-slash base) "/" bucket "/export")
    response (request url "POST" {} {} params)]
    (cond
      (not (= (:code response) 204)) (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn do-import [base bucket params]
  (let [url (str (strip-slash base) "/" bucket "/import")
    response (request url "POST" {} {} params)]
    (cond
      (not (= (response :code) 204)) (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn do-update [base bucket k update params]
  (let [url (str (strip-slash base) "/" bucket "/" k "/update")
    response (request url "POST" {"Content-Type" "application/json"} {} params update)]
    (cond
      (= (response :code) 200) (apply str (response :body-seq))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn do-predicate-query [base bucket params]
  (let [url (str (strip-slash base) "/" bucket "/predicate")
    response (request url "GET" {"Content-Type" "application/json"} {} params)]
    (cond
      (= (response :code) 200) (apply str (response :body-seq))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn do-range-query [base bucket params]
  (let [url (str (strip-slash base) "/" bucket "/range")
    response (request url "GET" {"Content-Type" "application/json"} {} params)]
    (cond
      (= (response :code) 200) (apply str (response :body-seq))
      :else (terrastore-error base (apply str (:body-seq response)))
      )
    )
  )

(defn key-operations [base bucket k]
  (fn [operation & args]
    ((fn-match key-operations-match
       ([:put] (put-value base bucket k (first args)))
       ([:get] (get-value base bucket k))
       ([:remove] (remove-value base bucket k))
       ([:conditionally-put] (do (def operation-args (apply hash-map (rest args))) (conditionally-put-value base bucket k (first args) (operation-args :params))))
       ([:conditionally-get] (do (def operation-args (apply hash-map args)) (conditionally-get-value base bucket k (operation-args :params))))
       ([:update] (do (def operation-args (apply hash-map args)) (do-update base bucket k (operation-args :arguments) (operation-args :params))))
       ) operation)
    )
  )

(defn bucket-operations [base bucket]
  (fn [operation & args]
    ((fn-match bucket-operations-match
       ([:list] (values base bucket))
       ([:remove] (remove-bucket base bucket))
       ([:import] (do (def operation-args (apply hash-map args)) (do-import base bucket (operation-args :params))))
       ([:export] (do (def operation-args (apply hash-map args)) (do-export base bucket (operation-args :params))))
       ([:query-by-predicate] (do (def operation-args (apply hash-map args)) (do-predicate-query base bucket (operation-args :params))))
       ([:query-by-range] (do (def operation-args (apply hash-map args)) (do-range-query base bucket (operation-args :params))))
       ([:key] (key-operations base bucket (first args)))
       ) operation)
    )
  )

(defn terrastore [base]
  (fn [operation & args]
    ((fn-match base-match
      ([:list] (buckets base))
      ([:bucket] (bucket-operations base (first args)))
      ) operation)
    )
  )