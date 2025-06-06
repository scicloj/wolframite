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

(defn- unchanged-expression? [input output]
  (let [size-wrapper? (= (proto/head-sym-str input)
                         (name internal-constants/wolframiteLimitSize))
        unwrapped-input (if size-wrapper?
                          (-> input proto/args first)
                          input)]
    (= unwrapped-input output)))

(defn ensure-no-eval-error [expr eval-result eval-messages]
  (let [messages-text (mapv :content eval-messages)
        failed-sym? (proto/failed? eval-result)]
    (cond
      (and (seq eval-messages)
           (or failed-sym?
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
