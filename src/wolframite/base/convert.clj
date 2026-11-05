(ns wolframite.base.convert
  "Convert a Clojure expression into a Wolfram JLink expression"
  (:require [wolframite.flags :as flags]
            [wolframite.impl.internal-constants :as internal-constants]
            [wolframite.impl.jlink-instance :as jlink-instance]
            [wolframite.impl.protocols :as proto]
            [wolframite.impl.wolfram-syms.intern :as intern]
            [wolframite.base.express :as express]
            [wolframite.lib.options :as options]
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
  (contains? #{byte/1 double/1 int/1 long/1 short/1
               byte/2 double/2 int/2 long/2 short/2
               byte/3 double/3 int/3 long/3 short/3}
             (some-> xs .getClass)))

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

(defmulti convert
  "Convert a Wolframite clj (sub)expression into a JLink object representation.
  You likely want to use [[wrapping-convert]] instead."
  (fn [clj-expr _opts] (dispatch clj-expr))) ; TODO Pass jlink-instance in explicitly inst. of fetching from the global

(defn- wrap-limit-size [clj-expr {:keys [flags] :wolframite.core/keys [no-wolframite-wrapping] :as _opts}]
  ;; See also wolframite.base.parse/unwrap-limit-size
  (if (or (options/flag?' flags flags/allow-large-data)
          no-wolframite-wrapping)
    clj-expr
    (list internal-constants/wolframiteLimitSize clj-expr)))

(defn- wrap-fast-array [clj-expr {:keys [flags] :wolframite.core/keys [no-wolframite-wrapping] :as _opts}]
  ;; See also wolframite.impl.jlink-proto-impl/read-result
  ;; Parallel evaluation reads results via ParallelSubmit & co., not directly => no point in tagging
  (if (or no-wolframite-wrapping
          (options/flag?' flags flags/parallel))
    clj-expr
    (list internal-constants/wolframiteFastArray clj-expr)))

(defn wrapping-convert
  "Convert the symbolic Clojure expression to jlink.Expr, after wrapping it in extra Wolframite code.
  See also `wolframite.base.parse/unwrapping-parse`."
  [clj-expr opts]
  ;; NOTE Error detection is handled in wolframite.impl.jlink-proto-impl/evaluate!
  (-> clj-expr
      (wrap-limit-size opts)
      (wrap-fast-array opts)
      (convert opts)))

;; * Helpers

(defn- simple-vector? [coll _]
  (and (sequential? coll)
       (not-any? dispatch coll)))

(defn- simple-matrix? [coll opts]
  (and (sequential? coll)
       (every? #(simple-vector? % opts) coll)))

(defn- coll->primitive-array
  "Turn a non-empty collection of only doubles or only longs into a Java array.
  Nil otherwise (incl. mixed types)."
  [coll]
  (when (seq coll)
    (condp every? coll
      #(instance? Double %) (double-array coll)
      #(instance? Long %) (long-array coll)
      nil)))

(defn- ->primitive-array
  "Turn a large collection of only doubles or only longs into a Java array, which is much faster to
  send to Wolfram than a list of individual numbers. Nil otherwise, incl. for small collections,
  where it doesn't matter."
  [coll]
  (when (>= (count coll) internal-constants/fast-array-min-size)
    (coll->primitive-array coll)))

(defn- ->primitive-array-2d
  "Same as [[->primitive-array]] for a matrix, i.e. equally long rows of the same type."
  [rows]
  (when (and (every? sequential? rows)
             (>= (* (count rows) (count (first rows))) internal-constants/fast-array-min-size)
             (apply = (map count rows)))
    (let [arrays (mapv coll->primitive-array rows)]
      (when (and (every? some? arrays)
                 (apply = (map class arrays)))
        (into-array (class (first arrays)) arrays)))))

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

(defn ->wolfram-str-expr
  "Turn `str` (a raw Wolfram expression string) into a value that `convert` will
  properly process as an expression, and not as a primitive string."
  [str]
  {::wolfram-str-expr str})

(defn- wolfram-str-expr->jlink-expr [wolfram-str-expr-map opts]
  (do (assert (= 1 (count wolfram-str-expr-map)) "::wolfram-str-expr must stand on its own")
      (assert (-> wolfram-str-expr-map vals first string?) "::wolfram-str-expr value must be a string")
      (express/express (::wolfram-str-expr wolfram-str-expr-map) opts)))

(defmethod convert :hash-map [map opts]
  ;; A map could be either normal map value the user supplied, or it could be our magical
  ;; map used to mark the actual value for special treatment, namely to be interpreted as
  ;; Wolfram code in a string; see `->wolfram-str-expr`
  (if (::wolfram-str-expr map)
    (wolfram-str-expr->jlink-expr map opts)
    (convert (apply list 'Association (for [[key value] map] (list 'Rule key value))) opts)))

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
            (throw (ex-info (str "Unsupported symbol / unknown alias: Symbols passed to Mathematica must be alphanumeric"
                                 " (apart from forward slashes and dollar signs). Other symbols may"
                                 " only be used if there is defined a Wolframite alias for them."
                                 " Passed: " s
                                 " Known aliases: " (or (-> aliases keys sort seq) "N/A"))
                            {:unknown-symbol s
                             :known-symbols (keys aliases)}))
            (proto/expr (jlink-instance/get) :Expr/SYMBOL s)))))))

(defn- convert-non-simple-list [elms opts]
  (let [converted-parts (map #(cond-> % (dispatch %) (convert opts)) elms)]
    (if (every? (partial proto/expr? (jlink-instance/get)) converted-parts)
      (proto/expr (jlink-instance/get)
                  (cons (convert 'List opts)
                        converted-parts))
      (convert (to-array converted-parts) opts))))

(defmethod convert :list [coll opts]
  (if-let [arr (or (->primitive-array coll) (->primitive-array-2d coll))]
    (convert arr opts)
    (cond (simple-matrix? coll opts) (convert (to-array-2d coll) opts)
          (simple-vector? coll opts) (convert (to-array coll) opts)
          :else (convert-non-simple-list coll opts))))

(defmethod convert :expr [[head & tail :as clj-expr] opts]
  (let [macro head
        arg (first tail)]
    (cond (= 'clojure.core/deref macro)    (convert (cexpr-from-prefix-form arg) opts)
          (= 'clojure.core/meta macro)     (convert (cexpr-from-postfix-form arg) opts)
          (= 'var macro)                   (convert (list 'Function arg) opts)
          ;; convert '(whatever...)
          ;; Quoted symbol intended to be sent as Wolfram symbol
          (and (= 'quote macro)
               (symbol? arg))               (convert arg opts)
          (= 'quote macro)                 (throw (ex-info (str "Unsupported quoted expression:"
                                                                (pr-str clj-expr))
                                                           {:expr clj-expr}))
          ;; Originally we called `(express/express arg opts)` but it fails b/c it only handles strings
          :else
          (let [expr-coll (cons (convert head
                                         (cond-> opts
                                                 (symbol? head)
                                                 (assoc ::args tail)))
                                (doall (map #(convert % opts) tail)))]
           (assert (every? #(proto/expr? (jlink-instance/get) %) expr-coll))
           (proto/expr (jlink-instance/get) expr-coll)))))

(comment
  (convert '(whatever 1) nil)
  (convert '(- 12 1 2) {})
  (convert '(Plus 1 2) {}))
