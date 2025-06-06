(ns wolframite.impl.jlink-proto-impl
  "The 'real' implementation of JLink, which does depend on JLink classes and thus
  cannot be loaded/required until JLink is on the classpath."
  (:require [clojure.tools.logging :as log]
            [wolframite.impl.error-detection :as error-detection]
            [wolframite.impl.internal-constants :as internal-constants]
            [wolframite.impl.protocols :as proto])
  (:import (clojure.lang BigInt)
           [com.wolfram.jlink Expr KernelLink MathCanvas MathLink MathLinkException MathLinkFactory
                              PacketListener PacketArrivedEvent PacketPrinter]))

(defn parse-integer [^Expr expr]
  (let [i (.asLong expr)]
    (if (and (<= i Integer/MAX_VALUE)
             (>= i Integer/MIN_VALUE))
      (int i)
      (long i))))

(defn parse-rational [^Expr expr]
  (let [numer (parse-integer (.part expr 1))
        denom (parse-integer (.part expr 2))]
    (/ numer denom)))

(defn- type-kwd->jlink-int [type-kw]
  (condp = type-kw
    proto/type-bigdecimal Expr/BIGDECIMAL
    proto/type-biginteger Expr/BIGINTEGER
    proto/type-integer Expr/INTEGER
    proto/type-rational Expr/RATIONAL
    proto/type-real Expr/REAL
    proto/type-string Expr/STRING
    proto/type-symbol Expr/SYMBOL
    (throw (IllegalArgumentException. (str "Unsupported/unknown type keyword " type-kw)))))

;; Wolfram sometimes indicates failure by returning the symbol $Failed
(defonce failed-expr (Expr. Expr/SYMBOL "$Failed"))

(extend-protocol proto/JLinkExpr
  Expr
  (args [this] (.args this))
  (as-array-1d [this elemen-type-kw]
    (.asArray this (type-kwd->jlink-int elemen-type-kw) 1))
  (as-number [expr]
    (cond (.bigIntegerQ expr)   (.asBigInteger expr)
          (.bigDecimalQ expr)   (.asBigDecimal expr)
          (.integerQ expr)      (parse-integer expr)
          (.realQ expr)         (.asDouble expr)
          (.rationalQ expr)     (parse-rational expr)
          :else (throw (IllegalArgumentException. "Not a number"))))
  (as-string [this]
    (.asString this)) ; Only works for Symbol, String; throws otherwise
  (atomic-type [this]
    (cond (.bigDecimalQ this) proto/type-bigdecimal
          (.bigIntegerQ this) proto/type-biginteger
          (.integerQ this) proto/type-integer
          (.rationalQ this) proto/type-rational
          (.realQ this) proto/type-real
          (.stringQ this) proto/type-string
          (.symbolQ this) proto/type-symbol
          :else nil))
  (head [this] (.head this))
  (head-sym-str [this]
    (let [head (.head this)]
      ;; JLink throws if not symbol / string
      (.asString head)))
  (list? [this] (.listQ this))
  (failed? [this] (= this failed-expr)))

(defn- array? [x]
  (some-> x class .isArray))

