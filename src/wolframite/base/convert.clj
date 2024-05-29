(ns wolframite.base.convert
  "Convert a Clojure expression into a Wolfram JLink expression"
  (:require [wolframite.impl.jlink-instance :as jlink-instance]
            [wolframite.impl.protocols :as proto]
            [wolframite.runtime.jlink]
            [wolframite.lib.options :as options]
            [wolframite.base.express :as express]
            [wolframite.base.expr :as expr]
            [wolframite.runtime.defaults :as defaults]))

;; (remove-ns 'wolframite.base.convert)

;; * defmulti and dispatch

(defn- dispatch [obj]
  (cond (and (list? obj)
             (empty? obj))   :null
        (seq? obj)           :expr
        (or (vector? obj)
            (list? obj))     :list
        (ratio? obj)         :rational
        (map? obj)           :hash-map
        (symbol? obj)        :symbol
        (nil? obj)           :null
        :else                nil))

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
  (proto/->expr @jlink-instance/jlink-instance obj))

(defmethod convert :null [_ opts]
  (convert 'Null opts))

(defmethod convert :rational [n opts]
  (convert (list 'Rational (.numerator n) (.denominator n)) opts))

(defmethod convert :hash-map [map {:keys [flags] :as opts}]
  (if (options/flag?' flags :hash-maps)
    (convert (apply list 'Association (for [[key value] map] (list 'Rule key value))) opts)
    (convert (seq map) opts)))

(defmethod convert :symbol [sym {:keys [aliases] :as opts}]
  (let [all-aliases (merge defaults/all-aliases aliases)]
    (if-let [alias (all-aliases sym)]
      (convert alias opts)
      (if-let [[_ ^String n] (re-matches #"%(\d*)" (str sym))]
        (let [n (Long/valueOf (if (= "" n) "1" n))]
          (convert (list 'Slot n) opts))
        ;(let [s (str-utils/replace (str sym) #"\|(.*?)\|" #(str "\\\\[" (second %) "]"))]   )
        (let [s (str sym)]
          (if (re-find #"[^a-zA-Z0-9$\/]" s)
            (throw (Exception. (str "Symbols passed to Mathematica must be alphanumeric (apart from forward slashes and dollar signs). Passed: " s)))
            (proto/expr @jlink-instance/jlink-instance :Expr/SYMBOL s)))))))

(defmethod convert :list [coll opts]
  (cond (simple-matrix? coll opts) (convert (to-array-2d coll) opts)
        (simple-vector? coll opts) (convert (to-array coll) opts)
        :else                      (convert (to-array (map #(if dispatch (convert % opts) %)
                                                           coll))
                                            opts)))

(defmethod convert :expr [cexpr opts]
  (let [macro (first cexpr)
        arg   (second cexpr)]
    (cond (= 'clojure.core/deref macro)    (convert (cexpr-from-prefix-form arg) opts)
          (= 'clojure.core/meta macro)     (convert (cexpr-from-postfix-form arg) opts)
          (= 'var macro)                   (convert (list 'Function arg) opts)
          (= 'quote macro)                 (express/express arg opts)
          :else                            (expr/expr-from-parts (map #(convert % opts) cexpr)))))
