(ns clojuratica.base.parse
  (:require
   [clojuratica.jlink]
   [clojuratica.lib.options :as options]
   [clojuratica.base.expr :as expr]
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
    ;; BAIL: if any of the parts are not reckognized,
    ;; let it go through normal parsing
    (when (not-any? nil? parts)
      (keyword (str/join "." (butlast parts))
               (last parts)))))

(defn custom-parse-dispatch [expr {:keys [parse/custom-parse-symbols]}]
  (let [head (symbol (expr/head-str expr))]
    (when-not
        (and (seq custom-parse-symbols) (not (contains? (set custom-parse-symbols) head)))
      head)))

(ns-unmap *ns* 'custom-parse)
(defmulti custom-parse #'custom-parse-dispatch)

;; (defmethod custom-parse 'Hyperlink [expr opts]
;;   (let [parsed-url (parse (second (.args expr)) opts)]
;;     (try
;;       (java.net.URL. parsed-url)
;;       (catch java.net.MalformedURLException _
;;         parsed-url))))

(defmethod custom-parse 'EntityProperty [expr opts]
  (entity-type->keyword expr opts))

(defmethod custom-parse 'Entity [expr opts]
  (entity-type->keyword expr opts))

(defmethod custom-parse :default [_ _]
  nil)

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
        rules     (parse
                   (cond (.listQ inside) inside
                         (= "Dispatch" (expr/head-str inside)) (first (.args inside))
                         :else (assert (or (.listQ inside)
                                           (= "Dispatch" (expr/head-str inside)))))
                   opts)
        keys      (map second rules)
        vals      (map (comp second #(nth % 2)) rules)]
    (zipmap keys vals)))

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
    (bound-map #(parse-simple-vector % type opts) (.args expr) opts)))

(defn parse-fn [expr opts]
  (fn [& args]
    (let [cep-fn (requiring-resolve `clojuratica.base.cep/cep)]
      (cep-fn (apply list expr args) opts #_(update opts :flags #(options/set-flag % :as-expression))))))

(defn parse-generic-expression [expr opts]
  (-> (list)  ;must start with a real list because the promise is that expressions will be converted to lists
      (into (map #(parse % opts) (rseq (vec (.args expr)))))
      (conj (parse (.head expr) opts))))

(defn parse-complex-atom [expr {:keys [flags] :as opts}]
  (let [head (expr/head-str expr)]
    (cond (.bigIntegerQ expr)      (.asBigInteger expr)
          (.bigDecimalQ expr)      (.asBigDecimal expr)
          (.integerQ expr)         (parse-integer expr)
          (.realQ expr)            (.asDouble expr)
          (.stringQ expr)          (.asString expr)
          (.rationalQ expr)        (parse-rational expr)
          (.symbolQ expr)          (parse-symbol expr opts)
          (= "Function" head)      (if (and (options/flag?' flags :functions)
                                            (not (options/flag?' flags :full-form)))
                                     (parse-fn expr opts)
                                     (parse-generic-expression expr opts))
          (= "HashMapObject" head) (if (and (options/flag?' flags :hash-maps)
                                            (not (options/flag?' flags :full-form)))
                                     (parse-hash-map expr opts)
                                     (parse-generic-expression expr opts))
          :else                    (parse-generic-expression expr opts))))

(defn parse [expr {:keys [flags] :as opts}]
  (assert (instance? com.wolfram.jlink.Expr expr))
  (cond (options/flag?' flags :as-function)                   (parse-fn expr opts)
        (or (atom? expr) (options/flag?' flags :full-form))   (parse-complex-atom expr opts)
        (simple-vector-type expr)                        (parse-simple-vector expr nil opts)
        (simple-matrix-type expr)                        (parse-simple-matrix nil expr opts)
        :else                                            (parse-complex-list expr opts)))
