(ns wolframite.base.package
  "The place to be for loading and manipulating Wolfram packages.
"
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [wolframite.core :as wl]
   [wolframite.impl.wolfram-syms.intern :as wi]
   [wolframite.wolfram :as w]
   [clojure.repl :as repl]))

(defn intern-context!
  "Finds all of the public symbols in the given Wolfram `context` (a string) and interns them to a Clojure namespace named by the given`alias` (a symbol; default: the context's name). The namespace is created
  if it does not yet exist.

  N.B. context does not contain the trailing '`'"
  ([context]
   (intern-context! context (symbol context)))
  ([context alias]
    {:pre [(string? context) (symbol? alias)]}
   (let [names (wl/eval (w/Names (str context "`*")))
         docs (for [name names]
                (mapv #(wl/eval (w/Information name %))
                      ["FullName" "Usage"]))]

     (run! (fn [[fname doc]]
             (wi/clj-intern (symbol (-> (str/split fname #"`") last))
                            {:intern/ns-sym alias
                             :intern/extra-meta {:doc doc}}))
           docs))))

(defn load-package!
  "An extended version of Wolfram's 'Get'. Gets a Wolfram package and makes the constants/functions etc. accessible via a Clojure namespace (the given `alias`, by default same as `context`).

  Example:  `(<<! \"./resources/WolframPackageDemo.wl\" \"WolframPackageDemo\" 'wp)`

 - `path` - string pointing to the package file
 - `context` - string for Wolfram context (essentially Wolfram's version of a namespace)
 - `alias` - Clojure symbol to be used for accessing the Wolfram context. This will effectively become a Clojure namespace
 
See [[intern-context!]] for details of turning the Wolfram context into a Clojure namespace."
;; TODO: Should check that the symbol isn't already being used.
  ([path]
   (let [context (-> path fs/file-name fs/strip-ext)]
     (load-package! path context (symbol context))))

  ([path context]
   (load-package! path context (symbol context)))

  ([path context alias]
   (wl/eval (w/Get path))
   (intern-context! context alias)))

(def <<!
  "A Wolfram-like alias to load-package! Gets a Wolfram package and adds the constants/functions etc. under a clojure-accessible symbol: either the given one or the name of the context by default."
  load-package!)

(comment
  (wl/start)

  (<<! "resources/WolframPackageDemo.wl")
  (load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo")
  (load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo" 'wp)

  (wl/eval  (w/Information wp/tryIt "Usage"))
  (wl/eval (wp/tryIt 10))

  (wl/eval  (w/Information WolframPackageDemo/additional "Usage"))
  (wl/eval (WolframPackageDemo/additional 10)))
