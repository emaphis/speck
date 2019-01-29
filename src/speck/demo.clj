(ns speck.demo
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]))

;; http://www.bradcypert.com/an-informal-guide-to-clojure-spec/

(spec/def ::id string?)

(spec/valid? ::id "ABC-123")
;; => true


;;; Composing spec definitions

(def id-regex #"^[0-9]*$")
(spec/def ::id int?)
(spec/def ::id-regex
  (spec/and
   string?
   #(re-matches id-regex %)))

(spec/def ::id-types (spec/or ::id ::id-regex))


;; now do some checking

(spec/valid? ::id-types "12345")
;; => true
(spec/valid? ::id-types 12345)
;; => false


;;; maps

;; example map
{::name "Brad" ::age 24 ::skills '()}

(spec/def ::name string?)
(spec/def ::age int?)
(spec/def ::skills list?)

(spec/def ::developer (spec/keys :req [::name ::age]
                                 :opt [::skills]))

(spec/valid? ::developer {::name "Brad" ::age 24 ::skills '()})
;; => true

;; But if consuming Jason keys won't be namespaced.

(spec/def ::developer (spec/keys :req-un [::name ::age]
                                 :opt-un [::skills]))

;; error messages
(spec/explain ::id-types "Wrong!")
;; "Wrong!" - failed: (re-matches id-regex %)
;; at: [:speck.demo/id] spec: :speck.demo/id-regex

;;; Test.Check

(gen/generate (spec/gen int?))
;; => 51312831

(gen/generate (spec/gen ::developer))
;; => #:speck.demo{:name "5T3pVj59rl33",
;;                 :age -30}
