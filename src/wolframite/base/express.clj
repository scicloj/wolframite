(ns wolframite.base.express)

(defn express [s {:keys [kernel/link]}]
  (assert (string? s))
  ;; TODO: debug log: "express string>"
  (let [held-s (str "HoldComplete[" s "]")
        output (io!
                (locking link
                  (doto link
                    (.evaluate held-s)
                    (.waitForAnswer))
                  (.. link getExpr args)))]
    (if (= (count output) 1)
      (first output)
      (throw (Exception. (str "Invalid expression: " s))))))
