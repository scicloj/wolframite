(ns wolframite.impl.jlink-proto-impl
  "The 'real' implementation of JLink, which does depend on JLink classes and thus
  cannot be loaded/required until JLink is on the classpath."
  (:require [clojure.string :as str]
            [wolframite.impl.protocols :as proto])
  (:import [com.wolfram.jlink Expr KernelLink MathCanvas MathLinkException MathLinkFactory]))

(defn- array? [x]
  (-> x class str (str/starts-with? "class [L")))

(defrecord JLinkImpl [opts kernel-link-atom]
  proto/JLink
  (create-kernel-link [_this kernel-link-opts]
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
                           {:kernel-opts kernel-link-opts})))))
  (terminate-kernel! [_this]
    (.terminateKernel ^KernelLink @kernel-link-atom)
    (reset! kernel-link-atom nil))
  (expr [_this primitive-or-exprs]
    (if (sequential? primitive-or-exprs)
      (try (Expr.
            ^Expr (first primitive-or-exprs)
            ^"[Lcom.wolfram.jlink.Expr;" (into-array Expr (rest primitive-or-exprs)))
           (catch Exception e
             (throw (ex-info (str "Failed to create an expression from "
                                  primitive-or-exprs
                                  "caused by "
                                  (ex-message e)
                                  "All elements in the sequence must be instances of the 'Expr' class.")
                             {:argument primitive-or-exprs
                              :cause e
                              :types (map type primitive-or-exprs)}))))
      ;; Here, primitive-or-exprs could be an int, a String, long[], or similar
      (Expr. primitive-or-exprs)))
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
