; ***** BEGIN LICENSE BLOCK *****
; Version: MPL 1.1/GPL 2.0/LGPL 2.1
;
; The contents of this file are subject to the Mozilla Public License Version
; 1.1 (the "License"); you may not use this file except in compliance with
; the License. You may obtain a copy of the License at
; http://www.mozilla.org/MPL/
;
; Software distributed under the License is distributed on an "AS IS" basis,
; WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
; for the specific language governing rights and limitations under the
; License.
;
; The Original Code is the Clojure-Mathematica interface library Clojuratica.
;
; The Initial Developer of the Original Code is Garth Sheldon-Coulson.
; Portions created by the Initial Developer are Copyright (C) 2009
; the Initial Developer. All Rights Reserved.
;
; Contributor(s):
;
; Alternatively, the contents of this file may be used under the terms of
; either the GNU General Public License Version 2 or later (the "GPL"), or
; the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
; in which case the provisions of the GPL or the LGPL are applicable instead
; of those above. If you wish to allow use of your version of this file only
; under the terms of either the GPL or the LGPL, and not to allow others to
; use your version of this file under the terms of the MPL, indicate your
; decision by deleting the provisions above and replace them with the notice
; and other provisions required by the GPL or the LGPL. If you do not delete
; the provisions above, a recipient may use your version of this file under
; the terms of any one of the MPL, the GPL or the LGPL.
;
; ***** END LICENSE BLOCK *****

(ns wolframite.core
  (:refer-clojure :exclude [eval])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [wolframite.base.cep :as cep]
    [wolframite.base.convert :as convert]
    [wolframite.base.evaluate :as evaluate]
    [wolframite.base.express :as express]
    [wolframite.base.parse :as parse]
    [wolframite.impl.intern :as intern]
    [wolframite.jlink :as jlink]
    [wolframite.runtime.defaults :as defaults])
  (:import (clojure.lang IMeta)
           (com.wolfram.jlink KernelLink MathLinkException MathLinkFactory)))

(defonce kernel-link-atom (atom nil))

(defn kernel-link-opts ^"[Ljava.lang.String;" [{:keys [platform mathlink-path]}]
  ;; See https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html#createKernelLink(java.lang.String%5B%5D)
  ;; and https://reference.wolfram.com/language/tutorial/RunningTheWolframSystemFromWithinAnExternalProgram.html for the options
  (into-array String ["-linkmode" "launch"
                      "-linkname"
                      (format "\"/%s\" -mathlink"
                              (or mathlink-path
                                  (jlink/path--kernel)
                                  (throw (IllegalStateException. "mathlink path neither provided nor auto-detected"))))]))

