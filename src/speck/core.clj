(ns speck.core
  (:require [clojure.spec :as s]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn add3 [num]
  (+ 3 num))

;; does data conform to a predicate
(s/conform even? 1000)