(defn- make-expr [primitive-or-exprs]
  (try
    (cond
      (sequential? primitive-or-exprs)
      (Expr.
       ^Expr (first primitive-or-exprs)
       ^Expr/1 (into-array Expr (rest primitive-or-exprs)))
      ;; Here, primitive-or-exprs could be an int, a String, long[], or similar

      (array? primitive-or-exprs)
      (Expr. primitive-or-exprs) ; could be one of many array types, so reflection necessary...

      (string? primitive-or-exprs)
      (Expr. ^String primitive-or-exprs)

      (number? primitive-or-exprs)
      (condp = (type primitive-or-exprs)
        Long    (Expr. ^long (.longValue ^Long primitive-or-exprs))
        Double  (Expr. ^double (.doubleValue ^Double primitive-or-exprs))
        Integer (Expr. ^int (.intValue ^Integer primitive-or-exprs))
        Float   (Expr. ^float (.floatValue ^Float primitive-or-exprs))
        Short   (Expr. ^short (.shortValue ^Short primitive-or-exprs))
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

(defrecord InfoPacketCaptureListener [capture]
  ;; A packet listener that enables us to get hold of the normally ignored Print outputs
  ;; and warning messages sent before a return packet.
  PacketListener
  (packetArrived [_this #_PacketArrivedEvent event]
    (let [link ^KernelLink (cast KernelLink (.getSource event))]
      (some->>
        (condp = (.getPktType event) ; note: `case` doesn't work 🤷
          MathLink/TEXTPKT
          {:type :text :content (.getString link)}

          MathLink/MESSAGEPKT
          (let [expr ^Expr (.getExpr link)]
            (when-not (.symbolQ expr)
              ;; not sure why these are sent, not useful; e.g. Get when a Get call failed etc.
             {:type :message :content expr}))

          nil)
        (swap! capture conj)))
    true))

(comment
  (let [link (proto/kernel-link ((requiring-resolve 'wolframite.impl.jlink-instance/get)))]
    ;(.removePacketListener link packet-listener)
    (.addPacketListener link packet-listener)
    ,)
  ,)

(defn install-packet-logger!
  "Call this to help debug your program - it will print all incoming JLink packets (the units
  of communication between JVM and Wolfram) to stdout.

  Ex.:
  ```clj
  (install-packet-logger! (proto/kernel-link (jlink-instance/get)))
  ```"
  [^KernelLink link]
  (.addPacketListener link (PacketPrinter. System/out)))

(defn- evaluate! [^KernelLink link packet-capture-atom ^Expr expr]
  (assert link "Kernel link not initialized?!")
  (io!
    (locking link
      (reset! packet-capture-atom nil)
      ;; NOTE: There is also evaluateToImage => byte[] of GIF for graphics-returning fns such as Plot
      ;; NOTE 2: .waitForAnswer discard packets until ReturnPacket; our packet-listener collects those
      (doto link (.evaluate expr) (.waitForAnswer))
      (error-detection/ensure-no-eval-error
        expr
        (.getExpr link)
        (seq (first (reset-vals! packet-capture-atom nil)))))))

(defrecord JLinkImpl [opts kernel-link-atom packet-listener]
  proto/JLink
  (create-kernel-link [_this kernel-link-opts]
    ;; BEWARE: The fact that createKernelLink() succeeds does not mean that the link is connected
    ;; and functioning properly. There is a difference between creating a link (which involves
    ;; setting up your side) and connecting one (which verifies that the other side is alive and well).
    ;; WSTP will automatically try to connect it the first time you try to read or write something.
    (loop [attempts 3, wait-ms 10, orig-err nil]
      ;; Sometimes, starting a link may fail b/c the previous one
      ;; has not been shut down properly
      (let [res
            (try (let [opts-array (into-array String kernel-link-opts)
                       kernel-link
                       (->> (doto (MathLinkFactory/createKernelLink ^String/1 opts-array)
                              (.addPacketListener packet-listener) ; TBD doesn't get anything when link fails due to e.g. # kernels > license
                              ;; Note: The call below ensures we actually try to connect to the kernel
                              (.discardAnswer))
                            (reset! kernel-link-atom))]
                   ;(.getError kernel-link) (.getErrorMessage kernel-link)
                   kernel-link)
                 (catch MathLinkException e
                   (if (= (ex-message e) "MathLink connection was lost.")
                     (throw (ex-info (str "MathLink connection was lost. Perhaps you need to activate Mathematica first,"
                                          " you are trying to start multiple concurrent connections (from separate REPLs),"
                                          " or there is some other issue and you need to retry, or restart and retry."
                                          " You may want to try running Wolfram Kernel from the command line to learn more.")
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
    (when-let [link ^KernelLink @kernel-link-atom]
      (doto link
        (.terminateKernel)
        (.close)))
    (reset! kernel-link-atom nil))
  (evaluate! [_this expr]
    (evaluate! @kernel-link-atom (:capture packet-listener) expr))
  (expr [_this primitive-or-exprs]
    (make-expr primitive-or-exprs))
  (expr [_this type name]
    (Expr. ^int (case type
                  :Expr/SYMBOL  Expr/SYMBOL)
           ^String (apply str (replace {\/ \`} name))))
  (->expr [_this obj] ; fallback for transforming anything we don't handle manually, via JLink itself
    (.getExpr
     (doto (MathLinkFactory/createLoopbackLink)
       (.put obj)
       (.endPacket))))
  (expr? [_this x]
    (instance? Expr x))
  (expr-element-type [_this container-type expr]
    (let [expr ^Expr expr]
     (case container-type
       :vector
       (cond (.vectorQ expr Expr/BIGDECIMAL) proto/type-bigdecimal
             (.vectorQ expr Expr/BIGINTEGER) proto/type-biginteger
             (.vectorQ expr Expr/INTEGER) proto/type-integer
             (.vectorQ expr Expr/RATIONAL) proto/type-rational
             (.vectorQ expr Expr/REAL) proto/type-real
             (.vectorQ expr Expr/STRING) proto/type-string
             (.vectorQ expr Expr/SYMBOL) proto/type-symbol
             :else nil)
       :matrix
       (cond (.matrixQ expr Expr/BIGDECIMAL) proto/type-bigdecimal
             (.matrixQ expr Expr/BIGINTEGER) proto/type-biginteger
             (.matrixQ expr Expr/INTEGER) proto/type-integer
             (.matrixQ expr Expr/RATIONAL) proto/type-rational
             (.matrixQ expr Expr/REAL) proto/type-real
             (.matrixQ expr Expr/STRING) proto/type-string
             (.matrixQ expr Expr/SYMBOL) proto/type-symbol
             :else nil))))
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
    :kernel-link-atom kernel-link-atom
    :packet-listener (->InfoPacketCaptureListener (atom nil))}))
