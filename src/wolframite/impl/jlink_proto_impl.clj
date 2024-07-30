(ns wolframite.impl.jlink-proto-impl
  "The 'real' implementation of JLink, which does depend on JLink classes and thus
  cannot be loaded/required until JLink is on the classpath."
  (:require [clojure.string :as str]
            [wolframite.impl.protocols :as proto])
  (:import (clojure.lang BigInt)
           [com.wolfram.jlink Expr KernelLink MathCanvas MathLinkException MathLinkFactory]))

(defn- array? [x]
  (some-> x class .isArray))

(defn- make-expr [primitive-or-exprs]
  (try
    (cond
      (sequential? primitive-or-exprs)
      (Expr.
       ^Expr (first primitive-or-exprs)
       ^"[Lcom.wolfram.jlink.Expr;" (into-array Expr (rest primitive-or-exprs)))
      ;; Here, primitive-or-exprs could be an int, a String, long[], or similar

      (array? primitive-or-exprs)
      (Expr. primitive-or-exprs)

      (string? primitive-or-exprs)
      (Expr. ^String primitive-or-exprs)

      (number? primitive-or-exprs)
      (condp = (type primitive-or-exprs)
        Long    (Expr. ^long (.longValue primitive-or-exprs))
        Double  (Expr. ^double (.doubleValue primitive-or-exprs))
        Integer (Expr. ^int (.intValue primitive-or-exprs))
        Float   (Expr. ^float (.floatValue primitive-or-exprs))
        Short   (Expr. ^short (.shortValue primitive-or-exprs))
        BigDecimal (Expr. ^BigDecimal primitive-or-exprs)
        BigInt (Expr. (.toBigInteger ^BigInt primitive-or-exprs))
        BigInteger (Expr. ^BigInteger primitive-or-exprs))

      :else
      (throw (IllegalArgumentException. (str "Unsupported primitive value of type "
                                             (type primitive-or-exprs)
                                             " Value " primitive-or-exprs))))

    (catch Exception e
      (throw (ex-info (str "Failed to create an expression from "
                           primitive-or-exprs
                           " Caused by: "
                           (ex-message e)
                           (when (sequential? primitive-or-exprs)
                             " All elements in the sequence must be instances of the 'Expr' class."))
                      {:argument primitive-or-exprs
                       :types (if (sequential? primitive-or-exprs)
                                (map type primitive-or-exprs)
                                (type primitive-or-exprs))
                       :cause e})))))

(defrecord JLinkImpl [opts kernel-link-atom]
  proto/JLink
  (create-kernel-link [_this kernel-link-opts]
    (loop [attempts 3, wait-ms 10, orig-err nil]
      ;; Sometimes, starting a link may fail b/c the previous one
      ;; has not been shut down properly
      (let [res
            (try (let [opts-array (into-array String kernel-link-opts)
                       kernel-link
                       (->> (doto (MathLinkFactory/createKernelLink ^"[Ljava.lang.String;" opts-array)
                              (.discardAnswer))
                            (reset! kernel-link-atom))]
                   ;(.getError kernel-link) (.getErrorMessage kernel-link)
                   kernel-link)
                 (catch MathLinkException e
                   (if (= (ex-message e) "MathLink connection was lost.")
                     (throw (ex-info (str "MathLink connection was lost. Perhaps you need to activate Mathematica first,"
                                          " you are trying to start multiple concurrent connections (from separate REPLs),"
                                          " or there is some other issue and you need to retry, or restart and retry...")
                                     {:kernel-link-opts (cond-> kernel-link-opts
                                                          (array? kernel-link-opts)
                                                          vec)
                                      :cause e}))
                     (throw e)))
                 (catch Exception e
                   (throw (ex-info (str "Failed to start a Math/Wolfram Kernel process: "
                                        (ex-message e)
                                        " Verify the settings are correct: `" kernel-link-opts "`")
                                   {:kernel-opts kernel-link-opts}))))]
        (if (instance? Exception res)
          (if (pos? attempts)
            (do (Thread/sleep wait-ms)
                (recur (dec attempts) (* 3 wait-ms) (or orig-err res)))
            (throw orig-err))
          res))))
  (terminate-kernel! [_this]
    ;; BEWARE: it is not absolutely guaranteed that the kernel will die immediately
    (doto ^KernelLink @kernel-link-atom
      (.terminateKernel)
      (.close))
    (reset! kernel-link-atom nil))
  (expr [_this primitive-or-exprs]
    (make-expr primitive-or-exprs))
  (expr [_this type name]
    (Expr. ^int (case type
                  :Expr/SYMBOL  Expr/SYMBOL)
           ^String (apply str (replace {\/ \`} name))))
  (->expr [_this obj]
    (.getExpr
     (doto (MathLinkFactory/createLoopbackLink)
       (.put obj)
       (.endPacket))))
  (expr? [_this x]
    (instance? Expr x))
  (expr-element-type [_this container-type expr]
    (case container-type
      :vector
      (cond (.vectorQ expr Expr/INTEGER)     :Expr/INTEGER
            (.vectorQ expr Expr/BIGINTEGER)  :Expr/BIGINTEGER
            (.vectorQ expr Expr/REAL)        :Expr/REAL
            (.vectorQ expr Expr/BIGDECIMAL)  :Expr/BIGDECIMAL
            (.vectorQ expr Expr/STRING)      :Expr/STRING
            (.vectorQ expr Expr/RATIONAL)    :Expr/RATIONAL
            (.vectorQ expr Expr/SYMBOL)      :Expr/SYMBOL
            :else                            nil)
      :matrix
      (cond (.matrixQ expr Expr/INTEGER)     :Expr/INTEGER
            (.matrixQ expr Expr/BIGINTEGER)  :Expr/BIGINTEGER
            (.matrixQ expr Expr/REAL)        :Expr/REAL
            (.matrixQ expr Expr/BIGDECIMAL)  :Expr/BIGDECIMAL
            (.matrixQ expr Expr/STRING)      :Expr/STRING
            (.matrixQ expr Expr/RATIONAL)    :Expr/RATIONAL
            (.matrixQ expr Expr/SYMBOL)      :Expr/SYMBOL
            :else                            nil)))
  (->expr-type [_this type-kw]
    (case type-kw
      :Expr/INTEGER Expr/INTEGER
      :Expr/BIGINTEGER Expr/BIGINTEGER
      :Expr/REAL Expr/REAL
      :Expr/BIGDECIMAL Expr/BIGDECIMAL
      :Expr/STRING Expr/STRING
      :Expr/RATIONAL Expr/RATIONAL
      :Expr/SYMBOL Expr/SYMBOL))

  (kernel-link [_this] @kernel-link-atom)
  (kernel-link? [_this]
    (some->> @kernel-link-atom (instance? KernelLink)))
  (make-math-canvas! [this]
    (proto/make-math-canvas! this (proto/kernel-link this)))
  (make-math-canvas! [_this kernel-link]
    (doto (MathCanvas. kernel-link)
      (.setUsesFE true)
      (.setImageType MathCanvas/GRAPHICS)))
  (jlink-package-name [_this]
    KernelLink/PACKAGE_CONTEXT))

(defn create [kernel-link-atom opts]
  (map->JLinkImpl
   {:opts opts
    :kernel-link-atom kernel-link-atom}))
