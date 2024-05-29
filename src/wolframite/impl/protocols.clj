(ns wolframite.impl.protocols
  (:import (java.awt Component)))

(defprotocol JLink
  "A protocol to divorce the code from a direct dependency on JLink, so that it can be loaded
  even before the JLink jar is added to the classpath. (Though th methods fail at runtime until this
  is done.)

  NOTE: This is not a _good_ interface, because it wasn't designed but only extracted
  from the existing code."
  (create-kernel-link [this kernel-link-opts])
  (terminate-kernel! [this])
  (expr
    [this expr-coll]
    [this type expr-coll])
  (->expr [this obj])
  (expr? [this x])
  (expr-element-type [this container-type expr])
  (->expr-type [this type-kw])
  (kernel-link? [_this x])
  (^Component make-math-canvas! [this kernel-link])
  (jlink-package-name [this]))

(extend-protocol JLink
  nil
  (create-kernel-link [this kernel-link-opts]
    (throw (IllegalStateException. "JLink not loaded!")))
  (terminate-kernel! [this]
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
  (->expr-type [this type-kw]
    (throw (IllegalStateException. "JLink not loaded!")))
  (kernel-link? [_this x]
    (throw (IllegalStateException. "JLink not loaded!")))
  (^Component make-math-canvas! [this kernel-link]
    (throw (IllegalStateException. "JLink not loaded!")))
  (jlink-package-name [this]
    (throw (IllegalStateException. "JLink not loaded!"))))