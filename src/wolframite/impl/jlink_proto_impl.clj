(ns wolframite.impl.jlink-proto-impl
  "The 'real' implementation of JLink, which does depend on JLink classes and thus
  cannot be loaded/required until JLink is on the classpath."
  (:require [clojure.string :as str]
            [wolframite.impl.protocols :as proto])
  (:import [com.wolfram.jlink Expr KernelLink MathCanvas MathLinkFactory]))

(defn- array? [x]
  (-> x class str (str/starts-with? "class [L")))

(defrecord JLinkImpl [opts kernel-link-atom]
  proto/JLink
  (create-kernel-link [_this kernel-link-opts]
    (try (->> (doto (com.wolfram.jlink.MathLinkFactory/createKernelLink kernel-link-opts)
                (.discardAnswer))
              (reset! kernel-link-atom))
         (catch com.wolfram.jlink.MathLinkException e
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
    (.terminateKernel ^com.wolfram.jlink.KernelLink @kernel-link-atom)
    (reset! kernel-link-atom nil))
  (expr [_this expr-coll]
    (com.wolfram.jlink.Expr.
      (first expr-coll)
      (into-array com.wolfram.jlink.Expr (rest expr-coll))))
  (expr [_this type expr-coll]
    (-> (case type
          :Expr/SYMBOL Expr/SYMBOL)
        (Expr. (apply str (replace {\/ \`} expr-coll)))))
  (->expr [_this obj]
    (.getExpr
      (doto (MathLinkFactory/createLoopbackLink)
        (.put obj)
        (.endPacket))))
  (expr? [_this x]
    (instance? com.wolfram.jlink.Expr x))
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
      :Expr/SYMBOL Expr/SYMBOL
      ))
  (kernel-link [_this] @kernel-link-atom)
  (kernel-link? [_this]
    (some->> @kernel-link-atom (instance? com.wolfram.jlink.KernelLink)))
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