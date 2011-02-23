(ns terrastore.dsl-test
  (:use [clojure.test]
        [terrastore.terrastore-dsl]))

(deftest test-all
  (is (= "1 + 1" (clj-to-js (+ 1 1))))
  (is (= "a > 5" (clj-to-js (> "a" 5))))
  (is (= "b == 1" (clj-to-js (= "b" 1))))
  (is (= "(b == 1) or (c > 2)" (clj-to-js (or (= b 1) (> c 2)))))
  (is (=  "((b == 1) or (c == 2)) and (d == 3)"
          (clj-to-js (and (or (= "b" 1) (= "c" 2)) (= d 3))))))