(defn evaluator-init [opts]
  (let [wl-convert #(convert/convert   % opts)
        wl-eval    #(evaluate/evaluate % opts)]
    (wl-eval (wl-convert 'init))
    (wl-eval (wl-convert '(Needs "Parallel`Developer`")))
    (wl-eval (wl-convert '(Needs "Developer`")))
    (wl-eval (wl-convert '(ParallelNeeds "Developer`")))
    (wl-eval (wl-convert '(Needs "ClojurianScopes`")))
    (wl-eval (wl-convert '(ParallelNeeds "ClojurianScopes`")))
    (wl-eval (wl-convert '(Needs "HashMaps`")))
    (wl-eval (wl-convert '(ParallelNeeds "HashMaps`")))))

(comment

  (evaluator-init (merge {:kernel/link @kernel-link-atom} defaults/default-options)))

(defn- array? [x]
  (-> x class str (str/starts-with? "class [L")))

(defn init!
  "Provide platform identifier as one of: `:linux`, `:macos`, `:macos-mathematica` or `:windows`
  Defaults to platform identifier based on `os.name`"
  ([]
   (init! {:platform (jlink/detect-platform)}))
  ([{:keys [platform] :as init-opts}]
   {:pre [(if platform (jlink/supported-platforms platform) true)]}
   (let [opts (kernel-link-opts init-opts)
         kl (try (doto (MathLinkFactory/createKernelLink opts)
                   (.discardAnswer))
                 (catch MathLinkException e
                   (if (= (ex-message e) "MathLink connection was lost.")
                     (throw (ex-info (str "MathLink connection was lost. Perhaps you need to activate Mathematica first,"
                                          " you are trying to start multiple concurrent connections (from separate REPLs),"
                                          " or there is some other issue and you need to retry, or restart and retry...")
                                     {:kernel-link-opts (cond-> opts
                                                          (array? opts)
                                                          vec)
                                      :cause e}))
                     (throw e)))
                 (catch Exception e
                   (throw (ex-info (str "Failed to start a Math/Wolfram Kernel process: "
                                        (ex-message e)
                                        " Verify the settings are correct: `" opts "`")
                                   {:kernel-opts opts}))))]
     (reset! kernel-link-atom kl)
     kl)))

(defn terminate-kernel! []
  (.terminateKernel ^KernelLink @kernel-link-atom)
  (reset! kernel-link-atom nil))

(defn un-qualify [form]
  (walk/postwalk (fn [form]
                   (if (qualified-symbol? form)
                     (symbol (name form))
                     form))
                 form))

(defn make-wl-evaluator [opts]
  (when-not (instance? KernelLink @kernel-link-atom) (init!))
  (evaluator-init (merge {:kernel/link @kernel-link-atom} opts))
  (fn wl-eval
    ([expr]
     (wl-eval expr {}))
    ([expr eval-opts]
     (let [with-eval-opts (merge {:kernel/link @kernel-link-atom}
                                 opts
                                 eval-opts)
           expr' (un-qualify (if (string? expr) (express/express expr with-eval-opts) expr))]
       (cep/cep expr' with-eval-opts)))))

(defonce ^:private evaluator (make-wl-evaluator defaults/default-options))

(def ^:deprecated wl "DEPRECATED - use `eval` instead." evaluator)

(def
  ^{:arglists '([expr]
                [expr opts])
    :doc "Evaluate the given Wolfram expression (a string, or a Clojure data) and return the result as Clojure data.

    The `opts` map may contain `:flags [kwd ...]` and is passed e.g. to the `custom-parse` multimethod.

    Example:
    ```clojure
    (wl/eval \"Plus[1,2]\")
    ; => 3
    (wl/eval '(Plus 1 2))`
    ; => 3
    ```

    See also [[load-all-symbols]], which enable you to make a Wolfram function callable directly.

    Tip: Use [[->wl]] to look at the final expression that would be sent to Wolfram for evaluation."}
  eval evaluator)

;; TODO Should we expose this, or will just folks shoot themselves in the foot with it?
(defn- clj-intern-autoevaled
  "Intern the given Wolfram symbol into the given `ns-sym` namespace as a function,
  which will call Wolfram to evaluate itself.

  BEWARE: If you nest these functions, e.g. `(Plus 1 (Plus 2 3))`, there will be a call to Wolfram kernel for each one.
  This is likely not what you want."
  [wl-sym {:intern/keys [ns-sym extra-meta] :as opts}]
  (intern (create-ns (or ns-sym (.name *ns*)))
          (with-meta wl-sym (merge {:clj-intern true} extra-meta))
          (parse/parse-fn wl-sym (merge {:kernel/link @kernel-link-atom}
                                          defaults/default-options
                                          opts))))

(defn ->clj
  "Turn the given Wolfram expression string into its Clojure data structure form.

  Ex.: `(->clj \"Power[2,3]\") ; => (Power 2 3)`"
  [s]
  {:flags [:no-evaluate]}
  (eval (list 'quote s) {:flags [:no-evaluate]}))

(defn ->wl
  "Convert clojure forms to mathematica Expr.
  Generally useful, especially for working with graphics - or for troubleshooting
  what will be sent to Wolfram for evaluation."
  ([clj-form] (->wl clj-form {:output-fn str}))
  ([clj-form {:keys [output-fn] :as opts}]
   (cond-> (convert/convert clj-form (merge {:kernel/link @kernel-link-atom} opts))
           (ifn? output-fn) output-fn)))

(defn load-all-symbols
  "Loads all WL global symbols as vars with docstrings into a namespace given by symbol `ns-sym`.
  These vars evaluate into a symbolic form, which can be passed to [[eval]]. You gain docstrings,
  (possibly) autocompletion, and convenient inclusion of vars that you want evaluated before sending the
  form off to Wolfram, without the need for quote - unquote: `(let [x 3] (eval (Plus x 1)))`.

  Beware: May take a couple of seconds.
  Example:
  ```clojure
  (wl/load-all-symbols 'w)
  (w/Plus 1 2) ; now the same as (wl/eval '(Plus 1 2))
  ```

  Alternatively, load the included but likely outdated `resources/wld.wl` with a dump of the data."
  [ns-sym]
  ;; TODO (jh) support loading symbols from a custom context - use (Names <context name>`*) to get the names -> (Information <context name>`<fn name>) -> get FullName (drop ...`), Usage (no PlaintextUsage there) from the entity
  ;; IDEA: Provide also (load-symbols <list of symbols or a regexp>), which would load only a subset
  (doall (->> (eval '(EntityValue (WolframLanguageData) ["Name", "PlaintextUsage"] "EntityPropertyAssociation"))
              vals ; keys ~ `(Entity "WolframLanguageSymbol" "ImageCorrelate")`
              (map (fn [{sym "Name", doc "PlaintextUsage"}]
                     (intern/clj-intern
                       (symbol sym)
                       {:intern/ns-sym     ns-sym
                        :intern/extra-meta {:doc (when (string? doc) ; could be `(Missing "NotAvailable")`
                                                   doc)}}))))))

(comment
  (->
    (eval ('Names "System`*"))
    println)

  (-> (eval '(Information "System`Plus"))
      (nth 1))

  (defn is-function?
    "Guesses whether or not the given symbol is a 'function' in the normal sense.

  NOTE: It turns out that this is pretty difficult because everything in Mathematica is pretty much technically a function...
  "
    [symbol]
    (let [ks ["UpValues" "DefaultValues" "SubValues" "OwnValues" "FormatValues" "DownValues" "NValues"]
          data (-> (eval `(Information ~symbol)) second)
          has-values? (->> data
                           (partial get)
                           (#(map % ks))
                           (mapv #(not= 'None %))
                           (some identity))
          has-function? (-> data
                            (get "Attributes")
                            (->> (map str)
                                 (mapv #(str/includes? % "Function"))
                                 (some identity)))]

      (or has-values? has-function?)))

  (->  (is-function? "System`Subtract")))
