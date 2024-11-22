(ns wolframite.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [wolframite.core :as wl]
            [wolframite.flags :as flags]
            [wolframite.impl.wolfram-syms.wolfram-syms :as wolfram-syms]
            [wolframite.wolfram :as w :refer :all
             :exclude [* + - -> / < <= = == > >= fn
                       Byte Character Integer Number Short String Thread]]
            [wolframite.wolfram :as w])
  (:import (clojure.lang ExceptionInfo)))

;; * Basic Sanity Checks

;; (deftest wl-evalute-initialized
;;   (testing (is wl/math-evaluate)))

(deftest basic-math
  (wl/start!)
  (is (= 2 (wl/eval '(Plus 1 1)))
      "A simple expression-as-symbol")
  (is (= 2 (wl/eval "Plus[1,1]"))
      "A simple expression-as-Wolfram-str")
  (is (= 2 (wl/eval (w/Plus 1 1)))
      ;; see wolfram-test for more complex tests of the Wolf vars
      "A simple expression-as-var"))

(deftest various-data-types
  (wl/start!)
  (is (= -1 (wl/eval '(Minus 1)))
      "Number")
  (is (= "1" (wl/eval (ToString 1)))
      "String")
  (is (= {"key" "value"}
         (wl/eval (Association (w/-> "key" "value"))))
      "Map")
  (is (= [1 2]
         (wl/eval (Join [1] [2])))
      "Wolf List <-> Clj vector")
  (testing "arrays of supported _fast_ types (byte, short, int, float, double)"
    ;; NOTE: Arrays are supported for  (boolean, byte, char, short, int, long, float, double, String) but only the above have "fast" methods
    ;; Note: Aside of nested lists, Wolfram has also other representations such as PackedArray, SparseArray and structured arrays
    ;; "Representing matrices as structured arrays, whenever feasible, often leads to efficiencies in storage, computation time or both."
    ;; TODO See Wolfram Videos: Structured Arrays https://shar.es/ag4By2
    ;; TODO See Wolfram Videos: Everything Arrays https://shar.es/ag4ByE
    (is (= [2 3]
           (wl/eval (Plus (into-array Integer/TYPE [1 2]) 1)))
        "Arrays may be sent from Wolframite instead of lists")
    (let [res ^ints (wl/eval (Plus (into-array Integer/TYPE [1 2]) 1) {:flags #{flags/arrays}})]
      (is (= int/1 (type res)) "Should return as array of the same type.")
      (is (java.util.Arrays/equals (into-array Integer/TYPE [2 3]) res)
          (str "Should return as array of the same type. Got: " (seq res))))
    (is (= int/1
           (type (wl/eval (Plus [1 2] 1) {:flags #{:arrays}})))
        "Even if we send in a vector and not an array, we get an array back when the flag is on")
    (let [res ^doubles (wl/eval (Plus (into-array Double/TYPE [1.0 2.0]) 1) {:flags #{flags/arrays}})]
      (is (= double/1 (type res)) "Should return as array of the same type.")
      (is (java.util.Arrays/equals (into-array Double/TYPE [2.0 3.0]) res)
          (str "Should return as array of the same type. Got: " (seq res))))))

;; (deftest basic-string<->data-translation
;;   (testing "translating TO clj" (is (= '(GridGraph [5 5])
;;                                         (wl/wl->clj "GridGraph[{5, 5}]" wl/math-evaluate))))
;;   (testing "translating FROM clj" (is (= "GridGraph[{5, 5}]"
;;                                          (wl/clj->wl '(GridGraph [5 5]) {:kernel-link wl/kernel-link
;;                                                                          :output-fn str})))))

(deftest load-all-symbols-test
  (wl/start!)
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
  (wl/start!)
  (is (= 'WolframPackageDemo
         (wl/<<! "resources/WolframPackageDemo.wl")))
  (is (= 'WolframPackageDemo
         (wl/load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo")))
  (is (= 'wd
         (wl/load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo" 'wd)))

  (is (= "Used for testing Wolframite."
         ;; NOTE: Tests are not repl, so we need to use `eval` to stop Clojure complaining about
         ;; unknown namespaces wd, WolframPackageDemo when _loading_ this file as a whole
         (wl/eval  (w/Information (eval 'wd/tryIt) "Usage"))))
  (is (= 1000
         (wl/eval (eval '(wd/tryIt 10)))))
  (is (= "Another function in the test package."
         (wl/eval (w/Information (eval 'WolframPackageDemo/additional) "Usage"))))
  (is (= 30
         (wl/eval (eval '(WolframPackageDemo/additional 10))))))

(deftest restart!
  (testing "first set of aliases"
    (wl/restart! {:aliases '{test** Power}})
    (is (= 8
           (wl/eval '(test** 2 3)))
        "test** is an alias for Power (and 2^3 is 8)"))
  (testing "restart & another aliases"
    (wl/restart! {:aliases '{pow Power}})
    (is (thrown-with-msg? ExceptionInfo
                          #"Unsupported symbol / unknown alias"
                          (wl/eval '(test** 2 3)))
        "test** is not known anymore and thus should fail")
    (is (= 8
           (wl/eval '(pow 2 3)))
        "Now, `pow` is an alias for Power (and 2^3 is 8)")))

(deftest quoted-symbols-test
  (wl/start!)
  (testing "Wolfram namespaces"
    (is (= (wl/eval '(Internal/StringToMReal "-123.56"))
           -123.56)
        ;; BEWARE: This may fail if/when Wolfram renames (again) this internal fn
        "Namespaced symbols are turned into fully qualified Wolfram symbols")))

(deftest corner-cases
  (wl/start!)
  (testing "Lambdas, nested"
    (is (= [[-1 -2]]
           (wl/eval (w/Map (w/fn [x]
                             (w/Map (w/fn [x] (w/Minus x)) x))
                           [[1 2]])))
        "Two nested uses of w/fn with arg shadowing should work")))

(deftest bug-fixes
  (wl/start!)
  (testing "#76 double eval of ->"
    (is (= '(-> x 5)
           (wl/eval (wl/eval (w/-> 'x 5))))
        "Should not throw"))
  (testing "Quoted expr inside w/fn fails to convert due to missing jlink-instance"
    (is (= [123.0]
           (wl/eval (wl/->wl (w/Map (w/fn [row] '(Internal/StringToMReal row))
                                    ["123"])))))))

(deftest ->clj
  (wl/start!)
  (is (= '(+ 1 2)
         (wl/->clj "Plus[1,2]"))
      "Translating Wolfram expr string to Wolframatica data form works")
  (testing "pure lambda"
    (is (= '(fn (Slot 1))
           (wl/->clj "#&"))
        "An anonymous, one-arg lambda becomes Function (aliased to fn) w/ Slot")
    (is (= [2 4]
           (wl/eval "Map[#+1&,{1,3}]")
           (wl/eval (w/Map (w/fn (w/+ (w/Slot 1) 1)) [1 3]))))
    (is (= [2 4]
           (wl/eval "Map[Function[{x}, x + 1],{1,3}]")
           (wl/eval (w/Map (w/fn [x] (w/+ x 1)) [1 3]))))))

(deftest error-handling
  (wl/start!)
  (is (thrown? ExceptionInfo
               (wl/eval (w/FromDigits "-87.6")))
      "Should throw on invalid Wolfram expression"))

(deftest kindly-support
  (wl/start!)
  (let [res (wl/eval '(Video "path/to/my-fake.mps"))
        {:keys [kindly/options] :as m}
        (meta res)
        view-fn (:kindly/f options)]
    (is (= 'Video (first res)) "Sanity check: expect Wolfram to return (Video ..)")
    (is (= :kind/fn (:kindly/kind m)))
    (is (= {:src "path/to/my-fake.mps"} (view-fn res))
        "Correct metadata is attached, including view fn to extract the video url")
    (is (-> (view-fn res) meta :kind/video))))


(comment
  (wl/->wl (w/Map (w/fn [] (w/+ (w/Slot 1) 1)) [1 3]))
  (wl/eval (w/Map (w/fn [x] (w/Plus x 1)) [1 2 3]))
  (wl/eval (w/Map (w/fn [x] (w/+ x 1)) [1 3]))
  (clojure.test/run-tests 'wolframite.core-test))
