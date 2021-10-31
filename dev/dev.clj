(ns dev
  (:require [clojuratica.init :as init :refer [WL]]
            [clojuratica :as wl]
            [clojuratica.base.evaluate :as eval]))

(comment ;; Get Started!

  ;; * Init
  ;;

  (WL (Dot [1 2 3] [4 5 6]))
  ;; * Check
  ;; => 32
  (WL '"{1 , 2, 3} . {4, 5, 6}")
  ;; => 32

  ;; * Intern WL fns as macros
  (wl/math-intern init/math-evaluate Plus)

  (Plus [1 2] [3 4])

  ;; * Intern WL fns as fns (w/ aliasing)
  (wl/math-intern :as-function init/math-evaluate [PlusFn Plus])
  (map #(apply PlusFn %) [[1 2] [3 4 'a] [5 6]])

  ;; * Define fns through clj vars
  (def hello
    (WL
     (Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

  ;; * ISSUES
  (WL {a b c d}) ;; => [[a b] [c d]]

  (WL '"{1, Sqrt[4], 3+4}")

  #_:end-comment)
