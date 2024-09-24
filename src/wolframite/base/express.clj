(ns wolframite.base.express
  (:require [wolframite.impl.protocols :as proto]))

(defn express
  "??? Convert a _string_ expression into JLink.Expr using Wolfram itself"
  [s {:keys [jlink-instance]}]
  {:pre [jlink-instance]}
  (assert (string? s) (str "Expected '" (pr-str s) "' to be a string but is " (type s)))
  ;; TODO: debug log: "express string>"
  (let [held-s (str "HoldComplete[" s "]")
        link (or (proto/kernel-link jlink-instance)
                 (throw (IllegalStateException.
                          (str "Something is wrong - there is no JLink instance, which shouldn't"
                               " be possible. Try to stop and start Wolframite again."))))
        output (io!
                (locking link
                  (doto link
                    (.evaluate held-s)
                    (.waitForAnswer))
                  (.. link getExpr args)))] ; TODO (jakub) get rid of reflection, move into the protocol
    (if (= (count output) 1)
      (first output)
      (throw (Exception. (str "Invalid expression: " s))))))
