(ns wolframite.base.convert-test
  (:require [clojure.test :refer :all]
            [wolframite.base.convert :as convert :refer [convert]]
            [wolframite.core :as wl]
            [wolframite.impl.jlink-instance :as jlink-instance]
            [wolframite.wolfram :as w]))

(deftest test-convert
  ;(#'wl/init-jlink! (deref #'wl/kernel-link-atom) {}) ; bypass private var via deref #'
  (wl/start!) ; needed for Wolfram expr string interpretation
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
  (testing "raw Wolfram expr. string"
    (is (= "Plus[1, 2]"
           (str (convert (convert/->wolfram-str-expr "Plus[1,2]")
                         {:jlink-instance (jlink-instance/get)})))))
  (testing "Clojure lambda -> `Function[{args}, body]`"
    (testing "(fn [x] x)"
      (let [expr (convert '(fn [x] x) nil)
            [signature body] (.args expr)]
        (is (= "Function" (str (.head expr))))
        (is (= 2 (count (.args expr))))
        (is (= "{x}" (str signature)))
        (is (= "x" (str body)))))
    (testing "(fn [x] (Plus x x)"
      (let [expr (convert '(fn [x] (Plus x x)) nil)
            [signature body] (.args expr)]
        (is (= "Function" (str (.head expr))))
        (is (= 2 (count (.args expr))))
        (is (= "{x}" (str signature)))
        (is (= "Plus[x, x]" (str body)))))
    (testing "w/fn"
      (testing "basics"
        (is (= (convert '(fn [x] x) nil)
               (convert (w/fn [x] x) nil))
            "Raw and evaluated form are equivalent")
        (is (= (convert '(fn [x] (Internal/StringToMReal x)) nil)
               (convert (w/fn [x] '(Internal/StringToMReal x)) nil))
            "Allow quoting inside the body (so the IDE doesn't complain of unknown symbols)"))
      (testing "using evaluated form w/<some-fn>"
        (is (= (convert '(fn [x] (Plus x x)) nil)
               (convert (w/fn [x] (w/Plus x x)) nil))
            "We can use w/some-fn inside the body"))
      (testing "deeper nesting of fns and w/functions"
        (is (= (convert '(fn [x] (Plus x (Minus x))) nil)
               (convert (w/fn [x] (w/Plus x (w/Minus x))) nil))))
      (testing "quoted sub-expression"
        (is (= "\"Map[Function[{row}, Internal`StringToMReal[row]], {\\\"123\\\"}]\"" ; not sure why JLink .toString double-escapes the strs
               (-> (convert (wl/->wl (w/Map (w/fn [row] '(Internal/StringToMReal row))
                                            ["123"]))
                            nil)
                   str))
            "Fn body with is converted correctly"))))
  (testing "Various"
    (testing "Wolfram namespaces"
      (is (= (str (convert '(Internal/StringToMReal "123.56") nil))
             "Internal`StringToMReal[\"123.56\"]")
          "Clojure ns becomes Wolfram ns"))))
