(ns speck.fish
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]))

;; tutorial:
;; <http://gigasquidsoftware.com/blog/2016/05/29/one-fish-spec-fish/#/index>

;;; Specifying the values of the parameters.

;; a notion of fish numbers
(def fish-numbers {0 "Zero"
                   1 "One"
                   2 "Two"})

;; our specification for a valid number is the keys of the fish-numbers map.
(s/def ::fish-number (set (keys fish-numbers)))

(s/valid? ::fish-number 1) ;; true
(s/valid? ::fish-number 5) ;; false
(s/valid? ::fish-number "One") ;; false


;; explain why five is wrong:
(s/explain ::fish-number 5)
;; 5 - failed: (set (keys fish-numbers)) spec: :speck.fish/fish-number


;; color specification, including Dun
(s/def ::color #{"Red" "Blue" "Dun"})

;; Dun rhymes with One  :-)


;;; Specifying the sequence of values
;;  two numbers followed by two colors using `s/cat` for concatenation
;;  of predicates/patterns.
(s/def ::first-line (s/cat :n1 ::fish-number :n2 ::fish-number :c1 ::color :c2 ::color))

;; try a failing spec
(s/explain ::first-line [1 2 "Red" "Black"])
;; "Black" - failed: #{"Blue" "Dun" "Red"} in: [3] at: [:c2] spec: :speck.fish/color


;; The second number should one be bigger than the first number. The input
;; to the function is going to be the map of the destructured tag keys from
;; the ::first-line.
(defn one-bigger? [{:keys [n1 n2]}]
  (= n2 (inc n1)))

;; colors should not be the same value. We can add these additional specifications
;; with s/and

(s/def ::first-line (s/and (s/cat :n1 ::fish-number :n2 ::fish-number
                                  :c1 ::color :c2 ::color)
                           one-bigger?
                           #(not= (:c1 %) (:c2 %))))

;; test our data
(s/valid? ::first-line [1 2 "Red" "Blue"]) ;; true

;; the destructured, conformed values
(s/conform ::first-line [1 2 "Red" "Blue"])
;; {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}


;; failing values can be identified:
(s/valid? ::first-line [2 1 "Red" "Blue"])

(s/explain ::first-line [2 1 "Red" "Blue"])
;; {:n1 2, :n2 1, :c1 "Red", :c2 "Blue"} - failed: one-bigger? spec: :speck.fish/first-line


;;; Generating test data -- and poetry with specification

(s/exercise ::first-line 5)
;; ([(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;;  [(0 1 "Dun" "Blue") {:n1 0, :n2 1, :c1 "Dun", :c2 "Blue"}]
;;  [(0 1 "Blue" "Red") {:n1 0, :n2 1, :c1 "Blue", :c2 "Red"}]
;;  [(0 1 "Red" "Blue") {:n1 0, :n2 1, :c1 "Red", :c2 "Blue"}]
;;  [(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}])

;; examples dont' rhyme!

;; rhyming predicate
(defn fish-number-rhymes-with-color? [{n :n2 c :c2}]
  (or
   (= [n c] [2 "Blue"])
   (= [n c] [1 "Dun"])))

;; add to first-line spec
(s/def ::first-line (s/and (s/cat :n1 ::fish-number :n2 ::fish-number
                                  :c1 ::color :c2 ::color)
                           one-bigger?
                           #(not= (:c1 %) (:c2 %))
                           fish-number-rhymes-with-color?))

(s/valid? ::first-line [1 2 "Red" "Blue"]) ;; true
(s/explain ::first-line [1 2 "Red" "Dun"]) ;; nil
;; {:n1 1, :n2 2, :c1 "Red", :c2 "Dun"}
;; - failed: fish-number-rhymes-with-color? spec: :speck.fish/first-line

;; data generation with new spec.
(s/exercise ::first-line)
;; ([(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;;  [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}]
;;  [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}]
;;  [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}]
;;  [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}]
;;  [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;;  [(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;;  [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}]
;;  [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;;  [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}])


;; Using spec with functions

;; a function to create a string for our mini-poem from our data.

(defn fish-line [n1 n2 c1 c2]
  (clojure.string/join " "
                       (map #(str % " fish.")
                            [(get fish-numbers n1)
                             (get fish-numbers n2)
                             c1
                             c2])))

;; specify that the args for this function be validated with ::first-line
;; and the return value is a string.
(s/fdef fish-line
  :args ::first-line
  :ret string?)

;; turn on the instrumentation of the validation for functions and see what happens.

(stest/instrument `fish-line)

(fish-line 1 2 "Red" "Blue")
;; "One fish. Two fish. Red fish. Blue fish."

;; bad data?
(fish-line 2 1 "Red" "Blue")
;; Spec: #object[clojure.spec.alpha$and_spec_impl$reify__2183 0xe111046
;;               "clojure.spec.alpha$and_spec_impl$reify__2183@e111046"]
;; Value: (2 1 "Red" "Blue")

;; Problems:

;; val: {:n1 2, :n2 1, :c1 "Red", :c2 "Blue"}
;; failed: one-bigger?
