(ns wolframite.base.parse-test
  (:require
    [clojure.test :refer :all]
    [wolframite.base.convert :as convert]
    [wolframite.base.parse :as parse]
    [wolframite.core :as wl]
    [wolframite.wolfram :as w]
    [wolframite.impl.jlink-instance :as jlink-instance]))

(deftest parse-test
  (#'wl/init-jlink! (deref #'wl/kernel-link-atom) {}) ; bypass private var via deref #'
  (testing "atomic expr"
    (is (= "str"
           (parse/unwrapping-parse (convert/convert "str" nil) nil))
        "string")
    (is (= 1
           (parse/unwrapping-parse (convert/convert 1 nil) nil))
        "int")
    (is (= Long/MAX_VALUE
           (parse/unwrapping-parse (convert/convert Long/MAX_VALUE nil) nil))
        "long")
    (is (= 1.1
           (parse/unwrapping-parse (convert/convert 1.1 nil) nil))
        "float")
    (is (= (bigint 333)
           (parse/unwrapping-parse (convert/convert (bigint 333) nil) nil))
        "bigint")
    #_ ; Not sure how to test w/o actually calling eval...
    (is (= (/ 1 3)
           (parse/unwrapping-parse (convert/convert (/ 1 3) nil) nil))
        "ratio")
    (is (= 'MySym
           (parse/unwrapping-parse (convert/convert 'MySym nil) nil))
        "symbol"))
  (testing "maps"
    (is (= {"a" 1}
           (parse/unwrapping-parse (convert/convert {"a" 1} nil) nil)))
    (is (= {}
           (parse/unwrapping-parse (convert/convert {} nil) nil))
        "Empty maps work too"))
  (testing "RuleDelayed"
    ;; Some errors are returned as a MessageTemplate -> delayed call to get the message name, f.ex.
    ;; `(wl/! (list (w/Interpreter "City") "San Francisco, US"))` while offline.
    (let [parsed (-> (w/Association (w/RuleDelayed "MessageTemplate" (w/MessageName w/Interpreter "noknow")))
                     (convert/convert nil)
                     (parse/unwrapping-parse nil))]
      (is (= {"MessageTemplate" '(MessageName Interpreter "noknow")}
             parsed)
          "We can parse RuleDelayed")
      (is (= true
             (-> parsed vals first meta :wolfram/delayed))))))

(deftest custom-parse
  (wl/start!)
  (testing "entities & co."
    (is (= (w/Entity "City" ["SanFrancisco" "California" "UnitedStates"])
           (wl/eval (w/Entity "City" ["SanFrancisco" "California" "UnitedStates"]))))
    (is (= (w/EntityProperty "City" "ActiveHomeListingCount")
           (wl/eval (w/Part (list (w/Entity "City" ["LosAngeles" "California" "UnitedStates"]) "Properties")
                            1))))
    (is (= (-> #'w/Quantity meta :name)
           (->> (wl/eval (w/Part (list (w/Entity "City" ["LosAngeles" "California" "UnitedStates"]) "Properties")
                                 1))
                (list (w/Entity "City" ["SanFrancisco" "California" "UnitedStates"]))
                (wl/eval)
                first))
        "Round-trip works (we get Ent.Prop. back, pass that to another eval)")))

(deftest parse-with-kernel-test
  (wl/start!)
  (let [ctx {:jlink-instance (jlink-instance/get)}]
    (testing "pure fn" ; needed by ->clj when translating wolfram expressions using this
      (testing "w/ named args"
        (is (= '(fn [x] x)
               (-> (convert/convert (convert/->wolfram-str-expr "Function[{x},x]") ctx)
                   (parse/unwrapping-parse nil))))
        (is (= '(fn [x y] (+ x y))
               (-> (convert/convert (convert/->wolfram-str-expr "Function[{x,y},x + y]") ctx)
                   (parse/unwrapping-parse nil)))))
      (testing "w/ anonymous args"
        ;; Fn w/ named args: Function[{x}, Plus[x, 1]]
        ;; Lambda w/ anonymous args: Function[Plus[(Slot 1), 1]]
        (is (= '(fn (Slot 1))
               (-> (convert/convert (convert/->wolfram-str-expr "#&") ctx)
                   (parse/unwrapping-parse nil)))
            "An anonymous, one-arg lambda becomes Function (aliased to fn) w/ Slot
            (# = #1 = Slot 1; `body&` is the fn)")
        (is (= '(fn (+ (Slot 1) (Slot 2)))
               (-> (convert/convert (convert/->wolfram-str-expr "#1 + #2&") ctx)
                   (parse/unwrapping-parse nil))))))))

(deftest fix-minus-reverse-aliased
  (wl/start!) ; we need the Kernel for turning stringified Wolfram into Expr
  (let [ctx {:jlink-instance (jlink-instance/get)}]
    (is (= '(- 42)
           (-> (convert/convert (convert/->wolfram-str-expr "Minus[42]") ctx)
               (parse/unwrapping-parse nil)))
        "Minus is parsed as the alias `-`")
   (is (= '(- 4 2)
          (-> (convert/convert (convert/->wolfram-str-expr "Subtract[4, 2]") ctx)
              (parse/unwrapping-parse nil)))
       "Subtract is parsed as the alias `-`")))
