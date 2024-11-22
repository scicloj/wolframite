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
  Example usage: `(wl/! (w/Map (w/fn [x] (w/Plus x 1)) [1 2 3]))`"
  ([body-sexp]
   ;; A pure lambda with anonymous args (#, #1, ... = Slot[1], ...) or possibly no args at all
   `(list (quote ~'Function) ~(wolframite.impl.wolfram-syms.intern/quote-args body-sexp [])))
  ([args body-sexp]
   (clojure.core/assert (clojure.core/vector? args))
   ;; If there is any w/ symbol such as w/Plus, replace it just with the fn name, `Plus` -
   ;; we need the whole body to be pure symbols and primitives only, which Wolfram will understand
   `(list (quote ~'Function) (quote ~args) ~(wolframite.impl.wolfram-syms.intern/quote-args body-sexp args))))
