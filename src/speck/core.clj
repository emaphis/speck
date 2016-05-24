(ns speck.core
  (:require [clojure.spec :as s]))

;; Following the tutorial at: <http://clojure.org/guides/spec>


;;; does data conform to a predicate
;; implicitly convers a predicate to a spec
(s/conform even? 1000)
;; => 1000

;; similar but return a boolean
(s/valid? even? 10)
;; => true


;;; some examples

speck.core>
(s/valid? nil? nil) ;; true
(s/valid? string? "abc") ;; true
(s/valid? #(> % 5) 10) ;; true
(s/valid? #(> % 5) 2) ;; false

(import java.util.Date)
(s/valid? #(instance? Date %) (Date.)) ;; true


;; Sets can also be used as predicates that match one or more literal values:
(s/valid? #{:club :diamond :heart :spade} :club) ;; true
(s/valid? #{:club :diamond :heart :spade} 42)  ;; false


;;; Registry
;; specs can be registered in a central namespaced registry

(s/def ::date #(instance? Date %)) ;; :spec.core/date
(s/def ::suit #{:club :diamond :heart :spade}) ;; :spec.core/suit

;; using registered specs
(s/valid? ::date (Date.)) ;; true
(s/conform ::suit :club)  ;; :club


;;; Composing predicates

(s/def ::big-even (s/and integer? even? #(> % 1000)))
(s/valid? ::big-even :foo) ;; false
(s/valid? ::big-even 10)   ;; false
(s/valid? ::big-even 10000) ;; true

;; use s/or to specify two alternatives
(s/def ::name-or-id (s/or :name string?
                          :id   integer?))
(s/valid? ::name-or-id "abd") ;; true
(s/valid? ::name-or-id 100) ;; true
(s/valid? ::name-or-id :foo) ;; false

;; or spec is the first case we’ve seen that involves a choice during validity checking
;; or is conformed, it returns a vector with the tag name and conformed value:
(s/conform ::name-or-id "abd") ;; [:name "abd"]
(s/conform ::name-or-id 100)  ;; [:id 100]

;; To include nil as a valid value, use the provided function nilable to make a spec:
(s/valid? string? nil) ;; false
(s/valid? (s/nilable string?) nil) ;; true


;;; Explain
;;  reports 'why' a value does not conform to a spec

(s/explain ::suit 42)
;; val: 42 fails predicate: #{:spade :heart :diamond :club}

(s/explain ::big-even 5)
;; val: 5 fails predicate: even?

(s/explain ::name-or-id :foo)
;; At: [:name] val: :foo fails predicate: string?
;; At: [:id] val: :foo fails predicate: integer?

;; use explain-data to receive the errors as data, which can be attached to
;; an exception or acted upon for further analysis.
(s/explain-data ::name-or-id :foo)
;;{:clojure.spec/problems
;; {[:name] {:pred string?, :val :foo, :via []},
;;  [:id] {:pred integer?, :val :foo, :via []}}}


;;; Sequences
;; uses  regular expression operators to describe the structure of a sequential data value:

;; an ingredient represented by a vector containing a quantity (number) and a unit (keyword)
(s/def ::ingedient (s/cat :quantity number? :unit keyword?))

(s/conform ::ingedient [2 :teaspoon])
;; {:quantity 2, :unit :teaspoon}

;; use explain to examine non-conforming data.

;; pass string for unit instead of keyword
(s/explain ::ingedient [11 "peaches"])
;; At: [:unit] val: "peaches" fails predicate: keyword?

(s/explain ::ingedient [2])
;; At: [:unit] val: () fails predicate: keyword?,  Insufficient input



