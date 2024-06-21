(ns wolframite.impl.wolfram-syms.wolfram-syms
  "Support for loading available symbols (fns & more) from Wolfram"
  (:require [wolframite.impl.wolfram-syms.intern :as intern]))

(defn fetch-all-wolfram-symbols [wl-eval]
  (doall (->> (wl-eval '(EntityValue (WolframLanguageData) ["Name", "PlaintextUsage"] "EntityPropertyAssociation"))
              vals ; keys ~ `(Entity "WolframLanguageSymbol" "ImageCorrelate")`
              (map (fn [{sym "Name", doc "PlaintextUsage"}]
                     {:sym (symbol sym), :doc doc})))))

(defn load-all-symbols
  ;; Docstring in wolframite.core
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
