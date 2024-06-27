(ns wolframite.core-test
  (:require  [clojure.test :as t :refer [deftest testing is]]
             [wolframite.core :as wl]))

;; * Basic Sanity Checks

;; (deftest wl-evalute-initialized
;;   (testing (is wl/math-evaluate)))

(deftest basic-math
  (wl/init!)
  (testing "1 + 1" (is (= 2 (wl/eval '(Plus 1 1))))))

;; (deftest basic-string<->data-translation
;;   (testing "translating TO clj" (is (= '(GridGraph [5 5])
;;                                         (wl/wl->clj "GridGraph[{5, 5}]" wl/math-evaluate))))
;;   (testing "translating FROM clj" (is (= "GridGraph[{5, 5}]"
;;                                          (wl/clj->wl '(GridGraph [5 5]) {:kernel-link wl/kernel-link
;;                                                                          :output-fn str})))))

(deftest load-all-symbols-test
  ;; NOTE For this test, we need to wrap expression with (clojure.core/eval (quote ...)) so that
  ;; we can load the file into REPL even before we run the code which creates the w/* vars the
  ;; code is using.
  ;; End-users face the same problem: they code works in the REPL when evaluated line by line, but
  ;; cannot be loaded as a whole (unless they have init! and load-all-symbols at the top level)
  (wl/init!)
  (wl/load-all-symbols 'w)
  (is (= 3
         (wl/eval (eval '(w/Plus 1 2)))) ; FIXME Fails to load due to "No such var: w/Plus"
      "An interned symbol can be used at a function position")
  (is (= 3
         (wl/eval (eval '(w/Floor w/Pi))))
      "An interned symbol can be used at a value position")
  (is (= -3
         (wl/eval (eval '(w/Floor (w/Plus 1 (w/Minus w/Pi)))))
         (wl/eval '(Floor (Plus 1 (Minus Pi)))))
      "Interned vars behave properly also when nested few levels deep")
  (is (= "x+y+z represents a sum of terms."
         (eval '(-> #'w/Plus meta :doc)))
      "Interned vars have docstrings"))

(comment
  (clojure.test/run-tests 'wolframite.core-test))
