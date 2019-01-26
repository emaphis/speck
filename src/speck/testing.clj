(ns speck.testing
  (:require [clojure.test :refer :all]))

;;; Testing examples

(deftest add-1-to-1
  (is (= 2 (+ 1 1))))

(deftest addition-tests
  (is (= 5 (+ 3 2)))
  (is (= 10 (+ 5 5))))

;; testing a function

(defn add [x y] (+ x y))

(deftest add-x-to-y
  (is (= 5 (add 2 3))))



(deftest add-x-to-y-a-few-time
  (is (= 5 (add 2 3)))
  (is (= 5 (add 1 4)))
  (is (= 5 (add 2 3))))


;; verify several values -  special syntax
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


;;; `is`

(is (= 4 (+ 2 2)) "Two plus two should equal 4")
(is (instance? Integer 256))
(is (.startsWith "abcde" "ab"))
;;(is (thrown? c body))
(is (thrown? ArithmeticException (/ 1 0)))
;; => #error {
;; :cause "Divide by zero"
;; :via
;; [{:type java.lang.ArithmeticException
;; :message "Divide by zero"
;; :at [clojure.lang.Numbers divide "Numbers.java" 188]}]
;; ...


(testing "Arithmetic"
  (testing "with positive integers"
    (is (= 4 (+ 2 2)))
    (is (= 7 (+ 3 4))))
  (testing "with negative integers"
    (is (= -4 (+ -2 -2)))
    (is (= -1 (+ 3 -4)))))


;;(run-all-tests #'speck.testing)
(run-all-tests)
