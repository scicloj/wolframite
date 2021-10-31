(ns clojuratica.base.evaluate
  (:require [clojuratica.lib.debug :as debug]
            [clojuratica.lib.options :as options]
            [clojuratica.base.convert :as convert]
            [clojuratica.runtime.default-options :as default-options]
            [clojuratica.runtime.dynamic-vars :as dynamic-vars]))

(declare process-state queue-run-or-wait)

(defn evaluate [expr]
  (let [kernel-link (dynamic-vars/*kernel* :link)]
    (assert (instance? com.wolfram.jlink.Expr       expr))
    (assert (instance? com.wolfram.jlink.KernelLink kernel-link))
    (when (options/flag? dynamic-vars/*options* :verbose) (println "evaluate expr>" expr))
    (if (options/flag? dynamic-vars/*options* :serial)
      (io!
        (locking kernel-link
          (doto kernel-link (.evaluate expr) (.waitForAnswer))
          (.getExpr kernel-link)))
      (options/binding-options [dynamic-vars/*options* [:serial] dynamic-vars/*options*] _
        (let [pid-expr (evaluate (convert/convert '(Unique Clojuratica/Concurrent/process)))]
          (when (options/flag? dynamic-vars/*options* :verbose) (println "pid-expr:" pid-expr))
          (evaluate (convert/convert (list '= pid-expr (list 'ParallelSubmit expr))))
          (evaluate (convert/convert '(QueueRun)))
          (loop []
            (let [[state result] (process-state pid-expr)]
              (if (not= :finished state)
                (do
                  (queue-run-or-wait)
                  (recur))
                (do
                  (evaluate (convert/convert (list 'Remove pid-expr)))
                  result)))))))))

(defn process-state [pid-expr]
  (assert (options/flag? dynamic-vars/*options* :serial))
  (let [state-expr    (evaluate (convert/convert (list 'ProcessState pid-expr)))
        state-prefix  (first (.toString state-expr))]
    (cond (= \r state-prefix) [:running nil]
          (= \f state-prefix) [:finished (.part state-expr 1)]
          (= \q state-prefix) [:queued nil]
          true
            (throw (Exception. (str "Error! State unrecognized: " state-expr))))))

(defn queue-run-or-wait []
  (assert (options/flag? dynamic-vars/*options* :serial))
  (let [lqr-atom (dynamic-vars/*kernel* :latest-queue-run-time)
        lqr-time @lqr-atom
        nano-pi  (* 1000000 (dynamic-vars/*options* :poll-interval))
        run-in   (when lqr-time (- (+ lqr-time nano-pi) (System/nanoTime)))]
    (if (or (nil? run-in) (neg? run-in))
      (do
        (when (options/flag? dynamic-vars/*options* :verbose) (println "QueueRunning at time" (System/currentTimeMillis)))
        (evaluate (convert/convert '(QueueRun)))
        (swap! lqr-atom (fn [_] (System/nanoTime))))
      (do
        (Thread/sleep (quot run-in 1000000))
        (when (options/flag? dynamic-vars/*options* :verbose) (println "Sleeping for" (quot run-in 1000000) "ms"))))))
