(ns wolframite.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [wolframite.core :as wl]
            [wolframite.impl.wolfram-syms.wolfram-syms :as wolfram-syms]
            [wolframite.wolfram :as w])
  (:import (clojure.lang ExceptionInfo)))

;; * Basic Sanity Checks

;; (deftest wl-evalute-initialized
;;   (testing (is wl/math-evaluate)))

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

(deftest package-test
  (wl/start)
  (is (= nil
         (wl/<<! "resources/WolframPackageDemo.wl")))
  (is (= nil
         (wl/load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo")))
  (is (= nil
         (wl/load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo" 'wd)))

  (is (= "Used for testing Wolframite."
         (wl/eval  (w/Information wd/tryIt "Usage"))))
  (is (= 1000
         (wl/eval (wd/tryIt 10))))
  (is (= "Another function in the test package."
         (wl/eval  (w/Information WolframPackageDemo/additional "Usage"))))
  (is (= 30
         (wl/eval (WolframPackageDemo/additional 10)))))

(deftest restart
  (testing "first set of aliases"
    (wl/restart {:aliases '{** Power}})
    (is (= 8
           (wl/eval '(** 2 3)))
        "** is an alias for Power (and 2^3 is 8)"))
  (testing "restart & another aliases"
    (wl/restart {:aliases '{pow Power}})
    (is (thrown-with-msg? ExceptionInfo
                          #"Unsupported symbol / unknown alias"
                          (wl/eval '(** 2 3)))
        "** is not known anymore")
    (is (= 8
           (wl/eval '(pow 2 3)))
        "Now, `pow` is an alias for Power (and 2^3 is 8)")))

(deftest bug-fixes
  (wl/start)
  (testing "#76 double eval of ->"
    (is (= '(-> x 5)
           (wl/eval (wl/eval (w/-> 'x 5))))
        "Should not throw")))

(comment
  (clojure.test/run-tests 'wolframite.core-test))
