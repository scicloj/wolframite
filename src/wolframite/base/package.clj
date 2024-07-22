(ns wolframite.base.package
  "The place to be for loading and manipulating Wolfram packages.
"
  (:require
   [clojure.string :as string]
   [wolframite.core :as wl]
   [wolframite.impl.wolfram-syms.intern :as wi]
   [wolframite.wolfram :as w]))

(defn load-symbols
  "Finds all of the public symbols in the given context and interns them to a clojure-accesible namespace. This is put under the given symbol or the context by default.

  N.B. context does not contain the trailing '`'"
  ([context]
   (load-symbols context (symbol context)))
  ([context sym--ns]
   (let [names (wl/eval (w/Names (str context "`*")))
         docs (mapv (fn [name]
                      (mapv (fn [attr] (wl/eval (w/Information name attr)))
                            ["FullName" "Usage"]))
                    names)]

     (println [sym--ns docs])
     (doall (map (fn [[name-sym doc]]
                   (wi/clj-intern (symbol (-> (string/split name-sym #"`") last)) {:intern/ns-sym sym--ns
                                                                                   :intern/extra-meta {:doc doc}}))
                 docs)))))

(defn <<
  "Get a Wolfram package and adds the constants/functions etc. under a clojure-accessible symbol: either the given one or the name of the context by default.

  TODO: Should check that the symbol isn't already being used."
  ([fname context]
   (<< fname context (symbol context)))

  ([fname context sym]
   (wl/eval (w/Get fname))
   (wl/eval (w/Names "WolframPackage`*"))
   (load-symbols context sym)))

(comment
  (wl/start)

  (<< "resources/wolframPackage.wl" "WolframPackage" 'wp)
  (<< "resources/wolframPackage.wl" "WolframPackage")

  (wl/eval  (w/Information wp/tryIt "Usage"))
  (wl/eval (wp/tryIt 10))
  (wl/eval  (w/Information WolframPackage/additional "Usage"))
  (wl/eval (WolframPackage/additional 10)))
