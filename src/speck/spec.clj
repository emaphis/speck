(ns speck.spec
  (:require [clojure.spec.alpha :as s]))

;;; Ingredient

(defn scale-ingredient [ingredient factor]
  (update ingredient :quantity * factor))

(scale-ingredient {:item "flour" :quantity 10 :unit :quart} 1/2)
;; => {:item "flour", :quantity 5N, :unit :quart}


;; Specs describing an ingredient.
(s/def ::ingredient (s/keys :req [::name ::quantity ::unit]))

(s/def ::name     string?)
(s/def ::quantity number?)
(s/def ::unit     keyword?)

(s/explain ::ingredient {:name "flour" :quantity 10 :unit :quart})

;; function spec for scale-ingredient
(s/fdef scale-ingredient
  :args (s/cat :ingredient ::ingredient :factor number?)
  :ret ::ingredient)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Validating data

;; Predicates
;; simple - boolean?, string?, keyword?, int?
;; compound - rational? number?
;; property - pos?, zero?, empty? any? some?

(s/def ::company-name string?)

(s/valid? ::company-name "Acme Moving") ;; => true
(s/valid? ::company-name 100) ;; => false


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; defining specs

;;; Enumerated values
;;  sets

(s/def ::color #{:red :green :blue})

(s/valid? ::color :red) ;; => true
(s/valid? ::color :pink) ;; => false


;; a bowling score
(s/def ::roll #{0 1 2 3 4 5 6 7 8 9 10})

(s/valid? ::roll 5) ;; => true

;; using a range spec - int-in, double-in, inst-in
(s/def ::ranged-roll (s/int-in 0 11))

(s/valid? ::ranged-roll 5) ;; => true
(s/valid? ::ranged-roll 11) ;; => false


;;; Handling `nil`

(s/def ::company-name-2 (s/nilable string?))

;; testing for the set #{true, false, nil}
(s/def ::nilable-boolean (s/nilable boolean?))


;;; Logical specs
;;  s/and s/or - combining predicates.

(s/def ::odd-int (s/and int? odd?))

(s/valid? ::odd-int 5) ;; => true
(s/valid? ::odd-int 4) ;; => false
(s/valid? ::odd-int 5.0) ;; => false

;; alternatives
(s/def ::odd-or-42 (s/or :odd ::odd-int  :42 #{42}))

;; return value is spec passes
(s/conform ::odd-or-42 42) ;; => [:42 42]
(s/conform ::odd-or-42 19) ;; => [:odd 19]

;; if failure
(s/explain ::odd-or-42 0)
;; 0 - failed: odd? at: [:odd] spec: :speck.spec/odd-int
;; 0 - failed: #{42} at: [:42] spec: :speck.spec/odd-or-42


;;; Collection specs
;;  - s/coll-of, s/map-of

(s/def ::names (s/coll-of string?))

(s/valid? ::names ["Alex" "Stu"]) ;; => true
(s/valid? ::names #{"Alex" "Stu"}) ;; => true
(s/valid? ::names '("Alex" "Stu")) ;; => true

;; options
;; :kind - predicate checked at the beginning vector? set?
;; :into - one of these literals [] () or #{} - conformed values collect.
;; :count - exact count of collection
;; :min-count, :max-count
;; :distinct - true if all elements of a collection are distinct.

(s/def ::my-set (s/coll-of int? :kind set? :min-count 2))
(s/valid? ::my-set #{10 20}) ;; => true

;; s/map-of - specs a map where the keys and values each follow a spec.
(s/def ::scores (s/map-of string? int?))

(s/valid? ::scores {"Stu" 100, "Alex" 200}) ;; => true

;;; Collection sampling
;; sampling spec s/every, s/every-kv
;; only sample a limited number of items for better performance.

;;; Tuples - vectors with a known structure where each element has it's own spec

(s/def ::point (s/tuple float? float?))
(s/conform ::point [1.3 2.7])
;; => [1.3 2.7]

(s/def ::label (s/tuple float? float? string?))
(s/conform ::label [1.3 2.7 "Here"])
;; => [1.3 2.7 "Here"]


;;; Information maps


;; a music release:
{:spec/id #uuid "40e30dc1-55ac-33e1-85d3-1f1508140bfc"
 :spec/artist "Rush"
 :spec/title "Moving Pictures"
 :spec/date #inst "1981-02-12"}

(s/def :spec/id uuid?)
(s/def :spec/artist string?)
(s/def :spec/title string?)
(s/def :spec/date inst?)

;; s/keys to specify map attributes
(s/def :spec/release
  (s/keys :req [:spec/id]
          :opt [:spec/artist
                :spec/title
                :spec/date]))

;; but, unqualified keys map
{:id #uuid "40e30dc1-55ac-33e1-85d3-1f1508140bfc"
 :artist "Rush"
 :title "Moving Pictures"
 :date #inst "1981-02-12"}

(s/def :spec/release-unqualified
  (s/keys :req-un [:spec/id]
          :opt-un [:spec/artist
                   :spec/title
                   :spec/date]))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; validating functions
