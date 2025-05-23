(ns wolframite.base.cep
  (:require
    [wolframite.flags :as flags]
    [wolframite.lib.options :as options]
    [wolframite.base.convert :as convert]
    [wolframite.base.evaluate :as evaluate]
    [wolframite.base.parse :as parse]))

(defn- identity-first [x & _] x)

(defn cep
  "Convert-Evaluate-Parse pipeline.
  Convert:  from clj data to jlink Expr
  Evaluate: the Expr on (some) Wolfram Engine
  Parse:    returned result into clj data.
  Each stage can be skipped with appropriate `opts` `:flag` e.g. `:no-parse`"
  [expr {:keys [flags] :as opts}]
  (let [convert (if (options/flag?' flags flags/convert)
                  convert/wrapping-convert
                  identity-first)
        evaluate (if (options/flag?' flags flags/evaluate)
                   evaluate/evaluate
                   identity-first)
        parse (if (options/flag?' flags flags/parse)
                parse/unwrapping-parse
                identity-first)]
    (-> expr
        (convert opts)
        (evaluate opts)
        (parse opts))))
