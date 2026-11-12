(ns wolframite.fast-arrays-test
  "Large numeric arrays take a faster path between Clojure and Wolfram - see #113"
  (:require [clojure.test :refer [deftest is testing]]
            [wolframite.core :as wl]
            [wolframite.flags :as flags]
            [wolframite.impl.internal-constants :as internal-constants]
            [wolframite.wolfram :as w]))

(def ^:private n (* 2 internal-constants/fast-array-min-size)) ; large enough to take the fast path

(def ^:private large {:flags #{flags/allow-large-data}})
(def ^:private large+arrays {:flags #{flags/allow-large-data flags/arrays}})

(deftest wolfram->clj-test
  (wl/start!)
  (testing "reals"
    (let [xs (vec (repeatedly n rand))]
      (is (= xs (wl/eval (w/Identity (double-array xs)) large)))
      (is (= double/1 (type (wl/eval (w/Identity (double-array xs)) large+arrays))))
      (is (= xs (vec (wl/eval (w/Identity (double-array xs)) large+arrays))))))
  (testing "integers"
    (let [res (wl/eval (w/Range n) large)]
      (is (= (range 1 (inc n)) res))
      (is (every? #(instance? Integer %) res) "Same as for small arrays, ints are returned as such"))
    (is (= int/1 (type (wl/eval (w/Range n) large+arrays))))
    (is (= (repeat n 1099511627776)
           (wl/eval (w/ConstantArray (w/Power 2 40) n) large))
        "Integers beyond 32 bits are not truncated")
    (is (= [1099511627776 1] (wl/eval [(w/Power 2 40) 1]))
        "Integers beyond 32 bits are not truncated in small arrays either")
    (let [res (wl/eval (w/Range (w/Power 2 32) (w/+ (w/Power 2 32) 2)) {:flags #{flags/arrays}})]
      (is (= long/1 (type res)) "With flags/arrays, integers beyond 32 bits are returned as long[]")
      (is (= [4294967296 4294967297 4294967298] (vec res))
          "Integers beyond 32 bits are not truncated with flags/arrays"))
    (let [res (wl/eval [(w/Range (w/Power 2 32) (w/+ (w/Power 2 32) 2)) [1 2 3]] {:flags #{flags/arrays}})]
      (is (= long/2 (type res)))
      (is (= [[4294967296 4294967297 4294967298] [1 2 3]] (mapv vec res))
          "Integers beyond 32 bits are not truncated in matrices with flags/arrays")))
  (testing "matrices and 3D arrays"
    (is (= (repeat 3 (range 1 (inc n)))
           (wl/eval (w/Table (w/Range n) 3) large)))
    (is (= double/2 (type (wl/eval (w/RandomReal 1 [3 n]) large+arrays)))
        "With flags/arrays, a matrix is returned as a 2D Java array")
    (let [res (wl/eval (w/RandomReal 1 [2 3 n]) large)]
      (is (= [2 3 n] [(count res) (count (first res)) (count (ffirst res))])
          "A 3D array is read correctly")
      (is (every? double? (flatten res))))
    (let [res (wl/eval (w/RandomReal 1 [2 1 1 n]) large)]
      (is (= [2 1 1 n] [(count res) (count (first res)) (count (ffirst res)) (count (first (ffirst res)))])
          "Deeper arrays still work (the normal way)")))
  (testing "non-packed data is unaffected"
    (let [res (wl/eval (w/Join (w/Range n) [2.5]) large)]
      (is (= 1 (first res)) "Not coerced to 1.0")
      (is (= 2.5 (last res))))
    (is (= [(range 1 (inc n)) [1 2]]
           (wl/eval [(w/Range n) [1 2]] large))
        "Ragged arrays")
    (is (= (inc n) (count (wl/eval (w/Join (w/RandomComplex 1 n) [1]) large)))
        "Complex numbers"))
  (testing "other flags & features"
    (is (= :wolframite/large-data (wl/eval (w/RandomReal 1 1000000)))
        "The size limit still applies")
    (is (= "List" (str (.head (wl/eval (w/RandomReal 1 n) {:flags #{flags/allow-large-data flags/no-parse}}))))
        "flags/no-parse returns the untagged Expr")
    (is (= '(+ 1 x) (wl/eval (w/Plus 'x 1) large))
        "We don't 'leak' the wrapping fn call to the user")))

(deftest clj->wolfram-test
  (wl/start!)
  (let [doubles (vec (repeatedly n rand))
        longs (vec (range n))]
    (is (= (reduce + longs) (wl/eval (w/Total longs))) "longs")
    (is (= n (wl/eval (w/Length doubles))) "doubles")
    (is (= [3 n] (wl/eval (w/Dimensions [doubles doubles doubles]))) "matrix")
    (is (= '[Integer Real]
           (wl/eval [(w/Head (w/First (conj longs 2.5))) (w/Head (w/Last (conj longs 2.5)))]))
        "Mixed types are kept as they are")
    (is (= [n 1] (wl/eval (w/Map w/Length [longs [1]])))
        "Ragged matrix")))
