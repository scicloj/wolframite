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

(deftest parse-with-kernel-test
  (wl/start)
  (let [ctx {:jlink-instance (jlink-instance/get)}]
    (testing "pure fn" ; needed by ->clj when translating wolfram expressions using this
      (testing "w/ named args"
        (is (= '(fn [x] x)
               (-> (convert/convert (convert/->wolfram-str-expr "Function[{x},x]") ctx)
                   (parse/parse nil))))
        (is (= '(fn [x y] (+ x y))
               (-> (convert/convert (convert/->wolfram-str-expr "Function[{x,y},x + y]") ctx)
                   (parse/parse nil)))))
      (testing "w/ anonymous args"
        ;; Fn w/ named args: Function[{x}, Plus[x, 1]]
        ;; Lambda w/ anonymous args: Function[Plus[(Slot 1), 1]]
        (is (= '(fn (Slot 1))
               (-> (convert/convert (convert/->wolfram-str-expr "#&") ctx)
                   (parse/parse nil)))
            "An anonymous, one-arg lambda becomes Function (aliased to fn) w/ Slot
            (# = #1 = Slot 1; `body&` is the fn)")
        (is (= '(fn (+ (Slot 1) (Slot 2)))
               (-> (convert/convert (convert/->wolfram-str-expr "#1 + #2&") ctx)
                   (parse/parse nil))))))))

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
