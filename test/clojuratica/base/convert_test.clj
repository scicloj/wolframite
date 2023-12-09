(ns clojuratica.base.convert-test
  (:require [clojure.test :refer :all])
  (:require [clojuratica.base.convert :refer [convert]]))

(deftest test-convert
  (testing "Basics"
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

