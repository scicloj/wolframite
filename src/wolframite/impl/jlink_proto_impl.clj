(ns wolframite.impl.jlink-proto-impl
  "The 'real' implementation of JLink, which does depend on JLink classes and thus
  cannot be loaded/required until JLink is on the classpath."
  (:require [clojure.tools.logging :as log]
            [wolframite.impl.internal-constants :as internal-constants]
            [wolframite.impl.protocols :as proto])
  (:import (clojure.lang BigInt)
           [com.wolfram.jlink Expr KernelLink MathCanvas MathLink MathLinkException MathLinkFactory
                              PacketListener PacketArrivedEvent PacketPrinter]))

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

(defrecord InfoPacketCaptureListener [capture]
  ;; A packet listener that enables us to get hold of the normally ignored Print outputs
  ;; and warning messages sent before a return packet.
  PacketListener
  (packetArrived [_this #_PacketArrivedEvent event]
    (let [link (cast KernelLink (.getSource event))]
      (some->>
        (condp = (.getPktType event) ; note: `case` doesn't work 🤷
          MathLink/TEXTPKT
          {:type :text :content (.getString link)}

          MathLink/MESSAGEPKT
          (let [expr (.getExpr link)]
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

;; Wolfram sometimes indicates failure by returning the symbol $Failed
(defonce failed-expr (Expr. Expr/SYMBOL "$Failed"))

(defn- unchanged-expression? [^Expr input, ^Expr output]
  (let [size-wrapper? (= (some-> input (.head) str)
                         (name internal-constants/wolframiteLimitSize))
        unwrapped-input (if size-wrapper?
                          (-> input (.args) first)
                          input)]
   (= unwrapped-input output)))

(defn- evaluate! [^KernelLink link packet-capture-atom ^Expr expr]
  (assert link "Kernel link not initialized?!")
  (io!
    (locking link
      (reset! packet-capture-atom nil)
      ;; NOTE: There is also evaluateToImage => byte[] of GIF for graphics-returning fns such as Plot
      ;; NOTE 2: .waitForAnswer discard packets until ReturnPacket; our packet-listener collects those
      (doto link (.evaluate expr) (.waitForAnswer))
      (let [res (.getExpr link)
            messages (seq (first (reset-vals! packet-capture-atom nil)))
            messages-text (mapv :content messages)]
        (cond
          (and (seq messages)
               (or (= res failed-expr)
                   (unchanged-expression? expr res)))
          ;; If input expr == output expr, this usually means the evaluation failed
          ;; (or there was nothing to do); if there are also any extra text/message packets
          ;; then it most likely has failed, and those messages explain what was wrong
          (throw (ex-info (str "Evaluation seems to have failed. Result: "
                               res
                               " Details: "
                               (cond-> messages-text
                                       (= 1 (count messages-text))
                                       first))
                          {:expr expr
                           :messages messages
                           :result res}))

          (= res failed-expr) ; but no messages
          (throw (ex-info (str "Evaluation has failed. Result: "
                               res
                               " No details available.")
                          {:expr expr :result res}))

          :else
          (do (when (seq messages)
                (log/info "Messages retrieved during evaluation:" messages-text))
              res))))))

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
  (expr-primitive-type [_this expr]
    (cond (.bigDecimalQ expr) :Expr/BIGDECIMAL
          (.bigIntegerQ expr) :Expr/BIGINTEGER
          (.integerQ expr) :Expr/INTEGER
          (.rationalQ expr) :Expr/RATIONAL
          (.realQ expr) :Expr/REAL
          (.stringQ expr) :Expr/STRING
          (.symbolQ expr) :Expr/SYMBOL
          :else nil))
  (->expr-type [_this type-kw]
    (case type-kw
      :Expr/BIGDECIMAL Expr/BIGDECIMAL
      :Expr/BIGINTEGER Expr/BIGINTEGER
      :Expr/INTEGER Expr/INTEGER
      :Expr/RATIONAL Expr/RATIONAL
      :Expr/REAL Expr/REAL
      :Expr/STRING Expr/STRING
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
    :kernel-link-atom kernel-link-atom
    :packet-listener (->InfoPacketCaptureListener (atom nil))}))
