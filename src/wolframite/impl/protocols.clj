(ns wolframite.impl.protocols)

(defprotocol JLink
  "A protocol to divorce the code from a direct dependency on JLink, so that it can be loaded
  even before the JLink jar is added to the classpath. (Though th methods fail at runtime until this
  is done.)"
  (create-kernel-link [this kernel-link-opts])
  (terminate-kernel! [this])
  (expr
    [this expr-coll]
    [this type expr-coll])
  (->expr [this obj])
  (expr? [this x])
  (expr-element-type [this container-type expr])
  (->expr-type [this type-kw])
  (kernel-link? [_this x]))

(extend-protocol JLink
  nil
  ;; TODO impl all
  (create-kernel-link [_] (throw (IllegalStateException. "JLink not loaded!"))))