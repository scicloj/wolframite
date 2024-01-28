(ns wolframite.base.parse
  "Translate a jlink.Expr returned from an evaluation into Clojure data"
  (:require
   [wolframite.jlink]
   [wolframite.lib.options :as options]
   [wolframite.base.expr :as expr]
   [clojure.string :as str])
  (:import [com.wolfram.jlink Expr]))

(declare parse)

(defn pascal->kebab
  [s]
  (some-> s
          (str/replace #"\w(\p{Upper})" (fn [[[p l] _]] (str  p "-" (str/lower-case l))))
          str/lower-case))

(defn entity-type->keyword [expr opts]
  (let [head (pascal->kebab (expr/head-str expr))
        parts (reduce (fn [acc part]
                        (into acc (cond
                                    (string? part) [(pascal->kebab part)]
                                    (coll? part) (reverse (map pascal->kebab part))
                                    :else nil)))
                      [head]
                      (map #(parse % opts) (.args expr)))]
    ;; BAIL: if any of the parts are not recognized,
    ;; let it go through normal parsing
    (when (not-any? nil? parts)
      (keyword (str/join "." (butlast parts))
               (last parts)))))

(defn custom-parse-dispatch [expr {:keys [parse/custom-parse-symbols]}]
  (let [head (symbol (expr/head-str expr))]
    (when-not
        (and (seq custom-parse-symbols) (not (contains? (set custom-parse-symbols) head)))
      head)))

(defn atom? [expr]
  (not (.listQ expr)))

(defn simple-vector-type [expr]
  (cond (.vectorQ expr Expr/INTEGER)     Expr/INTEGER
        (.vectorQ expr Expr/BIGINTEGER)  Expr/BIGINTEGER
        (.vectorQ expr Expr/REAL)        Expr/REAL
        (.vectorQ expr Expr/BIGDECIMAL)  Expr/BIGDECIMAL
        (.vectorQ expr Expr/STRING)      Expr/STRING
        (.vectorQ expr Expr/RATIONAL)    Expr/RATIONAL
        (.vectorQ expr Expr/SYMBOL)      Expr/SYMBOL
        :else                            nil))

(defn simple-matrix-type [expr]
  (cond (.matrixQ expr Expr/INTEGER)     Expr/INTEGER
        (.matrixQ expr Expr/BIGINTEGER)  Expr/BIGINTEGER
        (.matrixQ expr Expr/REAL)        Expr/REAL
        (.matrixQ expr Expr/BIGDECIMAL)  Expr/BIGDECIMAL
        (.matrixQ expr Expr/STRING)      Expr/STRING
        (.matrixQ expr Expr/RATIONAL)    Expr/RATIONAL
        (.matrixQ expr Expr/SYMBOL)      Expr/SYMBOL
        :else                            nil))

(defn simple-array-type [expr]
  (or (simple-vector-type expr) (simple-matrix-type expr)))

;; FIXME: change name (it's more of a concrete type map)
(defn bound-map [f coll {:keys [flags] :as opts}]
  (if (options/flag?' flags :vectors)
    (mapv #(f % opts) coll)
    (map  #(f % opts) coll)))

(defn parse-complex-list [expr opts]
  (bound-map parse (.args expr) opts))

(defn parse-integer [expr]
  (let [i (.asLong expr)]
    (if (and (<= i Integer/MAX_VALUE)
             (>= i Integer/MIN_VALUE))
      (int i)
      (long i))))

(defn parse-rational [expr]
  (let [numer (parse-integer (.part expr 1))
        denom (parse-integer (.part expr 2))]
    (/ numer denom)))

(defn parse-symbol [expr {:keys [aliases/base-list]}]
  (let [aliases (into {} (map (comp vec rseq) (options/aliases base-list)))
        s       (.toString expr)
        sym     (symbol (apply str (replace {\` \/} s)))]
    (if-let [alias (aliases sym)]
      alias
      (cond (= "True" s)   true
            (= "False" s)  false
            (= "Null" s)   nil
            :else          sym))))

(defn parse-hash-map [expr opts]
  (let [inside    (first (.args expr))
        ;; inside    (first (.args expr))
        all-rules? (every? true? (map #(= "Rule" (expr/head-str %)) (.args expr)))
        rules     (cond (.listQ inside) (parse inside opts)
                        all-rules? (into {}
                                         (map (fn [kv]
                                                (bound-map (fn [x _opts] (parse x opts)) kv opts))
                                              (.args expr)))
                        (= "Dispatch" (expr/head-str inside)) (parse (first (.args inside)) opts)
                        :else (assert (or (.listQ inside)
                                          (= "Dispatch" (expr/head-str inside)))))
        keys      (map second rules)
        vals      (map (comp second #(nth % 2)) rules)]
    (if (map? rules)
      rules
      (zipmap keys vals))))

(defn parse-simple-atom [expr type opts]
  (cond (= type Expr/BIGINTEGER)   (.asBigInteger expr)
        (= type Expr/BIGDECIMAL)   (.asBigDecimal expr)
        (= type Expr/INTEGER)      (parse-integer expr)
        (= type Expr/REAL)         (.asDouble expr)
        (= type Expr/STRING)       (.asString expr)
        (= type Expr/RATIONAL)     (parse-rational expr)
        (= type Expr/SYMBOL)       (parse-symbol expr opts)))

;; parameters list used to be: [expr & [type]] (??)
(defn parse-simple-vector [expr type {:keys [flags] :as opts}]
  (let [type (or type (simple-vector-type expr))]
    (if (and (options/flag?' flags :N)
             (some #{Expr/INTEGER Expr/BIGINTEGER Expr/REAL Expr/BIGDECIMAL} #{type}))
      ((if (options/flag?' flags :vectors) vec seq) (.asArray expr Expr/REAL 1))
      (bound-map (fn [e _opts] (parse-simple-atom e type opts)) (.args expr) opts))))

(defn parse-simple-matrix [expr type opts]
  (let [type (or type (simple-matrix-type expr))]
    (bound-map (fn process-bound-map [a _opts]
                 (parse-simple-vector a type opts)) (.args expr) opts)))

(defn parse-fn
  "Return a function that invokes the Wolfram expression `expr` (typically just a symbol naming a fn),
  converting any arguments given to it from Clojure to Wolfram and does the opposite conversion on the
  result.
  Ex.: `((parse/parse-fn 'Plus {:kernel/link @wl/kernel-link-atom}) 1 2) ; => 3`

  Beware: Nesting such fns would result in multiple calls to Wolfram, which is inefficient. Prefer wl/eval in such cases."
  [expr opts]
  (fn [& args]
    (let [cep-fn (requiring-resolve `wolframite.base.cep/cep)]
      (cep-fn (apply list expr args) opts #_(update opts :flags #(options/set-flag % :as-expression))))))

(defn parse-generic-expression [expr opts]
  (-> (list)  ;must start with a real list because the promise is that expressions will be converted to lists
      (into (map #(parse % opts) (rseq (vec (.args expr)))))
      (conj (parse (.head expr) opts))))

(defn parse-complex-atom [expr {:keys [flags] :as opts}]
  (let [head (expr/head-str expr)
        ;; TODO(jh) finish this? NOTE: leaving this in until hash map support has been tested
        #_#_handle-hash-map (fn [expr opts] (if (and (options/flag?' flags :hash-maps)
                                                     (not (options/flag?' flags :full-form)))
                                              (parse-hash-map expr opts)
                                              (parse-generic-expression expr opts)))]
    (cond (.bigIntegerQ expr)      (.asBigInteger expr)
          (.bigDecimalQ expr)      (.asBigDecimal expr)
          (.integerQ expr)         (parse-integer expr)
          (.realQ expr)            (.asDouble expr)
          (.stringQ expr)          (.asString expr)
          (.rationalQ expr)        (parse-rational expr)
          (.symbolQ expr)          (parse-symbol expr opts)
          (= "Association" head)   (parse-hash-map expr opts) #_(parse-generic-expression expr opts)
          (= "Function" head)      (if (and (options/flag?' flags :functions)
                                            (not (options/flag?' flags :full-form)))
                                     (parse-fn expr opts)
                                     (parse-generic-expression expr opts))
          ;; (= "HashMapObject" head) (handle-hash-map expr opts)
          :else                    (parse-generic-expression expr opts))))

(defn standard-parse [expr {:keys [flags] :as opts}]
  (assert (instance? com.wolfram.jlink.Expr expr))
  (cond
    (options/flag?' flags :as-function)                   (parse-fn expr opts)
    (or (atom? expr) (options/flag?' flags :full-form))   (parse-complex-atom expr opts)
    (simple-vector-type expr)                        (parse-simple-vector expr nil opts)
    (simple-matrix-type expr)                        (parse-simple-matrix expr nil opts)
    :else                                            (parse-complex-list expr opts)))


(ns-unmap *ns* 'custom-parse)
(defmulti custom-parse
  "Modify how Wolfram response is parsed into Clojure data.

  The dispatch-val should be a symbol, matched against the first one of the result list.
  You can override this by including `:parse/custom-parse-symbols ['sym1 'sym2 ...]` in the flags,
  to be able to match against multiple symbols.

  Example:

  ```clj
   ; return a Java URL from a Hyperlink call, instead of `'(Hyperlink <label> <url string>)`
  (defmethod custom-parse 'Hyperlink [expr opts]
    (-> (second (.args expr)) ; 1st = label
        (parse/parse opts)
        java.net.URI.))

  (wl/eval '(Hyperlink \"foo\" \"https://www.google.com\"))
  ; or: (wl/eval '(Hyperlink \"foo\" \"https://www.google.com\") {:parse/custom-parse-symbols ['Hyperlink, #_...]})
  ; => #object[java.net.URI 0x3f5e5a46 \"https://www.google.com\"]
  ```"
  #'custom-parse-dispatch)

(defmethod custom-parse 'EntityProperty [expr opts]
  (entity-type->keyword expr opts))

(defmethod custom-parse 'Entity [expr opts]
  (entity-type->keyword expr opts))

(defmethod custom-parse :default [expr opts]
  (standard-parse expr opts))

(defn parse [expr opts]
  (custom-parse expr opts))
