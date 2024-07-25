(ns wolframite.base.convert-test
  (:require [clojure.test :refer :all]
            [wolframite.core :as wl]
            [wolframite.base.convert :refer [convert]]))

(deftest test-convert
  (#'wl/init-jlink! (deref #'wl/kernel-link-atom) {}) ; bypass private var via deref #'
  (testing "Basics"
    (testing "primitives"
      (is (= (str (convert "txt" nil)) "\"txt\""))
      (is (= (str (convert Long/MAX_VALUE nil)) (str Long/MAX_VALUE)))
      (is (= (str (convert 3.333 nil)) "3.333"))
      (is (= (str (convert (.add (BigDecimal. Long/MAX_VALUE)
                                 (BigDecimal. Long/MAX_VALUE)) nil))
             "18446744073709551614"))
      (is (= (str (convert BigInteger/ONE nil)) "1")))
    (testing "primitive arrays"
      (is (= (str (convert (int-array [1 2]) nil)) "{1,2}"))
      (is (= (str (convert (long-array [1 2]) nil)) "{1,2}"))
      (is (= (str (convert (double-array [1.1 2.2]) nil)) "{1.1,2.2}")))

    (testing "List is invocation"
      (is (= (str (convert '() nil)) "Null")
          "Empty list = Wolfram Null")
      (is (= (str (convert '(whatever 1) nil)) "whatever[1]")
          "List = invocation of a Wolfram function, with arguments"))
    (testing "Vector is W. list"
     (is (= (str (convert '[1 2] nil)) "{1, 2}")
         "Vector becomes Wolfram list"))
    (testing "Clojure map ..."
      (is (= (str (convert '{:evil 1, "good" 2} nil))
             "Association[Rule[\":evil\", 1], Rule[\"good\", 2]]")
          "Map becomes W. Association, with keys stringified")))
  (testing "Clojure lambda -> `Function[{x}, x]`"
    (let [expr (convert '(fn [x] x) nil)
          [signature body] (.args expr)]
      (is (= "Function" (str (.head expr))))
      (is (= 2 (count (.args expr))))
      (is (= "{x}" (str signature)))
      (is (= "x" (str body))))))

