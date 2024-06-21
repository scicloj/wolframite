(ns wolframite.impl.wolfram-syms.write-ns
  "Support creating wolframite/wolfram.clj with defs for all loaded Wolfram symbols - to support
  autocompletion (even in editors using static code analysis) and linters (clj-kondo only does
  static code)"
  (:require
    [clojure.string :as str]
    [wolframite.core :as core]
    [wolframite.impl.wolfram-syms.intern :as intern]
    [wolframite.impl.wolfram-syms.wolfram-syms :as wolfram-syms]))

(def wolfram-ns-heading
  ['(ns wolframite.wolfram
      "[GENERATED - see `...wolfram-syms.write-ns/write-ns!`]
      Vars for all Wolfram functions (and their Clojurite aliases, where those exist).
     These can be composed into expressions and passed to `wl/eval`.

     BEWARE: This is based off a particular version of Wolfram and you may need to refresh it."
      (:require [wolframite.impl.wolfram-syms.intern])
      (:refer-clojure :only [map ns-unmap])) ; def and quote do not need to be listed
   ;; FIXME Add aliases for +, -, *, /, etc.
   `(do ~@(map (fn [s] `(ns-unmap *ns* (quote ~s)))
               '[Byte Character Integer Number Short String Thread]))
   ,])

(defn- make-defs
  ([] (make-defs (wolfram-syms/fetch-all-wolfram-symbols core/eval)))
  ([all-syms]
   (for [{:keys [sym doc]} (sort-by :sym all-syms)]
     (list 'def sym (if (string? doc) doc "") `(intern/wolfram-fn '~sym)))))

(defn write-ns! []
  (spit "src/wolframite/wolfram.clj"
        (str/join "\n"
                  (concat
                    (map pr-str wolfram-ns-heading)
                    (map pr-str (make-defs))))))

;(defmacro make-wolf-defs []
;  `(do ~@(make-defs)))
; USAGE: Use
;(macroexpand '(make-wolf-defs))

(comment

  (do (time (write-ns!))
      (load-file "src/wolframite/wolfram.clj")))