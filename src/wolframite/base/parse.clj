(ns wolframite.base.parse
  "Translate a jlink.Expr returned from an evaluation into Clojure data"
  (:require
    [clojure.set :as set]
    [clojure.tools.logging :as log]
    [wolframite.flags :as flags]
    [wolframite.impl.internal-constants :as internal-constants]
    [wolframite.impl.jlink-instance :as jlink-instance]
    [wolframite.impl.protocols :as proto]
    [wolframite.lib.options :as options]
    [wolframite.base.expr :as expr]
    [clojure.string :as str]))

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

(defn custom-parse-dispatch [expr _opts]
  (let [head (symbol (expr/head-str expr))]
    head))

(defn atom? [expr]
  (not (.listQ expr)))

(defn simple-vector-type [expr]
  (proto/expr-element-type (jlink-instance/get) :vector expr))

(defn simple-matrix-type [expr]
  (proto/expr-element-type (jlink-instance/get) :matrix expr))

(defn simple-array-type [expr]
  (or (simple-vector-type expr) (simple-matrix-type expr)))

;; FIXME: change name (it's more of a concrete type map)
(defn bound-map [f coll {:keys [flags] :as opts}]
  (if (options/flag?' flags :seqs)
    (map  #(f % opts) coll)
    (mapv #(f % opts) coll)))

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
  (let [alias->wolf (options/aliases base-list)
        smart-aliases (keep (fn [[alias wolf]]
                              (when (fn? wolf) alias))
                            alias->wolf)
        wolf->smart-alias (into {} (for [[alias smart-fn] (select-keys alias->wolf smart-aliases)
                                         wolf-sym (-> smart-fn meta :wolframite.alias/targets)
                                         :when wolf-sym]
                                     [wolf-sym alias]))
        wolf->alias (-> (apply dissoc alias->wolf smart-aliases)
                        set/map-invert
                        (merge wolf->smart-alias))
        s       (.toString expr)
        sym     (symbol (apply str (replace {\` \/} s)))]
    (if-let [alias (get wolf->alias sym)]
      alias
      (cond (= "True" s)   true
            (= "False" s)  false
            (= "Null" s)   nil
            :else          sym))))

(defn parse-hash-map [expr opts]
  (let [inside    (first (.args expr))
        ;; inside    (first (.args expr))
        all-rules? (every? true? (map #(= "Rule" (expr/head-str %)) (.args expr)))
        rules     (cond (some-> inside (.listQ)) (parse inside opts)
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

(defn parse-simple-atom
  "Parse an atomic expression (i.e. a primitive value). Return nil when not a simple atom."
  ([expr opts]
   (parse-simple-atom expr (proto/expr-primitive-type (jlink-instance/get) expr) opts))
  ([expr type opts]
   (case type
     :Expr/BIGINTEGER (.asBigInteger expr)
     :Expr/BIGDECIMAL (.asBigDecimal expr)
     :Expr/INTEGER (parse-integer expr)
     :Expr/REAL (.asDouble expr)
     :Expr/STRING (.asString expr)
     :Expr/RATIONAL (parse-rational expr)
     :Expr/SYMBOL (parse-symbol expr opts)
     nil)))

;; parameters list used to be: [expr & [type]] (??)
(defn parse-simple-vector [expr type {:keys [flags] :as opts}]
  (let [type (or type (simple-vector-type expr))]
    (if (and (options/flag?' flags flags/arrays)
             ;; TODO Why only these types? W. supports boolean, byte, char, short, int, long, float, double, String arrays;
             ;; Though only byte, short, int, float, double have "fast" methods
             (some #{:Expr/INTEGER :Expr/BIGINTEGER :Expr/REAL :Expr/BIGDECIMAL} #{type}))
      (.asArray expr (proto/->expr-type (jlink-instance/get) type) 1)
      (bound-map (fn [e _opts] (parse-simple-atom e type opts)) (.args expr) opts))))

(defn parse-simple-matrix [expr type opts]
  (let [type (or type (simple-matrix-type expr))]
    (bound-map (fn process-bound-map [a _opts]
                 (parse-simple-vector a type opts))
               (.args expr)
               opts)))

(defn parse-fn
  "Return a function that invokes the Wolfram expression `expr` (typically just a symbol naming a fn),
  converting any arguments given to it from Clojure to Wolfram and does the opposite conversion on the
  result.
  Ex.: `((parse/parse-fn 'Plus {:jlink-instance (jlink-instance/get)}) 1 2) ; => 3`

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
  (let [head (expr/head-str expr)]
    (cond (.bigIntegerQ expr)      (.asBigInteger expr)
          (.bigDecimalQ expr)      (.asBigDecimal expr)
          (.integerQ expr)         (parse-integer expr)
          (.realQ expr)            (.asDouble expr)
          (.stringQ expr)          (.asString expr)
          (.rationalQ expr)        (parse-rational expr)
          (.symbolQ expr)          (parse-symbol expr opts)
          (= "Association" head)   (parse-hash-map expr opts) #_(parse-generic-expression expr opts)
          (= "Function" head)      (parse-generic-expression expr opts)
          ;(if (and (options/flag?' flags :functions)
          ;         (not (options/flag?' flags :full-form)))
          ;  (parse-fn expr opts)
          ;  (parse-generic-expression expr opts))
          :else                    (parse-generic-expression expr opts))))

(defn standard-parse [expr {:keys [flags] :as opts}]
  (assert (proto/expr? (jlink-instance/get) expr))
  (cond
    ;(options/flag?' flags :as-function)                   (parse-fn expr opts)
    (or (atom? expr) (options/flag?' flags :full-form))   (parse-complex-atom expr opts)
    (simple-vector-type expr)                        (parse-simple-vector expr nil opts)
    (simple-matrix-type expr)                        (parse-simple-matrix expr nil opts)
    :else                                            (parse-complex-list expr opts)))

(ns-unmap *ns* 'custom-parse)
(defmulti custom-parse
  "Modify how Wolfram response is parsed into Clojure data.

  The dispatch-val should be a symbol, matched against the first one of the result list.

  Example:

  ```clj
   ; return a Java URL from a Hyperlink call, instead of `'(Hyperlink <label> <url string>)`
  (defmethod custom-parse 'Hyperlink [expr opts]
    (-> (second (.args expr)) ; 1st = label
        (parse/parse opts)
        java.net.URI.))

  (wl/eval '(Hyperlink \"foo\" \"https://www.google.com\"))
  ; => #object[java.net.URI 0x3f5e5a46 \"https://www.google.com\"]
  ```"
  #'custom-parse-dispatch)

(defmethod custom-parse :default [expr opts]
  (standard-parse expr opts))

(defn- unwrap-limit-size [jlink-expr {:keys [flags] :as opts}]
  ;; There are multiple cases here:
  ;; 1. We actually did evaluate and the data was small enough => it is returned => don't need to do anything
  ;; 2. We did evaluate and the data was too large => translate the Wolfram marker to a keyword & return
  ;; 3. Wolfram could not evaluate the expression and only did symbolic evaluation, returning it ± as-is but
  ;;    with the call to our wrapper fn => unwrap it [TODO: is this true?]
  ;; 4. We only do ->clj, i.e. no evaluation - `:wolframite.core/no-wolframite-wrapping` should have prevent us
  ;;   from wrapping it in the first place
  (if (and (not (options/flag?' flags flags/allow-large-data))
           (try (= internal-constants/WolframiteLargeData (parse-simple-atom jlink-expr opts))
                (catch Exception e
                  (log/warn e "Error checking whether the response is the WolframiteLargeData marker")
                  false)))
    :wolframite/large-data
    jlink-expr))

(defn parse
  "Low-level parsing function for parsing either the root or any sub-expression."
  [expr opts]
  (custom-parse expr opts))

(defn unwrapping-parse
  "Convert the jlink.Expr to a symbolic Clojure expression, after un-wrapping it from extra stuff that
  may have been added by Wolframite in `wolframite.base.convert/wrapping-convert`"
  [expr opts]
  (let [unwrapped (unwrap-limit-size expr opts)]
    ;; NOTE: `wolframite.impl.jlink-proto-impl/unchanged-expression?` is also wrapping-aware :'(
    (if (keyword? unwrapped)
      unwrapped
      (custom-parse expr opts))))
