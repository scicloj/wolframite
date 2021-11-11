(ns clojuratica.core-test
  (:require  [clojure.test :as t :refer [deftest testing is]]
             [clojuratica.core :as wl :refer [WL]]))

;; * Basic Sanity Checks

(deftest wl-evalute-initialized
  (testing (is wl/math-evaluate)))

(deftest basic-math
  (testing "1 + 1" (is (= 2 (WL (Plus 1 1))))))

(deftest basic-string<->data-translation
  (testing "translating TO clj" (is (= '(GridGraph [5 5])
                                        (wl/wl->clj "GridGraph[{5, 5}]" wl/math-evaluate))))
  (testing "translating FROM clj" (is (= "GridGraph[{5, 5}]"
                                         (wl/clj->wl '(GridGraph [5 5]) {:kernel-link wl/kernel-link
                                                                         :output-fn str})))))
