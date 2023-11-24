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

(ns clojuratica.core
  (:refer-clojure :exclude [eval])
  (:require
   [clojure.walk :as walk]
   [clojuratica.base.cep :as cep]
   [clojuratica.base.convert :as convert]
   [clojuratica.base.evaluate :as evaluate]
   [clojuratica.base.express :as express]
   [clojuratica.base.parse :as parse]
   [clojuratica.jlink :as jlink]
   [clojuratica.runtime.defaults :as defaults])
  (:import (com.wolfram.jlink MathLinkFactory)))

(defonce kernel-link-atom (atom nil))

(defn kernel-link-opts [{:keys [platform mathlink-path]}]
  (format "-linkmode launch -linkname '\"%s\" -mathlink'" (or mathlink-path (jlink/get-mathlink-path platform))))

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

  (evaluator-init (merge {:kernel/link @kernel-link-atom} defaults/default-options))

  )

(defn init!
  "Provide platform identifier as one of: `:linux`, `:macos`, `:macos-mathematica` or `:windows`
  Defaults to platform identifier based on `os.name`"
  ([]
   (init! {:platform (jlink/platform-id (System/getProperty "os.name"))}))
  ([{:keys [platform] :as init-opts}]
   {:pre [(if platform (jlink/supported-platform? platform) true)]}
   (let [kl (doto (MathLinkFactory/createKernelLink (kernel-link-opts init-opts))
              (.discardAnswer))]
     (reset! kernel-link-atom kl)
     kl)))

(defn terminate-kernel! []
  (.terminateKernel @kernel-link-atom)
  (reset! kernel-link-atom nil))

(defn un-qualify [form]
  (walk/postwalk (fn [form]
                   (if (qualified-symbol? form)
                     (symbol (name form))
                     form))
                 form))

(defn make-wl-evaluator [opts]
  (when-not (instance? com.wolfram.jlink.KernelLink @kernel-link-atom) (init!))
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

(defonce wl (make-wl-evaluator defaults/default-options))
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

    See also [[clj-intern]] and [[load-all-symbols]], which enable you to make a Wolfram function callable directly."}
  eval wl) ; TODO Decide whether we should keep both eval and wl, both meaning the same, or settle on just one.

(defn clj-intern
  "Finds or creates a var named by the symbol `wl-fn-sym` in the current namespace,
  which will be a function that invokes a Wolfram function of the same name.

  You can override the target namespace and add additional metadata to the var by
  setting the appropriate `opts`.

  Ex.:
  ```clj
  (clj-intern 'Plus {})
  (Plus 1 2)
  ; => 3
  ```

  See also [[load-all-symbols]]."
  ([wl-fn-sym]
   (clj-intern wl-fn-sym {}))
  ([wl-fn-sym {:intern/keys [ns-sym extra-meta] :as opts}]
   (intern (create-ns (or ns-sym (.name *ns*)))
           (with-meta wl-fn-sym (merge {:clj-intern true} extra-meta))
           (parse/parse-fn wl-fn-sym (merge {:kernel/link @kernel-link-atom}
                                            defaults/default-options
                                            opts)))))

(defn ->clj! [s]
  {:flags [:no-evaluate]}
  (wl (list 'quote s) {:flags [:no-evaluate]}))

(defn ->wl!
  "Convert clojure forms to mathematica Expr.
  Generally useful, especially for working with graphics."
  [clj-form {:keys [output-fn] :as opts}]
  (cond-> (convert/convert clj-form (merge {:kernel/link @kernel-link-atom} opts))
    (ifn? output-fn) output-fn))

(defn load-all-symbols
  ;; BEWARE: This is extremely slow (10s of secs / few minutes), most of this spent in Wolfram itself
  ;; IDEAS: 1) Have also (load-symbols <list of symbols or a regexp>), which would load only a subset
  ;;           (hopefully much faster)
  ;;        2) Pre-load an intern the symbols into a ns here. Pros: available at no wait; cons: sometimes outdated
  "Loads all WL global symbols with docstrings into a namespace given by symbol `ns-sym`,
  using [[clj-intern]].
  Beware: May take quite some time to complete. You may want to run it in a future."
  [ns-sym]
  (doall (->> '(Map (Function [e] ((e "Name") (e "PlaintextUsage")))
                    ;; this map is very slow (few minutes), likely due to fetching the docs; but even just the name takes ~20-30s
                    ;; All the time is spent in Wolfram; executing `Map[Function[{e},{e["Name"],e["PlaintextUsage"]}],WolframLanguageData[]]` in there

                    ;; directly also takes forever
                    (WolframLanguageData)) ; this takes 1-2s
              wl
              (map vec)
              (map (fn [[sym doc]]
                     (clj-intern (symbol sym) {:intern/ns-sym ns-sym
                                               :intern/extra-meta {:doc (when (string? doc) ; could be `(Missing "NotAvailable")`
                                                                          doc)}}))))))
