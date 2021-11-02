(ns demo
  (:require [clojuratica :as wl]
            [clojuratica.init :as init]))

;; * Clojure a 1min intro
;; ** Interactive developement
;; *** Evaluating forms from a text editor connected to a REPL

(+ 1 1)

;; ** Syntax a.k.a. data structures
;; *** Interesting scalar datatypes

;; - symbols: a b c d

;; unqoted
;; x

;; (def x "hello")

;; quoted

'a

;; - keywords

:hello

;; namespaced

:project.media/image


;; *** Interesting compund datatypes

;; vector

[1 3 :a :b 'foo 'bar]

;; hash map

{:a 23
 :b 73}

;; * Wolframite (nee Clojuratica) -- name is a WIP

;; Init

(require '[clojuratica.init :as init :refer [WL]]
         '[clojuratica :as wl])

(WL (Dot [1 2 3] [4 5 6]))

(WL '"{1 , 2, 3} . {4, 5, 6}")

(WL (Plus 23 '"{1 , 2, 3} . {4, 5, 6}"))

(wl/math-intern init/math-evaluate Plus)

(Plus [1 2] [3 4])

(def greetings
  (WL
   (Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings "Stephen")

;; threading fun

(-> "Ocean"
    Entity
    (GeoNearest Here)
    WL)

;; Wolfram Alpha

(WL (WolframAlpha "number of moons of Saturn" "Result"))

;; more examples

;; dealing with WL's syntactic sugar
(WL @(a b c d))

;; more interesting examples...

(WL (TextStructure "The cat sat on the mat."))

(WL (TextStructure "You can do so much with the Wolfram Language." "ConstituentGraphs"))
