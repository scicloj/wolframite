(ns wolframite.wolfram-test
  "Tests of wolframite.wolfram, with the pre-loaded Wolfram symbols as clj vars"
  (:require  [clojure.test :refer [deftest testing is]]
             [wolframite.core :as wl]
             [wolframite.wolfram :as w]))

(deftest wolfram-ns-tests
  (wl/start)
  (testing "The pre-loaded symbols ns works"

    (testing "basics"
      (is w/Plus "The ns exists and contains expected vars for Wolfram fns")
      (is (>= w/*wolfram-version* 14) "We record Wolfram's version")
      (is (-> #'w/Plus meta :doc) "The vars have docstrings"))

    (testing "single level expression"
      (is (= 3 (wl/eval (w/Plus 1 2))))
      (testing "Clojure alias fns"
        (is (= 6
               (wl/eval (w/+ 1 2 3))
               (wl/eval (w/Plus 1 2 3)))
            "+ becomes Plus")
        (is (= 2
               (wl/eval (w/- 10 8))
               (wl/eval (w/Subtract 10 8)))
            "- becomes Subtract")
        (is (= -1
               (wl/eval '(- 1))
               (wl/eval (w/- 1))
               (wl/eval (w/Minus 1)))
            "(- num) becomes Minus[num] (notice that Subtract requires 2 args")
        (is (= 6
               (wl/eval (w/* 2 3))
               (wl/eval (w/Times 2 3)))
            "* becomes Times")
        (is (= 5
               (wl/eval (w// 10 2))
               (wl/eval (w/Divide 10 2)))
            "/ becomes Divide")
        #_...))

    (testing "nested expressions"
      (is (= 7 (wl/eval (w/Plus 1 (w/Times 2 (w/Plus 2 1)))))
          "Nested expressions work as well")
      (is (= [2 4]
             (wl/eval (w/Map (w/fn [x] (w/Plus x 1)) [1 3])))
          "Wolfram Lambdas work as intended")
      (is (= [11 31]
             (wl/eval (w/Map (w/fn [x] (w/Plus 1 (w/Times 10 x))) [1 3])))
          "Wolfram Lambdas work as intended (even w/ nested expressions)")
      (is (= [-42 420]
             (wl/eval '[(Minus 42) (Times 42 10)])
             (wl/eval [(w/Minus 42) (w/Times 42 10)]))
          "Expressions nested in a seq are correctly evaluated")
      (is (= (wl/eval '$VersionNumber)
             (wl/eval w/$VersionNumber))
          "Value vars (i.e. not fn calls) are also resolved correctly")
      (is (= (wl/eval '(Map (fn [x] (Plus x 1)) [$VersionNumber 10]))
             (wl/eval (w/Map (w/fn [x] (w/Plus x 1)) [w/$VersionNumber 10])))
          "Value vars (i.e. not fn calls) are also resolved correctly also inside lists")
      (is (= (wl/eval '[$VersionNumber 42])
             (wl/eval [w/$VersionNumber 42]))
          "Value vars (i.e. not fn calls) are also resolved correctly inside lists passed to eval"))))
