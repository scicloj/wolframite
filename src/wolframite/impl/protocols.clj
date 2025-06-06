(ns wolframite.impl.protocols
  (:refer-clojure :exclude [list? number?])
  (:import (java.awt Component)))

(def type-bigdecimal
  "Keyword representing the corresponding atomic jlink.Expr type. See [[atomic-type]]."
  :Expr/BIGDECIMAL)
(def type-biginteger
  "Keyword representing the corresponding atomic jlink.Expr type. See [[atomic-type]]."
  :Expr/BIGINTEGER)
(def type-integer
  "Keyword representing the corresponding atomic jlink.Expr type. See [[atomic-type]]."
  :Expr/INTEGER)
(def type-rational
  "Keyword representing the corresponding atomic jlink.Expr type. See [[atomic-type]]."
  :Expr/RATIONAL)
(def type-real
  "Keyword representing the corresponding atomic jlink.Expr type. See [[atomic-type]]."
  :Expr/REAL)
(def type-string
  "Keyword representing the corresponding atomic jlink.Expr type. See [[atomic-type]]."
  :Expr/STRING)
(def type-symbol
  "Keyword representing the corresponding atomic jlink.Expr type. See [[atomic-type]]."
  :Expr/SYMBOL)

(def ^:private number-types #{type-bigdecimal
                              type-biginteger
                              type-integer
                              type-rational
                              type-real})

(defn number-type? [atomic-type-kw]
  (contains? number-types atomic-type-kw))

(defprotocol JLinkExpr
  "A protocol for `com.wolfram.jlink.Expr`, so that we can divorce the code from a direct dependency
  on JLink, so that it can be loaded even before the JLink jar is added to the classpath.
  (Though the methods fail at runtime until this is done.)
  "
  (args [this] "Same as .args, i.e. the remaining parts of the expression list")
  (as-array-1d [this element-type-kw] "Return this Expr as Java array with the given element type, such as [[type-integer]]")
  (as-number [this] "Calls .asBigInteger/Double/... or throws if not a number")
  (as-string [this] "If String/Symbol, returns it as str.")
  (atomic-type [this]
    "If this Expr represents an atomic type then return its type as a keyword,
      f.ex. :Expr/SYMBOL. Nil otherwise.
      See [[atomic-type]].")
  (head [this] "Same as .head, i.e. the first part of the expression list")
  (head-sym-str [this] "Returns the head as a string when it is a Symbol, otherwise nil")
  (list? [this])
  (failed? [this] "True if this is the `$Failed` symbol that Wolfram sometimes returns to indicate an error."))

(defprotocol JLink
  "A protocol to divorce the code from a direct dependency on JLink, so that it can be loaded
  even before the JLink jar is added to the classpath. (Though the methods fail at runtime
  until this is done.)

  NOTE: This is not a _good_ interface, because it wasn't designed but only extracted
  from the existing code."
  (create-kernel-link [this kernel-link-opts])
  (terminate-kernel! [this])
  (evaluate! [this expr] "Evaluate the given JLink Expr in the kernel")
  (expr
    [this]
    [this primitive-or-exprs]
    [this type name]
    "Create a JLink Expr

    * [primitive-or-exprs] a primitive value (long, Long, BigDecimal/Int., String) or a sequence
                           of 1+ jlink Exprs (which is interpreted as a fn call).
    * [type name] where type=:Expr/SYMBOL - create a Wolfram symbol")
  (->expr [this obj] "Turn the given obj into a jlink Expr via the loopback link 🤷")
  (expr? [this expr] "Is `expr` an instance of jlink Expr?")
  (expr-element-type [this container-type expr] "Return the type of elements in an array/list/...")
  (kernel-link [_this])
  (kernel-link? [_this])
  (^Component make-math-canvas!
    [this]
    [this kernel-link])
  (jlink-package-name [this]))

(extend-protocol JLink
  nil
  (create-kernel-link [this kernel-link-opts]
    (throw (IllegalStateException. "JLink not loaded!")))
  (terminate-kernel! [this]
    (throw (IllegalStateException. "JLink not loaded!")))
  (evaluate! [this expr]
    (throw (IllegalStateException. "JLink not loaded!")))
  (expr [this expr-coll]
    (throw (IllegalStateException. "JLink not loaded!")))
  (expr [this type expr-coll]
    (throw (IllegalStateException. "JLink not loaded!")))
  (->expr [this obj]
    (throw (IllegalStateException. "JLink not loaded!")))
  (expr? [this x]
    (throw (IllegalStateException. "JLink not loaded!")))
  (expr-element-type [this container-type expr]
    (throw (IllegalStateException. "JLink not loaded!")))
  (expr-primitive-type [this expr]
    (throw (IllegalStateException. "JLink not loaded!")))
  (kernel-link [_this]
    (throw (IllegalStateException. "JLink not loaded!")))
  (kernel-link? [_this]
    (throw (IllegalStateException. "JLink not loaded!")))
  (^Component make-math-canvas! [this]
    (throw (IllegalStateException. "JLink not loaded!")))
  (^Component make-math-canvas! [this kernel-link]
    (throw (IllegalStateException. "JLink not loaded!")))
  (jlink-package-name [this]
      (throw (IllegalStateException. "JLink not loaded!"))))
