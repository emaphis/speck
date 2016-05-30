(ns speck.fish
  (:require [clojure.spec :as s]))


;; tutorial:
;; <http://gigasquidsoftware.com/blog/2016/05/29/one-fish-spec-fish/#/index>

(def fish-numbers {0 "Zero"
                   1 "One"
                   2 "Two"})

;; our specification for a valid number is the keys of the fish-numbers map.
(s/def ::fish-number (set (keys fish-numbers)))

(s/valid? ::fish-number 1) ;; true
(s/valid? ::fish-number 5) ;; false


;; explain why five is wrong:
(s/explain ::fish-number 5)
;; val: 5 fails predicate: (set (keys fish-numbers))


;; color specification
(s/def ::color #{"Red" "Blue" "Dun"})

;; Dun rhymes with One  :-)

;;; Specifying the sequence of values
(s/def ::first-line (s/cat :n1 ::fish-number :n2 ::fish-number :c1 ::color :c2 ::color))


;; try a failing spec
(s/explain ::first-line [1 2 "Red" "Black"])
;; In: [3] val: "Black" fails spec: :speck.fish/color
;;  at: [:c2] predicate: #{"Blue" "Dun" "Red"}


;; the second number should be one bigger than the first number. The input
;; to the function is going to be the map of the destructured tag keys from
;; the ::first-line

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
;; val: {:n1 2, :n2 1, :c1 "Red", :c2 "Blue"}
;;  fails predicate: one-bigger?


;;; Generating test data -- and poetry with specification

(s/exercise ::first-line 5)

;;([(1 2 "Red" "Dun") {:n1 1, :n2 2, :c1 "Red", :c2 "Dun"}]
;; [(1 2 "Blue" "Dun") {:n1 1, :n2 2, :c1 "Blue", :c2 "Dun"}]
;; [(1 2 "Blue" "Dun") {:n1 1, :n2 2, :c1 "Blue", :c2 "Dun"}]
;; [(0 1 "Blue" "Red") {:n1 0, :n2 1, :c1 "Blue", :c2 "Red"}]
;; [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}])

;; examples dont' rhyme!

;; rhyming predicate

(defn fish-number-rhymes-with-color? [{n :n2 c :c2}]
  (or
   (= [n c] [2 "Blue"])
   (= [n c] [1 "Dun"])))

(s/def ::first-line (s/and (s/cat :n1 ::fish-number :n2 ::fish-number
                                  :c1 ::color :c2 ::color)
                           one-bigger?
                           #(not= (:c1 %) (:c2 %))
                           fish-number-rhymes-with-color?))

(s/valid? ::first-line [1 2 "Red" "Blue"]) ;; true
(s/explain ::first-line [1 2 "Red" "Dun"]) ;; nil
;; val: {:n1 1, :n2 2, :c1 "Red", :c2 "Dun"}
;;  fails predicate: fish-number-rhymes-with-color?


(s/exercise ::first-line)

;;([(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;; [(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;; [(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;; [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}]
;; [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}]
;; [(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;; [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}]
;; [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;; [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;; [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}])



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
        :ret  string?)


;; turn on the instrumentation of the validation for functions and see what happens.

(s/instrument #'fish-line)

(fish-line 1 2 "Red" "Blue")
;; "One fish. Two fish. Red fish. Blue fish."

;; bad data
(fish-line 2 1 "Red" "Blue")
;; Call to #'speck.fish/fish-line did not conform to spec: val: {:n1
;;   2, :n2 1, :c1 "Red", :c2 "Blue"} fails at: [:args] predicate:
;;   one-bigger?  :clojure.spec/args (2 1 "Red" "Blue")

;;{:clojure.spec/problems
;; {[:args]
;;  {:pred one-bigger?,
;;   :val {:n1 2, :n2 1, :c1 "Red", :c2 "Blue"},
;;   :via [],
;;   :in []}},
;; :clojure.spec/args (2 1 "Red" "Blue")}

