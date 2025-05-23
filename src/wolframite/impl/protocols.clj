(ns wolframite.impl.protocols
  (:import (java.awt Component)))

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
  (expr? [this x])
  (expr-element-type [this container-type expr]
    "Return the type of elements in the given vector/matrix `container`, as a keyword. See also [[expr-primitive-type]].")
  (expr-primitive-type [this expr]
    "Return the type of the expression as a keyword, such as `:Expr/SYMBOL` for `com.wolfram.jlink.Expr/SYMBOL`.
    Only works for primitive expressions and not lists and other containers.
    See also [[->expr-type]], which turns such keyword into the corresponding constant in Expr.")
  (->expr-type [this type-kw]
    "Return one of the primitive type constants in Expr, given corresponding keyword, such as `com.wolfram.jlink.Expr/SYMBOL` for `:Expr/SYMBOL`.")
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
  (->expr-type [this type-kw]
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
