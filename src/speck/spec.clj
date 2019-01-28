(ns speck.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]
            [clojure.repl :as r]))

;;; Ingredient

(defn scale-ingredient [ingredient factor]
  (update ingredient :quantity * factor))

(scale-ingredient {:item "flour" :quantity 10 :units "quart"} 1/2)
;; => {:item "flour", :quantity 5N, :units "quart"}


;; Specs describing an ingredient.
(s/def ::ingredient (s/keys :req [::name ::quantity ::units]))

(s/def ::name     string?)
(s/def ::quantity number?)
(s/def ::units     string?)

(s/explain ::ingredient {:name "flour" :quantity 10 :units "quart"})

;; function spec for scale-ingredient
(s/fdef scale-ingredient
  :args (s/cat :ingredient ::ingredient :factor number?)
  :ret ::ingredient)

(stest/instrument `scale-ingredient)


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

;;; sequences with structure
;;  s/cat

(s/def ::cat-example (s/cat :s string? :i int?))

(s/valid? ::cat-example ["abc" 100]) ;; => true

(s/conform ::cat-example ["abc" 100])
;; => {:s "abc", :i 100}

;; s/alt for indicating alternatives
(s/def ::alt-example (s/alt :i int? :k keyword?))

(s/valid? ::alt-example [100])   ;; => true
(s/valid? ::alt-example [:foo])  ;; => true
(s/conform ::alt-example [:foo]) ;; => [:k :foo]

;;; Repetition operators
;;  s/? 0 or 1
;;  s/* 0 or more
;;  s/+ 1 or more

;; a collection that contains one or more odd numbers and an optional trailing
;; even number
(s/def ::oe (s/cat :odds (s/+ odd?) :even (s/? even?)))

(s/conform ::oe [1 3 5 100])
;; => {:odds [1 3 5], :even 100}

;; factored regex operators
(s/def ::odds (s/+ odd?))
(s/def ::optional-even (s/? even?))
(s/def ::oe2 (s/cat :odds ::odds :even ::optional-even))

(s/conform ::oe2 [1 3 5 7 8])
;; => {:odds [1 3 5 7], :even 8}

;;; Variable argument lists
;;  any? - any type
;;  s/* repetition

(s/def ::println-args (s/* any?))

(r/doc clojure.set/intersection)
;; clojure.set/intersection
;;   ([s1] [s1 s2] [s1 s2 & sets])
;;   Return a set that is the intersection of the input sets

(s/def ::intersection-args
  (s/cat :s1 set?
         :sets (s/* set?)))

(s/conform ::intersection-args [#{1 2} #{ 2 3} #{ 3 4}])
;; => {:s1 #{1 2}, :sets [#{3 2} #{4 3}]}

;; or ...
(s/def ::intersection-args-2 (s/+ set?))

(s/conform ::intersection-args-2 [#{1 2} #{ 2 3} #{ 3 4}])
;; => [#{1 2} #{3 2} #{4 3}]

;;; optional keyword arguments

;; atom -> (atom x & options)
;;   options -> :meta :validator

(s/def ::meta map?)
(s/def ::validator ifn?)
(s/def ::atom-args
  (s/cat :x any? :options (s/keys* :opt-un [::meta ::validator])))

(s/conform ::atom-args [100 :meta {:foo 1} :validator int?])
;; {:x 100,
;;  :options {:meta {:foo 1},
;;            :validator #function[clojure.core/int?]}}


;;; Multi-arity argument lists

(r/doc repeat)
;; clojure.core/repeat
;;  ([x] [n x])
;;  Returns a lazy (infinite!, or length n if supplied) sequence of xs.

(s/def ::repeat-args
  (s/cat :n (s/? int?) :x any?))

(s/conform ::repeat-args [100 "foo"])
;; => {:n 100, :x "foo"}
(s/conform ::repeat-args ["foo"])
;; => {:x "foo"}


;;; Specifying functions

(r/doc clojure.core/rand)
;; clojure.core/rand
;; ([] [n])
;;   Returns a random floating point number between 0 (inclusive) and
;;   n (default 1) (exclusive).

(s/def ::rand-args (s/cat :n (s/? number?)))
(s/def ::rand-ret double?)

;; the returned random number must be >=0 and <=n.
(s/def ::rand-fn
  (fn[{:keys [args ret]}]
    (let [n (or (:n args) 1)]
      (cond (zero? n) (zero? ret)
            (pos? n) (and (>= ret 0) (< ret n))
            (neg? n) (and (<= ret 0) (> ret n))))))

(s/fdef clojure.core/rand
  :args ::rand-args
  :ret  ::rand-ret
  :fn   ::rand-fn)

;;; Anonymous functions

;; function takes a predicate function
(defn opposite [pred]
  (comp not pred))

(s/def ::pred
  (s/fspec :args (s/cat :x any?)
           :ret boolean?))

(s/fdef opposite
  :args (s/cat :pred ::pred)
  :ret ::pred)


;;; Instrumenting functions

(stest/instrument 'clojure.core/rand)

;; Instrumenting all symbols in a namespace
#_(stest/instrument (stest/enumerate-namespace 'clojure.core))

;; invalid call to `rand`
;; (rand :boom)
;; 1. Unhandled clojure.lang.ExceptionInfo
;; Spec assertion failed.

;; Spec: #object[ "clojure.spec.alpha$regex_spec_impl$reify__2509@2b946fc5"]
;; Value: (:boom)

;; Problems:

;; val: :boom
;; in: [0]
;; failed: number?
;; spec: :speck.spec/rand-args
;; at: [:n]


;;; Generative function testing

;;; testing `symbol`
;; clojure.core/symbol
;; ([name] [ns name])
;; Returns a Symbol with the given namespace and name. Arity-1 works
;; on strings, keywords, and vars.
(s/fdef clojure.core/symbol
  :args (s/cat :ns (s/? string?) :name string?)
  :ret symbol?
  :fn (fn [{:keys [args ret]}]
        (and (= (name ret) (:name args))
             (= (namespace ret) (:ns args)))))

(stest/check 'clojure.core/symbol)
;; ({:spec #object[clojure.spec.alpha$ ...
;;   :clojure.spec.test.check/ret {:result true,
;;                                 :num-tests 1000,
;;                                 :seed 1548649018066},
;;   :sym clojure.core/symbol})

;;; Generating examples

(s/exercise (s/cat :ns (s/? string?) :name string?))
;; ([("") {:name ""}]
;;  [("" "9") {:ns "", :name "9"}]
;;  [("h") {:name "h"}]
;;  [("N") {:name "N"}]
;;  [("s" "MX") {:ns "s", :name "MX"}]
;;  [("0L0") {:name "0L0"}]
;;  [("" "") {:ns "", :name ""}]
;;  [("1IDy" "3l") {:ns "1IDy", :name "3l"}]d
;;  [("U0AoU" "CRm") {:ns "U0AoU", :name "CRm"}]
;;  [("n25tN") {:name "n25tN"}])

;; Sometimes spec doesn't produce any data.

;; Combining generators with s/and

(defn big? [x] (> x 100))
(s/def ::big-odd (s/and odd? big?))

;; (s/exercise ::big-odd) fails

;; we need a predicate that has a generator: int?
(s/def ::big-odd-int (s/and int? odd? big?))

(s/exercise ::big-odd-int)
;; ([445 445]
;;  [717 717]
;;  [6681 6681]
;;  [181 181]
;;  [819 819]
;;  [390563 390563]
;;  [571 571]
;;  [375 375]
;;  [31219 31219]
;;  [181 181])

;;; Creating custom generator
;;  `s/coll-of` `s/map-of`  `s/every` `s/every-kv` `s/keys` accept
;;  custom generators
;;  You can override default generators to any spec with `s/with-gen`
;;  You can override a generator by name or path with `w/exercise`
;;   `stest/instrument` and `stest/check`

;; A replacement generator ::/color
(s/def ::color-red
  (s/with-gen ::color #(s/gen #{:red})))

(s/exercise ::color-red)
;; => ([:red :red] [:red :red] [:red :red] [:red :red] [:red :red]
;; [:red :red] [:red :red] [:red :red] [:red :red] [:red :red])

;; SKU example
(s/def ::sku
  (s/with-gen (s/and string? #(str/starts-with? % "SKU-"))
    (fn [] (gen/fmap #(str "SKU-" %) (s/gen string?)))))

(s/exercise ::sku)
;; (["SKU-" "SKU-"]
;;  ["SKU-F" "SKU-F"]
;;  ["SKU-93" "SKU-93"]
;;  ["SKU-c" "SKU-c"]
;;  ["SKU-X5X" "SKU-X5X"]
;;  ["SKU-By" "SKU-By"]
;;  ["SKU-ZM1S3D" "SKU-ZM1S3D"]
;;  ["SKU-tX" "SKU-tX"]
;;  ["SKU-8C3QV" "SKU-8C3QV"]
;;  ["SKU-wJ7" "SKU-wJ7"])
