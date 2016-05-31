(ns speck.testing
  (:require [clojure.test :refer :all]))


(deftest add-1-to-1
  (is (= 2 (+ 1 1))))


;; testing a function

(defn add [x y] (+ x y))

(deftest add-x-to-y
  (is (= 5 (add 2 3))))


(deftest add-x-to-y-a-few-time
  (is (= 5 (add 2 3)))
  (is (= 5 (add 1 4)))
  (is (= 5 (add 2 3))))


;; verify several values
(deftest add-x-to-y-using-are
  (are [x y] (= 5 (add x y))
    2 3
    1 4
    3 2))


;; grab values out of a map
(deftest grab-map-values-using-are
  (are [y z] (= y (:x z))
       2 {:x 2}
       1 {:x 1}
       3 {:x 3 :y 4}))



;;(run-all-tests #'speck.testing)
;;(run-all-tests)
