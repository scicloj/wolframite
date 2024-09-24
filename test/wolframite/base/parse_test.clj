(ns wolframite.base.parse-test
  (:require
    [clojure.test :refer :all]
    [wolframite.base.convert :as convert]
    [wolframite.base.parse :as parse]
    [wolframite.core :as wl]
    [wolframite.impl.jlink-instance :as jlink-instance]))

(deftest parse-test
  (#'wl/init-jlink! (deref #'wl/kernel-link-atom) {}) ; bypass private var via deref #'
  (testing "maps"
    (is (= {"a" 1}
           (parse/parse (convert/convert {"a" 1} nil) nil)))
    (is (= {}
           (parse/parse (convert/convert {} nil) nil))
        "Empty maps work too")))

(deftest fix-minus-reverse-aliased
  (wl/start) ; we need the Kernel for turning stringified Wolfram into Expr
  (let [ctx {:jlink-instance (jlink-instance/get)}]
    (is (= '(- 42)
           (-> (convert/convert (convert/->wolfram-str-expr "Minus[42]") ctx)
               (parse/parse nil)))
        "Minus is parsed as the alias `-`")
   (is (= '(- 4 2)
          (-> (convert/convert (convert/->wolfram-str-expr "Subtract[4, 2]") ctx)
              (parse/parse nil)))
       "Subtract is parsed as the alias `-`")))
