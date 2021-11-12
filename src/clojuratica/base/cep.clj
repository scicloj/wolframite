(ns clojuratica.base.cep
  (:require
    [clojuratica.lib.options :as options]
    [clojuratica.base.convert :as convert]
    [clojuratica.base.evaluate :as evaluate]
    [clojuratica.base.parse :as parse]))

(defn cep
  "Convert-Evaluate-Parase pipeline.
  Convert:  from clj data to jlink Expr
  Evaluate: the Expr on Wolfram Engine
  Parse:    returned result into clj data.
  Each stage can be skipped with appropraite `opts` `:flag` e.g. `:no-parse`"
  [expr {:keys [flags]
         :as   opts}]
  (let [convert  (if (options/flag?' flags :convert)   convert/convert   identity)
        evaluate (if (options/flag?' flags :evaluate)  evaluate/evaluate identity)
        parse    (if (options/flag?' flags :parse)     parse/parse       identity)]
    (-> expr
        (convert  opts)
        (evaluate opts)
        (parse    opts))))
