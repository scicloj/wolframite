(ns wolframite.impl.error-detection)

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