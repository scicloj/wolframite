(ns wolframite.flags
  "Options for modifying the behavior of converting and evaluating Wolframite code and parsing the response.

  Flags can be changed for the whole session via `wolframite.core/start!` or for a particular evaluation
  via `wolframite.core/eval`.

  Only the options described here are (currently) officially supported.")

(def parallel
  "Evaluate in parallel. This is useful when you have a bunch of parallel kernels (Wolfram-managed).
  Opposite of [[serial]]."
  :parallel)

(def serial
  "[DEFAULT] Send expressions to a single Wolfram kernel one by one, waiting for the response before sending the next one.
  Opposite of [[parallel]]."
  :serial)

(def parse
  "[DEFAULT] Do parse the Wolfram response into Clojure data.
  Opposite of [[no-parse]]."
  :parse)

(def no-parse
  "Return Wolfram response as-is, i.e. as JLink Expr.
  Opposite of [[parse]]."
  :no-parse)

(def evaluate
  "[DEFAULT] Actually send expressions to Wolfram for evaluation.
  Opposite of [[no-evaluate]]."
  :evaluate)

(def no-evaluate
  "Skip sending expressions to Wolfram for evaluation. May be useful for troubleshooting.
  Opposite of [[evaluate]]."
  :no-evaluate)

(def convert
  "[DEFAULT] Convert the Wolframite Clojure data expression into Jlink Expr.
  Opposite of [[no-convert]]."
  :convert)

(def no-convert
  "Opposite of [[convert]]."
  :no-convert)

(def arrays
  "Return sequences from Wolfram as Java arrays, not Clojure vectors. This is more efficient
  for large arrays of primitive types, primarily floats, doubles, and ints.
  Opposite of [[no-arrays]]."
  :arrays)

(def no-arrays
  "[DEFAULT] Return sequences from Wolfram as Clojure vectors.
  Opposite of [[arrays]]."
  :no-arrays)

(def allow-large-data
  "When set then Wolframite will return any result from Wolfram, no matter how large.
  Normally, Wolframite would return `:wolframite/large-data` if the result is too large."
  :allow-large-data)

;; #{:vectors :seqs #_:seq-fn} :vectors ;; **DEPRECATED**: this is not really a flag, not sure how useful at all
;; #{:full-form :clojure-form} ; TODO Not sure what this is
