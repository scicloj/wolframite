(ns wolframite.base.convert
  "Convert a Clojure expression into a Wolfram JLink expression"
  (:require [wolframite.impl.jlink-instance :as jlink-instance]
            [wolframite.impl.protocols :as proto]
            [wolframite.impl.wolfram-syms.intern :as intern]
            [wolframite.lib.options :as options]
            [wolframite.base.express :as express]
            [wolframite.base.expr :as expr]
            [wolframite.runtime.defaults :as defaults]))

;; (remove-ns 'wolframite.base.convert)

;; * defmulti and dispatch

(defn- primitive?
  "Is `x` a 'primitive' value that can be directly turned into an Expr?"
  [x]
  (or (number? x)
      (string? x)))

(defn- supported-primitive-array? [xs]
  ;; See jlink.Expr constructor for the types actually supported
  (some-> xs .getClass .componentType (#{Byte/TYPE Double/TYPE Integer/TYPE Long/TYPE Short/TYPE})))

(defn- dispatch [obj]
  (cond (and (list? obj)
             (empty? obj)) :null
        (seq? obj) :expr
        (or (vector? obj)
            (list? obj)) :list
        (ratio? obj) :rational
        (primitive? obj) :primitive
        (map? obj) :hash-map
        (symbol? obj) :symbol
        (nil? obj) :null
        (fn? obj) :fn-obj
        :else nil))

(defmulti convert (fn [obj _]
                    (dispatch obj)))

;; * Helpers

(defn- simple-vector? [coll _]
  (and (sequential? coll)
       (not-any? dispatch coll)))

(defn- simple-matrix? [coll opts]
  (and (sequential? coll)
       (every? #(simple-vector? % opts) coll)))

(defn- cexpr-from-postfix-form [cexprs]
  (assert (sequential? cexprs))
  (loop [cexpr     (first cexprs)
         remaining (rest cexprs)]
    (if (seq remaining)
      (recur (list (first remaining) cexpr) (rest remaining))
      cexpr)))

(defn- cexpr-from-prefix-form [cexprs]
  (assert (sequential? cexprs))
  (cexpr-from-postfix-form (reverse cexprs)))

;; * Method impls

(defmethod convert nil [obj _]
  ;; A  fall-back implementation, for anything we do not handle directly ourselves elsewhere here.
  ;; Also triggered for any other unknown/unhandled type, e.g. a :kwd
  (cond
    (proto/expr? (jlink-instance/get) obj)
    obj ; already a jlink Expr

    (supported-primitive-array? obj)
    (proto/expr (jlink-instance/get) obj)

    :else
    (proto/->expr (jlink-instance/get) obj)))

(defmethod convert :fn-obj [obj opts]
  ;; This normally means that the expression contained a reference to a var in wolframite.wolfram,
  ;; which has not been turned into a symbol for some reason, typically b/c it was not a fn call
  ;; (those do 'symbolify' themselves) => we check and do this here
  (if-let [fn-name (intern/interned-var-val->symbol obj)]
    (convert fn-name opts)
    (throw (IllegalArgumentException.
             (str "An expression contains a function object, which is not intern/wolfram-fn => "
                  "don't know how to turn it into a symbol that Wolfram could interpret: "
                  obj)))))


(defmethod convert :null [_ opts]
  (convert 'Null opts))

(defmethod convert :rational [n opts]
  (convert (list 'Rational (.numerator n) (.denominator n)) opts))

(defmethod convert :primitive [primitive _opts]
  (proto/expr (jlink-instance/get) primitive))

(defmethod convert :hash-map [map {:keys [flags] :as opts}]
  (if (options/flag?' flags :hash-maps)
    (convert (apply list 'Association (for [[key value] map] (list 'Rule key value))) opts)
    (convert (seq map) opts)))

(defmethod convert :symbol [sym {:keys [aliases] ::keys [args] :as opts}]
  (let [all-aliases (merge defaults/all-aliases aliases)]
    (if-let [alias-sym-or-fn (all-aliases sym)]
      (if (defaults/experimental-fn-alias? alias-sym-or-fn)
        (convert (alias-sym-or-fn args) opts)
        (convert alias-sym-or-fn opts))
      ;; Numbered args of shorthand lambdas - Clojure's `#(+ %1 %2)` => Wolfs #1 and #1 = Slot[1] and Slot[2]
      (if-let [[_ ^String n] (re-matches #"%(\d*)" (str sym))]
        (let [n (Long/valueOf (if (= "" n) "1" n))]
          (convert (list 'Slot n) opts))
        ;(let [s (str-utils/replace (str sym) #"\|(.*?)\|" #(str "\\\\[" (second %) "]"))]   )
        (let [s (str sym)]
          (if (re-find #"[^a-zA-Z0-9$\/]" s)
            (throw (Exception. (str "Symbols passed to Mathematica must be alphanumeric (apart from forward slashes and dollar signs). Passed: " s)))
            (proto/expr (jlink-instance/get) :Expr/SYMBOL s)))))))

(defn- convert-non-simple-list [elms opts]
  (let [converted-parts (map #(cond-> % (dispatch %) (convert opts)) elms)]
    (if (every? (partial proto/expr? (jlink-instance/get)) converted-parts)
      (proto/expr (jlink-instance/get)
                  (cons (convert 'List opts)
                        converted-parts))
      (convert (to-array converted-parts) opts))))

(defmethod convert :list [coll opts]
  (cond (simple-matrix? coll opts) (convert (to-array-2d coll) opts)
        (simple-vector? coll opts) (convert (to-array coll) opts)
        :else (convert-non-simple-list coll opts)))

(defmethod convert :expr [[head & tail :as _cexpr] opts]
  (let [macro head
        arg (first tail)]
    (cond (= 'clojure.core/deref macro)    (convert (cexpr-from-prefix-form arg) opts)
          (= 'clojure.core/meta macro)     (convert (cexpr-from-postfix-form arg) opts)
          (= 'var macro)                   (convert (list 'Function arg) opts)
          (= 'quote macro)                 (express/express arg opts)
          :else                            (expr/expr-from-parts
                                             (cons (convert head
                                                            (cond-> opts
                                                                    (symbol? head)
                                                                    (assoc ::args tail)))
                                                   (map #(convert % opts) tail))))))

(comment
  (convert '(- 12 1 2) {})
  (convert '(Plus 1 2) {}))