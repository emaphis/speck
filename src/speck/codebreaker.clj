(ns codebreaker
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]))

;; Tutorial
;; http://blog.cognitect.com/blog/2016/10/5/interactive-development-with-clojurespec


(def peg? #{:y :g :r :c :w :b})

(s/def ::code (s/coll-of peg? :min-count 4 :max-count 6))

(s/def ::exact-matches nat-int?)
(s/def ::loose-matches nat-int?)

(s/def ::secret-and-guess (s/and (s/cat :secret ::code :guess ::code)
                                 (fn [{:keys [secret guess]}]
                                   (= (count secret) (count guess)))))

(s/fdef exact-matches
  :args ::secret-and-guess
  :ret nat-int?
  :fn (fn [{{secret :secret} :args ret :ret}]
        (<= 0 ret (count secret))))


(defn exact-matches [secret guess]
  (count (filter true? (map = secret guess))))

(s/exercise-fn `exact-matches)
(stest/check `exact-matches)


(s/fdef score
  :args ::secret-and-guess
  :ret (s/keys :req [::exact-matches ::loose-matches])
  :fn (fn [{{secret :secret} :args ret :ret}]
        (<= 0 (apply + (vals ret)) (count secret))))

;; now exercise the `:args` spec
;; (s/exercise (:args (s/get-spec `score)))

(defn score [secret guess]
  {::exact-matches (exact-matches secret guess)
   ::loose-matches 0})

(stest/instrument `exact-matches)
(s/exercise-fn `score)

(stest/check `score)
