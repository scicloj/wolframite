(ns wolframite.impl.wolfram-syms.write-ns.includes
  "The content is to be included in the generated `wolframite.wolfram` ns."
  (:require
    clojure.walk
    wolframite.impl.wolfram-syms.intern)
  (:refer-clojure :only [defmacro let list]))

;; Cursive: resolve as defn; Kondo: TBD
;; TODO: Do we want to enable specs?!
;(s/fdef fn
;        :args (s/cat :params :clojure.core.specs.alpha/params+body
;                     :body any?)
;        :ret any?)

;;--INCLUDE-START--
(defmacro fn
  "Creates a Wolfram anonymous function with the given arguments and single expression body.
  Example usage: `(wl/eval (w/Map (w/fn [x] (w/Plus x 1)) [1 2 3]))`"
  [args sexp]
  ;; If there is any w/ symbol such as w/Plus, replace it just with the fn name, `Plus` -
  ;; we need the whole body to be pure symbols and primitives only, which Wolfram will understand
  (let [symbolic-sexp (clojure.walk/prewalk wolframite.impl.wolfram-syms.intern/try->wolf-sym sexp)]
    `(list (quote ~'Function) (quote ~args) (quote ~symbolic-sexp))))
