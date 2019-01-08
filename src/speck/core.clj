(ns speck.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.repl :as r]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Following the tutorial at: <http://clojure.org/guides/spec>


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Predicates

;;; does data conform to a predicate
;; implicitly convers a predicate to a spec
(s/conform even? 1000)
;; => 1000

(s/conform even? 1001)
;; => :clojure.spec.alpha/invalid

;; similar but return a boolean
(s/valid? even? 10)
;; => true

(s/valid? even? 11)
;; =>  false


;;; some examples

(s/valid? nil? nil) ;; true
(s/valid? string? "abc") ;; true

(s/valid? #(> % 5) 10) ;; true
(s/valid? #(> % 5) 0) ;; false

(s/valid? inst? (java.util.Date.)) ;; true


;; Sets can also be used as predicates that match one or more literal values:
(s/valid? #{:club :diamond :heart :spade} :club) ;; true
(s/valid? #{:club :diamond :heart :spade} 42)  ;; false

(s/valid? #{42} 42)  ;; true
(s/valid? #{42,33} 33) ;; true
(s/valid? #{42,66} 33)  ;; false


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Registry

;; specs can be registered in a central namespaced registry using `def`

(s/def ::date  inst?) ;; :speck.core/date
(s/def ::suit #{:club :diamond :heart :spade}) ;; :speck.core/suit

;; using registered specs, can be used in place of an inplace spec definition
(s/valid? ::date (java.util.Date.)) ;; true
(s/valid? ::date 3) ;; false
(s/conform ::suit :club)  ;; :club
(s/conform ::suit :spoon) ;; :clojure.spec.alpha/invalid

;; registered specs can (and should) be used anywhere we compose specs

;; doc knows about registered specs
(r/doc ::date)
;; -------------------------
;; :speck.core/date
;; Spec
;;   inst?
(r/doc ::suit)
;; -------------------------
;; :speck.core/suit
;; Spec
;; #{:spade :heart :diamond :club}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Composing predicates with 'and', 'or'

(s/def ::big-even (s/and integer? even? #(> % 1000))) ;; :speck.core/big-even
(s/valid? ::big-even :foo) ;; false
(s/valid? ::big-even 10)   ;; false
(s/valid? ::big-even 10000) ;; true
(s/valid? ::big-even 10000.0) ;; false

;; use s/or to specify two alternatives
(s/def ::name-or-id (s/or :name string?
                          :id   int?))  ;; :speck.core/name-or-id
(s/valid? ::name-or-id "abd") ;; true
(s/valid? ::name-or-id 100) ;; true
(s/valid? ::name-or-id :foo) ;; false

;; or spec is the first case we’ve seen that involves a choice during validity checking
;; or is conformed, it returns a vector with the tag name and conformed value:
(s/conform ::name-or-id "abd") ;; [:name "abd"]
(s/conform ::name-or-id 100)  ;; [:id 100]
(s/conform ::name-or-id :foo) ;; :clojure.spec.alpha/invalid

;; To include `nil` as a valid value, use the provided function `nilable` to make a spec:
(s/valid? string? nil) ;; false
;; test of string or nil
(s/valid? (s/nilable string?) nil) ;; true


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Explain
;;  reports 'why' a value does not conform to a spec

(s/explain ::suit 42)
;; 42 - failed: #{:spade :heart :diamond :club} spec: :speck.core/suit

(s/explain ::big-even 5)
;; 5 - failed: even? spec: :speck.core/big-even

(s/explain ::name-or-id :foo)
;; :foo - failed: string? at: [:name] spec: :speck.core/name-or-id
;; :foo - failed: int? at: [:id] spec: :speck.core/name-or-id

;; on failure explain gives us:
;; val - the value in the user’s input that does not match
;; spec - the spec that was being evaluated
;; at - a path (a vector of keywords) indicating the location within the spec where
;;  the error occurred - the tags in the path correspond to any tagged part in a
;;  spec (the alternatives in an or or alt, the parts of a cat, the keys in a map,
;;  etc)
;; predicate - the actual predicate that was not satisfied by val

;; in - the key path through a nested data val to the failing value.
;; In this example, the top-level value is the one that is failing so this is
;; essentially an empty path and is omitted.


;; use explain-data to receive the errors as data, which can be attached to
;; an exception or acted upon for further analysis.
(s/explain-data ::name-or-id :foo)
;; #:clojure.spec.alpha{:problems ({:path [:name],
;;                                  :pred clojure.core/string?,
;;                                  :val :foo,
;;                                  :via [:speck.core/name-or-id],
;;                                  :in []}
;;                                 {:path [:id],
;;                                  :pred clojure.core/int?,
;;                                  :val :foo,
;;                                  :via [:speck.core/name-or-id],
;;                                  :in []}),
;;                      :spec :speck.core/name-or-id, :value :foo}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Entity Maps

;;  Entity maps in spec are defined with keys:

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))

(s/def ::acctid int?)
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
;; #:speck.core{:first-name "Elon"} - failed: (contains? % :speck.core/last-name)
;;   spec: :speck.core/person
;; #:speck.core{:first-name "Elon"} - failed: (contains? % :speck.core/email)
;;   spec: :speck.core/person

;; Fails attribute conformance
(s/explain ::person
           {::first-name "Elon"
            ::last-name "Musk"
            ::email "n/a"})
;; "n/a" - failed: (re-matches email-regex %) in: [:speck.core/email]
;;   at: [:speck.core/email] spec: :speck.core/email-type

;; a `person` map that uses unqualified keys but checks conformance against the namespaced specs we registered earlier:

(s/def :unq/person
  (s/keys :req-un [::first-name ::last-name ::email]
          :opt-un [::phone]))

(s/conform :unq/person
           {:first-name "Elon"
            :last-name "Musk"
            :email "elon@example.com"})
;; => {:first-name "Elon", :last-name "Musk", :email "elon@example.com"}

(s/explain :unq/person
           {:first-name "Elon"
            :last-name "Musk"
            :email "n/a"})
;; "n/a" - failed: (re-matches email-regex %) in: [:email] at: [:email]
;;   spec: :speck.core/email-type

(s/explain :unq/person
           {:first-name "Elon"})
;; {:first-name "Elon"} - failed: (contains? % :last-name) spec: :unq/person
;; {:first-name "Elon"} - failed: (contains? % :email) spec: :unq/person

;; Unqualified keys can also be used to validate record attributes:
(defrecord Person [first-name last-name email phone])

(s/explain :unq/person
           (->Person "Elon" nil nil nil))
;; nil - failed: string? in: [:last-name] at: [:last-name] spec: :speck.core/last-name
;; nil - failed: string? in: [:email] at: [:email] spec: :speck.core/email-type

(s/conform :unq/person
           (->Person "Elon" "Musk" "elon@example.com" nil))
;; #speck.core.Person{:first-name "Elon", :last-name "Musk",
;;                    :email "elon@example.com", :phone nil}


;; One common occurrence in Clojure is the use of "keyword args" where
;; keyword keys and values are passed in a sequential data structure as options.
;; Spec provides special support for this pattern with the regex op keys*.
;; keys* has the same syntax and semantics as keys but can be embedded inside a
;; sequential regex structure.

(s/def ::port number?)
(s/def ::host string?)
(s/def ::id keyword?)
(s/def ::server (s/keys* :req [::id ::host] :opt [::port]))
(s/conform ::server [::id :s1 ::host "example.com" ::port 5555])
;; #:speck.core{:id :s1, :host "example.com", :port 5555}


;; Entity maps can be declared in parts, then s/merge can be used to combine them

(s/def :animal/kind string?)
(s/def :animal/says string?)
(s/def :animal/common (s/keys :req [:animal/kind :animal/says]))
(s/def :dog/tail? boolean?)
(s/def :dog/breed string?)
(s/def :animal/dog (s/merge :animal/common
                            (s/keys :req [:dog/tail? :dog/breed])))

(s/valid? :animal/dog
          {:animal/kind "dog"
           :animal/says "woof"
           :dog/tail? true
           :dog/breed "retriever"})
;;=> true


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Mutli-spec

;; specs for entities for event objects that share som common fields but
;; also have type-specifit shapes.

(s/def :event/type keyword?)
(s/def :event/timestamp int?)
(s/def :search/url string?)
(s/def :error/message string?)
(s/def :error/code int?)

;; We then neeed a mutimethod that defines a dispatch for choosing the  selector:

(defmulti event-type :event/type)
(defmethod event-type :event/search [_]
  (s/keys :req [:event/type :event/timestamp :search/url]))
(defmethod event-type :event/error [_]
  (s/keys :req [:event/type :event/timestamp :error/message :error/code]))

;; now our mult-spec:
(s/def :event/event (s/multi-spec event-type :event/type))

(s/valid? :event/event
          {:event/type :event/search
           :event/timestamp 1463970123000
           :search/url "http://clojure.org"}) ;; true


(s/valid? :event/event
          {:event/type :event/error
           :event/timestamp 1463970123000
           :error/message "Invalid host"
           :error/code 500}) ;; true

(s/explain :event/event
           {:event/type :event/restart})
;; #:event{:type :event/restart} - failed: no method at: [:event/restart]
;;   spec: :event/event

(s/explain :event/event
           {:event/type :event/search
            :search/url 200})
;; 200 - failed: string? in: [:search/url]
;;   at: [:event/search :search/url] spec: :search/url
;; {:event/type :event/search, :search/url 200} - failed: (contains? % :event/timestamp)
;;   at: [:event/search] spec: :event/event


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Collections

;; For the special case of a homogenous collection of arbitrary size,
;; you can use coll-of
;; coll-of must be provided a seed collection to use when conforming elements
;; - something like [], (), or (sorted-set)

(s/conform (s/coll-of keyword?) [:a :b :c])
;; [:a :b :c]

(s/conform (s/coll-of keyword?) #{:a :b :c})
;;#{:c :b :a}

(s/conform (s/coll-of number?) #{5 10 2})
;; #{2 5 10}

;; coll-of can be passed a number of optional keyword args:
;; :kind - a spec that the tested collection mus satisfy
;; :count - expected cound
;; :min-count, :max-count - check thte range of collections count
;; :distinct - all elements distinct?
;; :into  - one fo [],(),{}, #{}

;; examples:
(s/def ::vnum3 (s/coll-of number? :kind vector? :count 3 :distinct true :into #{}))
(s/conform ::vnum3 [1 2 3]) ;; #{1 3 2}

(s/explain ::vnum3 #{1 2 3})
;; #{1 3 2} - failed: vector? spec: :speck.core/vnum3

(s/explain ::vnum3 [1 1 1])
;; [1 1 1] - failed: distinct? spec: :speck.core/vnum3

(s/explain ::vnum3 [1 2 :a])
;; :a - failed: number? in: [2] spec: :speck.core/vnum3

;; another case is a fixed-size positional collection with fields of known type at
;; different positions. For that we have tuple

(s/def ::point (s/tuple double? double? double?))
(s/conform ::point [1.5 2.5 -0.5])
;; [1.5 2.5 -0.5]

;; other choices for point

;; regular expression
(s/def ::point-2 (s/cat :x double? :y double? :z double?))
(s/conform ::point-2 [1.5 2.5 -0.5])
;; {:x 1.5, :y 2.5, :z -0.5}

;; collection
(s/def ::point-3 (s/coll-of double?))
(s/conform ::point-3 [1.5 2.5 -0.5])
;; [1.5 2.5 -0.5]


;; map-of for maps with homogenous key and value predicates.

(s/def ::scores (s/map-of string? int?))
(s/conform ::scores {"Sally" 1000, "Joe" 500})
;; {"Sally" 1000, "Joe" 500}



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sequences
;; uses  regular expression operators to describe the structure of a sequential data value:

;; cat - concatenation of predicates/patterns
;; alt - choice among alternative predicates/patterns
;; * - 0 or more of a predicate pattern
;; + - 1 or more of a predicate pattern
;; ? - 0 or 1 of a predicate pattern

;; an ingredient represented by a vector containing a quantity (number) and a unit (keyword)
(s/def ::ingedient (s/cat :quantity number? :unit keyword?))
(s/conform ::ingedient [2 :teaspoon])
;; {:quantity 2, :unit :teaspoon}

;; use explain to examine non-conforming data.

;; pass string for unit instead of keyword
(s/explain ::ingedient [11 "peaches"])
;; "peaches" - failed: keyword? in: [1] at: [:unit] spec: :speck.core/ingedient

(s/explain ::ingedient [2])
;; () - failed: Insufficient input at: [:unit] spec: :speck.core/ingedient

;;; various occurence operators *, +, and ?:

(s/def ::seq-of-keywords (s/* keyword?))

(s/conform ::seq-of-keywords [:a :b :c])
;; [:a :b :c]

(s/explain ::seq-of-keywords [10 20])
;; 10 - failed: keyword? in: [0] spec: :speck.core/seq-of-keywords

(s/def ::odds-then-maybe-even (s/cat :odds (s/+ odd?)
                                     :even (s/? even?)))
(s/conform ::odds-then-maybe-even [1 3 5 100])
;; {:odds [1 3 5], :even 100}

(s/conform ::odds-then-maybe-even [1])
;; {:odds [1]}

(s/explain ::odds-then-maybe-even [100])
;; 100 - failed: odd? in: [0] at: [:odds] spec: :speck.core/odds-then-maybe-even

;; opts are alternating keyword and booleans
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
;; 1 - failed: string? in: [0] spec: :speck.core/even-strings

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Using spec for validation
;;   spec can be used for runtime data validation.

(defn person-name
  [person]
  {:pre  [(s/valid? ::person person)]
   :post [(s/valid? string? %)]}
  (str (::first-name person) " " (::last-name person)))

(person-name 42)
;; Java exception:
;; Assert failed: (s/valid? :speck.core/person person)

(person-name {::first-name "Elon" ::last-name "Musk" ::email "elon@example.com"})
;; "Elon Musk"


;; we can also use assert to check conformance to a spec

(defn person-name
  [person]
  (let [p (s/assert ::person person)]
    (str (::first-name p) " " (::last-name p))))

(s/check-asserts true)
(person-name 100)

;;1. Unhandled clojure.lang.ExceptionInfo
;;   Spec assertion failed val: 100 fails predicate: map?
;;   :clojure.spec/failure :assertion-failed
;;
;;  #:clojure.spec{:problems [{:path [], :pred map?, :val 100, :via [], :in []}], :failure :assertion-failed}


;; call conform and use the return value to destructure the input.

(defn- set-config [prop val]
  ;; dummy fn
  (println "set" prop val))


(defn configure [input]
  (let [parsed (s/conform ::config input)]
    (if (= parsed ::s/invalid)
      (throw (ex-info "Invalid input" (s/explain-data ::config input)))
      (for [{prop :prop [_ val] :val} parsed]
        (set-config (subs prop 1) val)))))

(configure ["-server" "foo" "-verbose" true "-user" "joe"])

;; set server foo
;; set verbose true
;; set user joe

(s/conform ::config ["-server" "foo" "-verbose" true "-user" "joe"])

;;[{:prop "-server", :val [:s "foo"]}
;; {:prop "-verbose", :val [:b true]}
;; {:prop "-user", :val [:s "joe"]}]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Spec'ing functions

;; using fdef to define specs for a function (arguments, return value, and relationship
;; between the two.

(defn ranged-rand
  "Returns random integer in range start <= rand < end"
  [start end]
  (+ start (long (rand (- end start)))))

;; now we give this spec:
(s/fdef ranged-rand
  :args (s/and (s/cat :start int? :end int?)
               #(< (:start %) (:end %)))
  :ret int?
  :fn (s/and #(>= (:ret %) (-> % :args :start))
             #(< (:ret %) (-> % :args :end))))

(r/doc ranged-rand)
;; -------------------------
;; speck.core/ranged-rand
;; ([start end])
;;   Returns random integer in range start <= rand < end
;; Spec
;;   args: (and (cat :start int? :end int?) (< (:start %) (:end %)))
;;   ret: int?
;;   fn: (and (>= (:ret %) (-> % :args :start)) (< (:ret %) (-> % :args :end)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Higher order functions

;; Higher order functions are common in Clojure and spec provides fspec to
;; support spec’ing them.

;;  returns a function that adds x. 
(defn adder [x] #(+ x %))

(def add-3 (adder 3)) ; use

(add-3 4) ;; 7

(s/fdef adder
  :args (s/cat :x number?)
  :ret  (s/fspec :args (s/cat :y number?)
                 :ret number?)  ; returns a function
  :fn #(= (-> :args :x) ((:ret %) 0)))
;; :fn spec can state a general property that relates the :args (where we know `x`)
;;  and the result we get from invoking the function returned from `adder`, namely
;;  that adding 0 to it should return `x`.


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Macros

;; Spec out the clojure.core/declare macro

(s/fdef clojure.core/declare
  :args (s/cat :names (s/* simple-symbol?))
  :ret  any?)

;; (declare 100)

;; Java exception
;;   Syntax error macroexpanding clojure.core/declare at (core.clj:605:1).
;;   100 - failed: simple-symbol? at: [:names]


;; 2. Unhandled clojure.lang.Compiler$CompilerException
;; Error compiling c:/src/Clojure/speck/src/speck/core.clj at (605:4)
;; #:clojure.error{:phase :macro-syntax-check,
;;                 :line 605,
;;                 :column 4,
;;                 :source "c:/src/Clojure/speck/src/speck/core.clj",
;;                 :symbol clojure.core/declare}

;; 1. Caused by clojure.lang.ExceptionInfo
;; Call to clojure.core/declare did not conform to spec.
;; #:clojure.spec.alpha{:problems
;;                      [{:path [:names],
;;                        :pred clojure.core/simple-symbol?,
;;                        :val 100,
;;                        :via [],
;;                        :in [0]}],
;;                      :spec
;;                      #object[clojure.spec.alpha$regex ... 
;;                      :value (100),
;;                      :args (100)}

;; Because macros are always checked during macro expansion, you do not need
;; to call instrument for macro specs.


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Example - A game of cards

(def suit? #{:club :diamond :heart :spade})
(def rank? (into #{:jack :queen :king :ace} (range 2 11)))
(def deck (for [suit suit? rank rank?] [rank suit]))

(s/def ::card (s/tuple rank? suit?))
(s/def ::hand (s/* ::card))

(s/def ::name string?)
(s/def ::score int?)
(s/def ::player (s/keys :req [::name ::score ::hand]))

(s/def ::players  (s/* ::player))
(s/def ::deck (s/* ::card))
(s/def ::game (s/keys :req [::players ::deck]))

;; now validate a piece of this data against the schema:
(def kenny
  {::name "Kenny Rogers"
   ::score 100
   ::hand []})

(s/valid? ::player kenny)
;; true

;; now some bad data:
(s/explain ::game
           {::deck deck 
            ::players [{::name "Kenny Rogers"
                        ::score 100
                        ::hand [[2 :banana]]}]})
;; :banana - failed: suit? in: [:speck.core/players 0 :speck.core/hand 0 1]
;; at: [:speck.core/players :speck.core/hand 1] spec: :speck.core/card

(defn total-cards [{:keys [::deck ::players] :as game}]
  (apply + (count deck)
         (map #(-> % ::hand count) players)))

(defn deal [game] game ,,,)

(s/fdef deal
  :ar gs (s/cat :game ::game)
  :ret  ::game
  :fn   #(= (total-cards (-> % :args :game))
            (total-cards (-> % :ret))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generators

;; add this to project.clj to use spec for testing
;; :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}}


;; require this to run spec generators durring testing
(require '[clojure.spec.gen.alpha :as gen])

(gen/generate (s/gen int?)) ;; -115723691

(gen/generate (s/gen nil?)) ;; nil

(gen/sample (s/gen string?))
;; ("" "n" "Y" "U" "Z" "T5Mj" "LNtD" "Lk" "76Xl" "7f31u")
;; ("" "" "u" "0" "" "H7H4" "6f3ZS" "" "wkzBFz7s" "54")
;; ("" "" "" "6" "W" "" "zops3" "X0" "EVI" "LJwRKjN7o")

(gen/sample (s/gen #{:club :diamond :heart :spade}))
;; (:club :club :spade :heart :diamond :diamond :heart :diamond :heart :spade)

(gen/sample (s/gen (s/cat :k keyword? :ns (s/+ number?))))

;;((:K -1)
;; (:!/+z -1)
;; (:o7/i*3 0.0 1.0 Infinity)
;; (:UI.*a/EA 0.5 3.5)
;; (:q02 -4 3.0 1.0 0)
;; (:uf.z?0.?/F*I86t 1 -0.5 3)
;; (:uZVp._*+q.T-4/+8_B -1.0 3.0 -0.8125 -3 15)
;; (:*6.-.N?6?+-E?.+.WK?.*wUW.q!Y/!4*?4r2j -1.0625 33)
;; (:n7x-?!.c.E4k9g7+a.wzBPFG_6?.Z!!WW/L -1 -0.5 -1 0 -3.0 0 -1.0)
;; (:-+E.b.-nS-bEI.*6kcZ6/f*N2mL*T -4 2))

;; WOWSERS!

;; generate a random player in out card game
(gen/generate (s/gen ::player))

;;#:speck.core{:name "cet8eRJQ2ybg4s7",
;;             :score 9436,
;;             :hand ([3 :spade]
;;                    [8 :heart]
;;                    [3 :club]
;;                    [9 :heart]
;;                    [:queen :heart]
;;                    [:jack :heart]
;;                    [:king :club]
;;                    [5 :club]
;;                    [5 :spade]
;;                    [8 :spade])}


;; generating a whole game
;; (gen/generate (s/gen ::game))
;; ... really long game ...


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Exercise
;;   returns pairs of generated and conformed values for a spec. `exercise` by
;;   default produces 10 samples (like `sample`) but you can pass both functions
;;;  a number indicating the number of samples to produce.

(s/exercise (s/cat :k keyword? :ns (s/+ number?)) 5)

;;([(:p -1) {:k :p, :ns [-1]}]
;; [(:_e/Ff -0.0 -2.0) {:k :_e/Ff, :ns [-0.0 -2.0]}]
;; [(:f48.j/! 1 -2) {:k :f48.j/!, :ns [1 -2]}]
;; [(:_9k.p?o/Q 1.0 -1) {:k :_9k.p?o/Q, :ns [1.0 -1]}]
;; [(:Un!.d?!/jDa*4 2 1 3.0 0 -6) {:k :Un!.d?!/jDa*4, :ns [2 1 3.0 0 -6]}])

(s/exercise (s/or :k keyword? :s string? :n number?) 5)

;;(["" [:s ""]]
;; [-1 [:n -1]]
;; [0.5 [:n 0.5]]
;; ["145" [:s "145"]]
;; [:*/lPg8! [:k :*/lPg8!]])


;; exercise-fn generates sample args for speced functions

(s/exercise-fn `ranged-rand)
;;([(-1 0) -1]
;; [(-4 2) 1]
;; [(-6 0) -1]
;; [(0 2) 1]
;; [(-2 6) 4]
;; [(-2 3) 1]
;; [(-2 1) 0]
;; [(-3 17) 3]
;; [(0 13) 4]
;; [(-1 1) -1])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Using s/and Generators

;; All of the generators we’ve seen worked fine but there are a number of cases
;;  where they will need some additional help.
;; One common case is when the predicate implicitly presumes values of a particular
;;  type but the spec does not specify them:

(gen/generate (s/gen even?))

;; Execution error (ExceptionInfo) at speck.core/eval7718 (form-init16562912574267509220.clj:791).
;; Unable to construct gen at: [] for: clojure.core$even_QMARK_@476bd286

;; we can use 'and' to create generator for primative predicates that dont have generators

(gen/generate (s/gen (s/and int? even?)))   ; even? acts as a filter
;; 4
;; -212800

;; positive numbers of 3:
(defn divisible-by [n] #(zero? (mod % n)))

(gen/sample (s/gen (s/and int?
                          #(> % 0)
                          (divisible-by 3))))

;; (3 18 9 3 3 2340 6 21 321 3)


;; consider trying to generate strings that happen to contain the world "hello"
;; which produces too few values.

;; hello, are you the one I'm looking for?
(gen/sample (s/gen (s/and string? #(clojure.string/includes? % "hello"))))

;;1. Unhandled clojure.lang.ExceptionInfo
;;   Couldn't satisfy such-that predicate after 100 tries.
;;   {}

;; I guess a million monkeys can't type Shakespeare after all.


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Custom Generators



;; There are three ways to build up custom generators - in decreasing order
;;  of preference:
;; *Let spec create a generator based on a predicate/spec
;; *Create your own generator from the tools in clojure.spec.gen.alpha
;; *Use test.check or other test.check compatible libraries (like test.chuck)


;; consider a spec with a predicate to specify keywords from a particular namespace:

(s/def ::kws (s/and keyword? #(= (namespace %) "speck.core")))
(s/valid? ::kws :speck.core/name) ;; true
(gen/sample (s/gen ::kws))
;; clojure.lang.ExceptionInfo - Couldn't satisfy such-that predicate after 100 tries.

;; A set is a valid predicate spec so we can create one and ask for it’s generator:
(def kw-gen (s/gen #{:speck.core/name :speck.core/occupation :speck.core/id}))
(gen/sample kw-gen 5)

;; (:speck.core/name
;;  :speck.core/name
;;  :speck.core/occupation
;;  :speck.core/name
;;  :speck.core/id)

;; To redefine our spec using this custom generator, use with-gen which takes a spec
;; and a replacement generator:
(s/def ::kws (s/with-gen (s/and keyword? #(= (namespace %) "speck.core"))
               #(s/gen #{:speck.core/name :speck.core/hand :speck.core/id})))

(s/valid? ::kws :speck.core/name) ;; true
(gen/sample (s/gen ::kws))
;; (:speck.core/name :speck.core/id :speck.core/name ...)

;; In this case we want our keyword to have open names but fixed namespaces.
;; There are many ways to accomplish this but one of the simplest is to use
;; `fmap` to build up a keyword based on generated strings: 

(def kw-gen-2 (gen/fmap #(keyword "speck.core" %) (gen/string-alphanumeric)))
(gen/sample kw-gen-2 5)
;; (:speck.core/ :speck.core/8 :speck.core/ :speck.core/u5 :speck.core/Yn)

;; some of these are too simple

;; filter with such-that
(def kw-gen-3 (gen/fmap #(keyword "speck.core" %)
                        (gen/such-that #(not= % "")
                                       (gen/string-alphanumeric))))

(gen/sample kw-gen-3 5)
;; (:speck.core/t :speck.core/8 :speck.core/P :speck.core/B :speck.core/vfUS)

;; Now return to the hello example
(s/def ::hello
  (s/with-gen #(clojure.string/includes? % "hello")
    #(gen/fmap (fn [[s1 s2]] (str s1 "hello" s2))
               (gen/tuple (gen/string-alphanumeric (gen/string-alphanumeric))))))

(gen/sample (s/gen ::hello))
;; ("hello" "hello" "9Phello" "mr0hello" "hello" "1Vkighello" "QK74Whello" "31UWhello" "BhYhello" "V6M9072zFhello")


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Range Specs and Generators

;; for a range of int values (say in a bowling roll) use int-in

(s/def ::roll (s/int-in 0 11))
(gen/sample (s/gen ::roll))
;; (0 0 1 0 2 2 2 1 1 7)

;; inst-in for a range of instants:
(s/def ::the-aughts (s/inst-in #inst "2000" #inst "2010"))
(drop 50 (gen/sample (s/gen ::the-aughts) 55))

;;(#inst "2008-12-25T01:29:10.730-00:00"
;; #inst "2000-01-01T00:00:00.007-00:00"
;; #inst "2000-01-01T00:17:05.685-00:00"
;; #inst "2007-02-01T16:08:08.246-00:00"
;; #inst "2000-01-01T00:00:00.109-00:00")

;; double-in supports double ranges with options for checking NaN, Infinity, -Infinity
(s/def ::dubs (s/double-in :min -100.0 :NaN? false :infinite? false))
(s/valid? ::dubs 2.9) ;;true

(s/valid? ::dubs Double/POSITIVE_INFINITY) ;; false

(gen/sample (s/gen ::dubs))
;; (-1.0 -2.0 0.625 0.5 0.0 1.0 3.25 -0.75 -0.5 -4.5)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Instrumentation and Testing

;; include development and testing functionality:
(require '[clojure.spec.test.alpha :as stest])

;;; Instrumentation

(stest/instrument `ranged-rand)

;; now if args don't meet the spec then you have and error:
(ranged-rand 8 5)
;; Execution error - invalid arguments to speck.core/ranged-rand at (form-init16562912574267509220.clj:931).
;; {:start 8, :end 5} - failed: (< (:start %) (:end %))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Testing

(require '[clojure.spec.test.alpha :as stest])

(stest/check `ranged-rand)
;; ({:spec #object[clojure.spec.alpha$fspec_impl$reify__2524 0x45d0ed9c ...
;;   :clojure.spec.test.check/ret {:result true, :num-tests 1000, :seed 1546904366232},
;;   :sym speck.core/ranged-rand})

;; *** A keen observer will notice that ranged-rand contains a subtle bug.
;; If the difference between start and end is very large (larger than is
;; representable by Long/MAX_VALUE), then ranged-rand will produce an
;; IntegerOverflowException. If you run check several times you will eventually
;; cause this case to occur.


;; an error: start and end are swapped
(defn ranged-rand   ;; BROKEN!
  "Returns random int in rane start <= rand end."
  [start end]
  (+ start (long (rand (- start end)))))

(stest/abbrev-result (first (stest/check `ranged-rand)))
;; {:spec (fspec
;;         :args (and (cat :start int? :end int?) (fn* [p1__7699#] (< (:start p1__7699#) (:end p1__7699#))))
;;         :ret int?
;;         :fn (and
;;              (fn* [p1__7700#] (>= (:ret p1__7700#) (-> p1__7700# :args :start)))
;;              (fn* [p1__7701#] (< (:ret p1__7701#) (-> p1__7701# :args :end))))),
;;  :sym speck.core/ranged-rand,
;;  :failure {:clojure.spec.alpha/problems [{:path [:fn],
;;                                           :pred (clojure.core/fn [%]
;;                                                   (clojure.core/>= (:ret %)
;;                                                                    (clojure.core/-> % :args :start))),
;;                                           :val {:args {:start -1, :end 1}, :ret -2},
;;                                           :via [],
;;                                           :in []}],
;;            :clojure.spec.alpha/spec #object[clojure.spec.alpha$and_spec_imp ...
;;            :clojure.spec.alpha/value {:args {:start -1, :end 1}, :ret -2},
;;            :clojure.spec.test.alpha/args (-1 1),
;;            :clojure.spec.test.alpha/val {:args {:start -1, :end 1}, :ret -2},
;;            :clojure.spec.alpha/failure
;;            :check-failed}}

;; you can check all of the spec'ed funtions in a namespace with enumerate-namespace
(-> (stest/enumerate-namespace 'user) stest/check)  ;; ()


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Combining check and instrument

;; Consider the case where we have a low-level function that invokes a remote
;;  service and a higher-level function that calls it.

;; code under test

(defn invoke-service [service request]
  ;; invokes remote service
  )

(defn run-query [service query]
  (let [{::keys [result error]} (invoke-service service {::query query})]
    (or result error)))

;; spec these functions using:
(s/def ::query string?)
(s/def ::request (s/keys :req [::query]))
(s/def ::result (s/coll-of string? :gen-max 3))
(s/def ::error int?)
(s/def ::response (s/or :ok (s/keys :req [::result])
                        :err (s/keys :req [::error])))

(s/fdef invoke-service
  :args (s/cat :service any? :request ::request)
  :ret ::response)

(s/fdef run-query
  :args (s/cat :service any? :query string?)
  :ret (s/or :ok ::result :err ::error))

;; now test the behavior of run-query while subbing out invoke-service

(stest/instrument `invoke-service {:stub #{`invoke-service}})
;; [speck.core/invoke-service]
(invoke-service nil {::query "test"})
;; => #:speck.core{:error -163902}
;; => #:speck.core{:error 8305500}
;; => #:speck.core{:result ["91sf28jf0EQx5FKdcgRBF7"]}
;; => #:speck.core{:result []}

(stest/summarize-results (stest/check `run-query))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Wrapping Up
