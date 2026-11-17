(ns wolframite.impl.error-detection
  "Centralize Wolfram-related error handling"
  (:require [clojure.tools.logging :as log]
            [wolframite.impl.internal-constants :as internal-constants]
            [wolframite.impl.protocols :as proto]))

(defn error-message-expr
  "Does the result represent a (delayed) error message template, which we can evaluate
  to get the actual message?"
  ;; Ex.: Association[RuleDelayed["MessageTemplate", MessageName[Interpreter, "noknow"]]]
  [clj-result]
  (and (map? clj-result)
       (= 1 (count clj-result))
       (= "MessageTemplate" (ffirst clj-result))
       (let [{:strs [MessageTemplate]} clj-result]
         (when (-> MessageTemplate meta :wolfram/delayed)
           MessageTemplate))))

(def ^:private wrapper-names
  #{(name internal-constants/wolframiteLimitSize)
    (name internal-constants/wolframiteFastArray)})

(defn- unwrap [input]
  (if (contains? wrapper-names
                 ;; head-sym-str throws if the head is not a symbol, e.g. for f[x][y]
                 (try (proto/head-sym-str input) (catch Exception _ nil)))
    (recur (-> input proto/args first))
    input))

(defn- unchanged-expression? [input output]
  (= (unwrap input) output))

(defn- head-name [expr]
  ;; head-sym-str throws if the head is not a symbol, e.g. for f[x][y]
  (try (proto/head-sym-str expr) (catch Exception _ nil)))

(defn- throw-if-invalid-string!
  "Throw if `eval-result` is the `WolframiteInvalidString[str, n]` marker of `wolframiteEvalString`,
  i.e. the string we were to evaluate didn't parse into exactly one expression."
  [eval-result eval-messages]
  (when (= (name internal-constants/WolframiteInvalidString) (head-name eval-result))
    (let [[s n] (proto/args eval-result)]
      ;; Same message as `wolframite.base.express/express` uses, when we parse the string separately
      (throw (ex-info (format "Invalid expression `%s`. Output lenght expected 1, was %d"
                              (proto/as-string s) (proto/as-number n))
                      {:messages eval-messages})))))

(defn ensure-no-eval-error [expr eval-result eval-messages]
  (throw-if-invalid-string! eval-result eval-messages)
  (let [;; `wolframiteEvalString` (string input) checks itself whether the result is unchanged
        unevaluated-marker? (= (name internal-constants/WolframiteUnevaluated) (head-name eval-result))
        eval-result (cond-> eval-result
                      unevaluated-marker? (-> proto/args first))
        messages-text (mapv :content eval-messages)
        failed-sym? (proto/failed? eval-result)]
    (cond
      (and (seq eval-messages)
           (or failed-sym?
               unevaluated-marker?
               (unchanged-expression? expr eval-result)))
      ;; If input expr == output expr, this usually means the evaluation failed
      ;; (or there was nothing to do); if there are also any extra text/message packets
      ;; then it most likely has failed, and those messages explain what was wrong
      (throw (ex-info (str "Evaluation seems to have failed. Result: "
                           eval-result
                           " Details: "
                           (cond-> messages-text
                                   (= 1 (count messages-text))
                                   first))
                      {:expr expr
                       :messages eval-messages
                       :result eval-result}))

      failed-sym? ; but no messages
      (throw (ex-info (str "Evaluation has failed. Result: "
                           eval-result
                           " No details available.")
                      {:expr expr :result eval-result}))

      :else
      (do (when (seq eval-messages)
            (log/info "Messages retrieved during evaluation:" messages-text))
          eval-result))))
