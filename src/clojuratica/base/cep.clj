(ns clojuratica.base.cep
  (:require
    [clojuratica.lib.options :as options]
    [clojuratica.base.convert :as convert]
    [clojuratica.base.evaluate :as evaluate]
    [clojuratica.base.parse :as parse]))

(defn cep
  "Convert-Evaluate-Parse pipeline.
  Convert:  from clj data to jlink Expr
  Evaluate: the Expr on (some) Wolfram Engine
  Parse:    returned result into clj data.
  Each stage can be skipped with appropriate `opts` `:flag` e.g. `:no-parse`"
  [expr {:keys [flags]
         :as   opts}]
  (let [convert  (if (options/flag?' flags :convert)   convert/convert   (fn [& args] (first args)))
        evaluate (if (options/flag?' flags :evaluate)  evaluate/evaluate (fn [& args] (first args)))
        parse    (if (options/flag?' flags :parse)     parse/parse       (fn [& args] (first args)))]
    (-> expr
        (convert  opts)
        (evaluate opts)
        (parse    opts))))
