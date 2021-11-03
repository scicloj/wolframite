(ns clojuratica.base.express
  (:require [clojuratica.jlink]
            [clojuratica.lib.options :as options]
            [clojuratica.runtime.dynamic-vars :as dynamic-vars]))

(defn express [s]
  (let [kernel-link (dynamic-vars/*kernel* :link)]
    (assert (string? s))
    (assert (instance? com.wolfram.jlink.KernelLink kernel-link))
    (when (options/flag? dynamic-vars/*options* :verbose) (println "express string>" s))
    (let [held-s (str "HoldComplete[" s "]")
          output (io!
                   (locking kernel-link
                     (doto kernel-link
                       (.evaluate held-s)
                       (.waitForAnswer))
                     (.. kernel-link getExpr args)))]
      (if (= (count output) 1)
        (first output)
        (throw (Exception. (str "Invalid expression: " s)))))))
