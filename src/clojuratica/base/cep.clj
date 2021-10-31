(ns clojuratica.base.cep
  (:require
    [clojuratica.runtime.dynamic-vars :as dynamic-vars]
    [clojuratica.runtime.default-options :as default-options]
    [clojuratica.lib.options :as options]
    [clojuratica.base.convert :as convert]
    [clojuratica.base.evaluate :as evaluate]
    [clojuratica.base.parse :as parse]))

(defn cep [expr]
  (if (= expr :get-dynamic-vars)
    {'*kernel* dynamic-vars/*kernel* '*options* dynamic-vars/*options*}
    (binding [dynamic-vars/*options* (if (options/flag? dynamic-vars/*options* :restore-defaults) default-options/*default-options* dynamic-vars/*options*)]
      (let [convert  (if (options/flag? dynamic-vars/*options* :convert)   convert/convert   identity)
            evaluate (if (options/flag? dynamic-vars/*options* :evaluate)  evaluate/evaluate identity)
            parse    (if (options/flag? dynamic-vars/*options* :parse)     parse/parse       identity)]
        ((comp parse evaluate convert) expr)))))
