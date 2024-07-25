(ns wolframite.impl.wolfram-syms.wolfram-syms
  "Support for loading available symbols (fns & more) from Wolfram"
  (:require [wolframite.impl.wolfram-syms.intern :as intern]))

(defn fetch-all-wolfram-symbols [wl-eval]
  (doall (->> (wl-eval '(EntityValue (WolframLanguageData) ["Name", "PlaintextUsage"] "EntityPropertyAssociation"))
              vals ; keys ~ `(Entity "WolframLanguageSymbol" "ImageCorrelate")`
              (map (fn [{sym "Name", doc "PlaintextUsage"}]
                     {:sym (symbol sym), :doc doc})))))

(defn load-all-symbols
  "BEWARE: You shouldn't need to use this, as they are already loaded into wolframite.wolfram; use
  `(wolframite.impl.wolfram-syms.write-ns/write-ns!)` if you want to refresh that file with new
  functions in your version of Wolfram.

  ### Old docstring
  Loads all WL global symbols as vars with docstrings into a namespace given by symbol `ns-sym`.
  These vars evaluate into a symbolic form, which can be passed to `wl-eval`. You gain docstrings,
  (possibly) autocompletion, and convenient inclusion of vars that you want evaluated before sending the
  form off to Wolfram, without the need for quote - unquote: `(let [x 3] (eval (Plus x 1)))`.

  Beware: May take a couple of seconds.
  Example:
  ```clojure
  (load-all-symbols wolframite.core/eval 'w)
  (w/Plus 1 2) ; now the same as (wl/eval '(Plus 1 2))
  ```
  "
  [wl-eval ns-sym]
  ;; TODO (jh) support loading symbols from a custom context - use (Names <context name>`*) to get the names -> (Information <context name>`<fn name>) -> get FullName (drop ...`), Usage (no PlaintextUsage there) from the entity
  ;; IDEA: Provide also (load-symbols <list of symbols or a regexp>), which would load only a subset
  (doall (->> (fetch-all-wolfram-symbols wl-eval)
              (map (fn [{:keys [sym doc]}]
                     (intern/clj-intern
                       sym
                       {:intern/ns-sym     ns-sym
                        :intern/extra-meta {:doc (when (string? doc) ; could be `(Missing "NotAvailable")`
                                                   doc)}}))))))
