;;-------- 用宏和Repl把其他语言的解释器包起来: Lisp化
;;http://clojuratica.weebly.com/intro.html
;;http://clojuratica.weebly.com/tutorial.html

(math (Dot [1 2 3] [4 5 6])) ;; => 32

(math (D (Power x 2) x)) ;;=> (* 2 x)

(math (Plus 1 1)) ;; => 2

(math (FactorInteger 12345)) ;; => [[3 1] [5 1] [823 1]]

(math
 (Plus 1 1)
 (FactorInteger 12345)) ;;=> [[3 1] [5 1] [823 1]]

(math (Sqrt (* 9 a))) ;;=> (* 3 (Power a 1/2))

(let [x [[2 1]
         [1 2]]]
  (math (CholeskyDecomposition ~x)))
;; => [[(Power 2 1/2) (Power 2 -1/2)] [0 (Power 3/2 1/2)]]

(math
 (Function ;; 函数不存在
  [n]
  (Take
   (Sort
    (Map
     (Function [gene] [(GenomeData gene "SequenceLength") gene])
     (GenomeData)))
   n)))

(def hello
  (math
   (Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

;;(hello "World") ;; :cause "clojure.lang.PersistentList cannot be cast to clojure.lang.IFn"

(def test-mma-fn (math (Function [x] (Pause 0.5) $KernelID)))
;; => (ClojurianScopes/Function [x] nil 0)
(test-mma-fn 11) ;;   :message "clojure.lang.PersistentList cannot be cast to clojure.lang.IFn"
(test-mma-fn [11]) ;; :cause "clojure.lang.PersistentList cannot be cast to clojure.lang.IFn"

(math (&& True False))  ;; => false
(math (= f 4)) ;;=> 4
(math f)  ;;=> 4

(math
 (Plus 1
       (do
         (= f 1)
         f))) ;; => 2

(math-intern math-evaluate Plus [FI FactorInteger]) ;;=> (#'user/Plus #'user/FI)

(Plus [1 2] [3 4]) ;; => [4 6]

(FI 12345) ;; => [[3 1] [5 1] [823 1]]

(Plus (* 4 x) (+ 20 2) (AnyFunction 3)) ;; => (+ 22 (* 4 x) (AnyFunction 3))

Plus ;; :cause "Can't take value of a macro: #'user/Plus"

(math-intern math-evaluate "System`Factor*")
;; => (#'user/Factor #'user/FactorComplete #'user/Factorial #'user/Factorial2 #'user/FactorialMoment #'user/FactorialMomentGeneratingFunction #'user/FactorialPower #'user/FactorInteger #'user/FactorList #'user/FactorSquareFree #'user/FactorSquareFreeList #'user/FactorTerms #'user/FactorTermsList)

(math-intern math-evaluate :scopes) ;;=> (#'user/Module #'user/Block #'user/With #'user/Let #'user/Function)

(math-intern :as-function math-evaluate [PlusFn Plus]) ;;=> (#'user/PlusFn)

PlusFn ;; => #object[clojuratica.base.parse$parse_fn$fn__311 0x65b88b76 "clojuratica.base.parse$parse_fn$fn__311@65b88b76"]

;; 专家系统编程: clojure prolog
(map #(apply PlusFn %) [[1 2] [3 4 'a] [5 6]]) ;;=> (3 (+ 7 a) 11)

(math-evaluate (list 'Plus 3 4 'a)) ;; => (+ 7 a)

(Plus :no-parse (* 4 x) (+ 20 2) (AnyFunction 3))
;; => #object[com.wolfram.jlink.Expr 0x4759ab8b "Plus[22, Times[4, x], AnyFunction[3]]"]

(math :no-evaluate (* 4 x) (+ 20 2) (AnyFunction 3))
;;=> (do (* 4 x) (+ 20 2) (AnyFunction 3))

(Plus (* 4 x) (+ 20 2) :no-parse (AnyFunction 3))
;; => #object[com.wolfram.jlink.Expr 0x50010787 "Plus[22, Times[4, x], AnyFunction[3]]"]

(Plus (* 4 x) (+ 20 2) :no-parse :parse (AnyFunction 3))
;;=> (+ 22 (* 4 x) (AnyFunction 3))

(math :clojure-aliases {} (+ 1 2))
;; :cause "Symbols passed to Mathematica must be alphanumeric (apart from forward slashes and dollar signs)."

(def math-evaluate* (math-evaluator :no-parse kernel-link))
(def-math-macro math* math-evaluate*) ;;=> (#'user/math*)

(math* 1) ;; => #object[com.wolfram.jlink.Expr 0x18bfa7f7 "1"]

(math ^(a b c d)) ;; :cause "Metadata must be Symbol,Keyword,String or Map"

(math @(a b c d)) ;; => (a (b (c d)))

(math #'(+ % %2)) ;;=> (ClojurianScopes/Function (+ (Slot 1) (Slot 2)))

(math (+ 1 '"{1, Sqrt[4], 3+4}")) ;;=> [2 3 8]

(Block [x y z]
       (do
         (Something x)
         (SomethingElse y)
         (YetMore z))) ;;
;;=> (ClojurianScopes/Block [x y z] (YetMore z))

(YetMore z) ;; :cause "Unable to resolve symbol: YetMore in this context"

(Block [x y z]
       (Something x)
       (SomethingElse y)
       (YetMore z))
;; => (ClojurianScopes/Block [x y z] (Something x) (SomethingElse y) (YetMore z))


(math (Head [1 2 3])) ;;=> List

(math (List 1 2 3)) ;; => [1 2 3]

(math :seqs [1 2 3]);;=> (1 2 3)

(math :seqs [[1 2 3] (MyExpression arg1 arg2 arg3)])
;; => ((1 2 3) (MyExpression arg1 arg2 arg3))

[(type (first *1)) (type (second *1))]
;;=> [clojure.lang.LazySeq clojure.lang.PersistentList]

(math (Head {a b c d})) ;; => HashMap

(math ({a b c d} c)) ;;) => ((HashMap (-> a b) (-> c d)) c)

(math ({a b c d})) ;; => ((HashMap (-> a b) (-> c d)))

(math (HashMap [(-> a b) (-> c d)])) ;; => (HashMap [(-> a b) (-> c d)])

(math :no-hash-maps {a b c d}) ;; => [[a b] [c d]]

(math :no-hash-maps (Head {a b c d})) ;;=> List

(math (LaunchKernels)) ;;=> [(Parallel/Kernels/kernel (Parallel/Kernels/Private/bk ...

(math (ParallelEvaluate $KernelID)) ;;=> [1 2]

(math (ParallelMap #'(Plus % 1) [1 2 3 4]))
;; => [((ClojurianScopes/Function (+ 1 (Slot 1))) 1) ((ClojurianScopes/Function (+ 1 (Slot 1))) 2) ((ClojurianScopes/Function (+ 1 (Slot 1))) 3) ((ClojurianScopes/Function (+ 1 (Slot 1))) 4)]

(math :parallel (+ 1 1)) ;; => 2

(time
 (let [f      (math :parallel (Function [x] (Pause 0.5) $KernelID))
       agents (take 10 (repeatedly #(agent nil)))]
   (doall (map #(send-off % f) agents))
   (doall (map await agents))
   (map deref agents))) ;; 函数定义总是用不了!!!
;; :cause "clojure.lang.PersistentList cannot be cast to clojure.lang.IFn"
