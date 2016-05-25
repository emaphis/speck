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


;; various occurence operators *, +, and ?:

(s/def ::seq-of-keywords (s/* keyword?))

(s/conform ::seq-of-keywords [:a :b :c])
;; [:a :b :c]

(s/explain ::seq-of-keywords [10 20])
;; val: 10 fails spec: :speck.core/seq-of-keywords predicate: keyword?

(s/def ::odds-then-maybe-even (s/cat :odds (s/+ odd?)
                                     :even (s/? even?)))
(s/conform ::odds-then-maybe-even [1 3 5 100])
;; {:odds [1 3 5], :even 100}

(s/conform ::odds-then-maybe-even [1])
;; {:odds [1]}

(s/explain ::odds-then-maybe-even [100])
;; At: [:odds] val: 100 fails spec: :speck.core/odds-then-maybe-even predicate: odd?

;; opts are alternating keyword and booleans
(defn boolean? [b] (instance? Boolean b))
(s/def ::opts (s/* (s/cat :opt keyword? :val boolean?)))
(s/conform ::opts [:silent? false :verbose true])
;; [{:opt :silent?, :val false} {:opt :verbose, :val true}]


;; use alt to specify alternatives within the sequential data
(s/def ::config (s/*
                 (s/cat :prop string?
                        :val (s/alt :s string? :b boolean?))))

(s/conform ::config ["-server" "foo" "-verbose" true "-user" "joe"])

;;[{:prop "-server", :val [:s "foo"]}
;; {:prop "-verbose", :val [:b true]}
;; {:prop "-user", :val [:s "joe"]}]


;; If you need a description of a specification, use describe to retrieve one
(s/describe ::seq-of-keywords)
;; (* keyword?)

(s/describe ::odds-then-maybe-even)
;; (cat :odds (+ odd?) :even (? even?))

(s/describe ::opts)
;; (* (cat :opt keyword? :val boolean?))


;; &, which takes a regex operator and constrains it with one or more additional predicates.
(s/def ::even-strings (s/& (s/* string?) #(even? (count %))))
(s/valid? ::even-strings ["a"]) ;; false
(s/valid? ::even-strings ["a" "b"]) ;; true
(s/valid? ::even-strings ["a" "b" "c"]) ;; false
(s/valid? ::even-strings [1 2]) ;; false
(s/explain ::even-strings [1 2])
;; val: 1 fails spec: :speck.core/even-strings predicate: string?


;; include a nested sequential collection, you must use an explicit call to spec to start
;; a new nested regex context
;; for example:  [:names ["a" "b"] :nums [1 2 3]]

(s/def ::nested
  (s/cat :names-kw #{:names}
         :names (s/spec (s/* string?))
         :nums-kw #{:nums}
         :nums (s/spec (s/* number?))))

(s/conform ::nested [:names ["a" "b"] :nums [1 2 3]])
;; {:names-kw :names, :names ["a" "b"], :nums-kw :nums, :nums [1 2 3]}

;; If the specs were removed this spec would instead match a sequence
;; like [:names "a" "b" :nums 1 2 3]
(s/def ::unnested
  (s/cat :names-kw #{:names}
         :names (s/* string?)
         :nums-kw #{:nums}
         :nums (s/* number?)))

(s/conform ::unnested [:names "a" "b" :nums 1 2 3])
;; {:names-kw :names, :names ["a" "b"], :nums-kw :nums, :nums [1 2 3]}


;; Entity Maps
;; Entity maps in spec are defined with keys:

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))

(s/def ::acctid integer?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email ::email-type)

(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                        :opt [::phone]))



(s/valid? ::person
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon@example.com"})
;; true

;; Fails required key check
(s/explain ::person
  {::first-name "Elon"})
;; val: {:speck.core/first-name "Elon"} fails predicate: [(contains? % :speck.core/last-name)
;; (contains? % :speck.core/email)]

;; Fails attribute conformance
(s/explain ::person
  {::first-name "Elon"
   ::last-name "Musk"
   ::email "n/a"})
;; At: [:speck.core/email] val: "n/a" fails spec: :speck.core/email predicate: (re-matches email-regex %)


;; a person map that uses unqualified keys but checks conformance against the namespaced specs we registered earlier:

(s/def :unq/person
  (s/keys :req-un [::first-name ::last-name ::email]
          :opt-un [::phone]))


