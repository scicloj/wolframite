(ns wolframite.base.express
  (:require [wolframite.impl.protocols :as proto]))

(defn express [s {:keys [jlink-instance]}]
  {:pre [jlink-instance]}
  (assert (string? s))
  ;; TODO: debug log: "express string>"
  (let [held-s (str "HoldComplete[" s "]")
        link (proto/kernel-link jlink-instance)
        output (io!
                (locking link
                  (doto link
                    (.evaluate held-s)
                    (.waitForAnswer))
                  (.. link getExpr args)))] ; TODO (jakub) get rid of reflection, move into the protocol
    (if (= (count output) 1)
      (first output)
      (throw (Exception. (str "Invalid expression: " s))))))
