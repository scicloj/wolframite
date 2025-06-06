(ns wolframite.base.parse
  "Translate a jlink.Expr returned from an evaluation into Clojure data"
  (:require
    [clojure.set :as set]
    [wolframite.flags :as flags]
    [wolframite.impl.jlink-instance :as jlink-instance]
    [wolframite.impl.protocols :as proto]
    [wolframite.lib.options :as options]))

(declare parse)

;(defn pascal->kebab
;  [s]
;  (some-> s
;          (str/replace #"\w(\p{Upper})" (fn [[[p l] _]] (str  p "-" (str/lower-case l))))
;          str/lower-case))

;(defn entity-type->keyword [expr opts]
;  (let [head (pascal->kebab (proto/head-sym-str expr))
;        parts (reduce (fn [acc part]
;                        (into acc (cond
;                                    (string? part) [(pascal->kebab part)]
;                                    (coll? part) (reverse (map pascal->kebab part))
;                                    :else nil)))
;                      [head]
;                      (map #(parse % opts) (proto/args expr)))]
;    ;; BAIL: if any of the parts are not recognized,
;    ;; let it go through normal parsing
;    (when (not-any? nil? parts)
;      (keyword (str/join "." (butlast parts))
;               (last parts)))))

(defn custom-parse-dispatch [expr _opts]
  (some-> expr proto/head-sym-str symbol))

(defn atom? [expr]
  (not (proto/list? expr)))

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
  (bound-map parse (proto/args expr) opts))

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
        s       (proto/as-string expr)
        sym     (symbol (apply str (replace {\` \/} s)))]
    (if-let [alias (get wolf->alias sym)]
      alias
      (case s
        "True" true
        "False" false
        "Null" nil
        sym))))

(defn parse-hash-map [expr opts]
  (let [inside    (first (proto/args expr))
        all-rules? (empty? (remove #(#{"Rule" "RuleDelayed"} (proto/head-sym-str %)) (proto/args expr)))
        rules     (cond (some-> inside proto/list?) (parse inside opts)
                        all-rules? (into {}
                                         (map (fn [kv]
                                                (let [head (proto/head-sym-str kv)
                                                      [k v] (seq kv)]
                                                 [(parse k opts)
                                                  (cond-> (parse v opts)
                                                          (= head "RuleDelayed")
                                                          (with-meta {:wolfram/delayed true}))])))
                                         (proto/args expr))
                        (= "Dispatch" (proto/head-sym-str inside)) (parse (first (proto/args inside)) opts)
                        :else (assert (or (proto/list? inside)
                                          (= "Dispatch" (proto/head-sym-str inside)))))
        keys      (map second rules)
        vals      (map (comp second #(nth % 2)) rules)]
    (if (map? rules)
      rules
      (zipmap keys vals))))

(defn parse-simple-atom [expr type opts]
  (let [atomic-type (proto/atomic-type expr)]
    (cond (proto/number? expr) (proto/as-number expr)
          (= atomic-type proto/type-string) (proto/as-string expr)
          (= atomic-type proto/type-symbol) (parse-symbol expr opts))))

;; parameters list used to be: [expr & [type]] (??)
(defn parse-simple-vector [expr type {:keys [flags] :as opts}]
  (let [type (or type (simple-vector-type expr))]
    (if (and (options/flag?' flags flags/arrays)
             ;; TODO Why only these types? W. supports boolean, byte, char, short, int, long, float, double, String arrays;
             ;; Though only byte, short, int, float, double have "fast" methods
             (some #{:Expr/INTEGER :Expr/BIGINTEGER :Expr/REAL :Expr/BIGDECIMAL} #{type}))
      (proto/as-array-1d expr type)
      (bound-map (fn [e _opts] (parse-simple-atom e type opts)) (proto/args expr) opts))))

(defn parse-simple-matrix [expr type opts]
  (let [type (or type (simple-matrix-type expr))]
    (bound-map (fn process-bound-map [a _opts]
                 (parse-simple-vector a type opts))
               (proto/args expr)
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
      (into (map #(parse % opts) (rseq (vec (proto/args expr)))))
      (conj (parse (proto/head expr) opts))))

(defn parse-complex-atom [expr opts]
  (let [head (proto/head-sym-str expr)
        maybe-type-kw (proto/atomic-type expr)]
    (cond (proto/number? expr) (proto/as-number expr)
          (= maybe-type-kw proto/type-string) (proto/as-string expr)
          (= maybe-type-kw proto/type-symbol) (parse-symbol expr opts)
          (= "Association" head) (parse-hash-map expr opts) #_(parse-generic-expression expr opts)
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
    (-> (second (proto/args expr)) ; 1st = label
        (parse/parse opts)
        java.net.URI.))

  (wl/eval '(Hyperlink \"foo\" \"https://www.google.com\"))
  ; => #object[java.net.URI 0x3f5e5a46 \"https://www.google.com\"]
  ```"
  #'custom-parse-dispatch)

(defmethod custom-parse :default [expr opts]
  (standard-parse expr opts))

(defn parse [expr opts]
  (custom-parse expr opts))
