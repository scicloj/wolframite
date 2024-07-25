(ns wolframite.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [wolframite.core :as wl]
            [wolframite.impl.wolfram-syms.wolfram-syms :as wolfram-syms]
            [wolframite.wolfram :as w]))

;; * Basic Sanity Checks

;; (deftest wl-evalute-initialized
;;   (testing (is wl/math-evaluate)))

(deftest conversions
  (wl/start)
  (is (= '(+ 1 1 1 1)
         (wl/->clj "Plus[1,1,1,1]")))
  (is (= '(- 5 4)
         (wl/->clj "Subtract[5, 4]")))
  (is (= '(- 5)
         (wl/->clj "Minus[5]"))))

(w/Subtract 5 4)

(deftest basic-math
  (wl/start)
  (is (= 2 (wl/eval '(Plus 1 1)))
      "A simple expression-as-symbol")
  (is (= 2 (wl/eval "Plus[1,1]"))
      "A simple expression-as-Wolfram-str")
  (is (= 2 (wl/eval (w/Plus 1 1)))
      ;; see wolfram-test for more complex tests of the Wolf vars
      "A simple expression-as-var"))

;; (deftest basic-string<->data-translation
;;   (testing "translating TO clj" (is (= '(GridGraph [5 5])
;;                                         (wl/wl->clj "GridGraph[{5, 5}]" wl/math-evaluate))))
;;   (testing "translating FROM clj" (is (= "GridGraph[{5, 5}]"
;;                                          (wl/clj->wl '(GridGraph [5 5]) {:kernel-link wl/kernel-link
;;                                                                          :output-fn str})))))

(deftest load-all-symbols-test
  (wl/start)
  (wolfram-syms/load-all-symbols wl/eval 'w2)
  (is (= 3
         (wl/eval (eval '(w2/Plus 1 2))))
      "An interned symbol can be used at a function position")
  (is (= 3
         (wl/eval (eval '(w2/Floor w2/Pi))))
      "An interned symbol can be used at a value position")
  (is (= -3
         (wl/eval (eval '(w2/Floor (w2/Plus 1 (w2/Minus w2/Pi)))))
         (wl/eval '(Floor (Plus 1 (Minus Pi)))))
      "Interned vars behave properly also when nested few levels deep")
  (is (= "x+y+z represents a sum of terms."
         (eval '(-> #'w2/Plus meta :doc)))
      "Interned vars have docstrings"))

(deftest aliases
  (wl/start)
  (is (= 4 (wl/! (w/Plus 1 1 2))))
  (is (= 4 (wl/! '(Plus 1 1 2)))))

(comment
  (clojure.test/run-tests 'wolframite.core-test))
