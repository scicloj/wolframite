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
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [wolframite.base.cep :as cep]
    [wolframite.base.convert :as convert]
    [wolframite.base.evaluate :as evaluate]
    [wolframite.base.express :as express]
    [wolframite.base.parse :as parse]
    [wolframite.impl.jlink-instance :as jlink-instance]
    [wolframite.impl.protocols :as proto]
    [wolframite.impl.wolfram-syms.wolfram-syms :as wolfram-syms]
    [wolframite.runtime.system :as system]
    [wolframite.runtime.jlink :as jlink]
    ;;^ currently necessary import to auto-install jlink
    [wolframite.runtime.defaults :as defaults]
    [wolframite.wolfram :as w]))

(defonce kernel-link-atom (atom nil)) ; FIXME (jakub) DEPRECATED, access it via the jlink-instance instead

(defn kernel-link-opts ^"[Ljava.lang.String;" [{:keys [platform mathlink-path]}]
  ;; See https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html#createKernelLink(java.lang.String%5B%5D)
  ;; and https://reference.wolfram.com/language/tutorial/RunningTheWolframSystemFromWithinAnExternalProgram.html for the options
  (into-array String ["-linkmode" "launch"
                      "-linkname"
                      (format "\"/%s\" -mathlink"
                              (or mathlink-path
                                  (system/path--kernel)
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

(defn init-jlink!
  "DO NOT USE! (internal)"
  [kernel-link-atom opts]
  (or (jlink-instance/get)
      (do (jlink/add-jlink-to-classpath!)
          (reset! jlink-instance/jlink-instance
                  ;; req. res. since we can't load this code until the JLink JAR has been loaded
                  ((requiring-resolve 'wolframite.impl.jlink-proto-impl/create)
                   kernel-link-atom opts)))))

(defn- init-kernel!
  "Provide os identifier as one of wolframite.runtime.system/supported-OS"
  ([jlink-impl]
   (init-kernel! jlink-impl {:os (system/detect-os)}))
  ([jlink-impl {:keys [os] :as init-opts}]
   {:pre [(some-> os system/supported-OS) jlink-impl]}
   (->> (kernel-link-opts init-opts)
        (proto/create-kernel-link jlink-impl))))

(defn terminate-kernel! []
  (proto/terminate-kernel! (jlink-instance/get)))

(defn un-qualify [form]
  (walk/postwalk (fn [form]
                   (if (qualified-symbol? form)
                     (symbol (name form))
                     form))
                 form))

(declare eval)

(def kernel-info
  "A promise map holding information about the Wolfram kernel, initialized when
  connected to the kernel for the first time. Ex.:

  ```clojure

  @kernel-info
  ; => {:wolfram-version 14.0
  ;     :wolfram-kernel-name \"Wolfram Language 14.0.0 Engine\"
  ;     :max-license-processes 2} ; how many concurrent kernels (=> Wolframite REPLs/processes) may we run
  ```"
  (promise))

(defn kernel-info!
  "Fetches info about the Wolfram kernel, such as:

  ```clojure
  {:wolfram-version 14.0
   :wolfram-kernel-name \"Wolfram Language 14.0.0 Engine\"
   :max-license-processes 2} ; how many concurrent kernels (=> Wolframite REPLs/processes) may we run
  ```
  Requires [[init!]] to be called first."
  []
  (zipmap
    [:wolfram-version :wolfram-kernel-name :max-license-processes]
    (eval '[$VersionNumber
            (SystemInformation "Kernel", "ProductKernelName")
            (SystemInformation "Kernel", "MaxLicenseProcesses")])))


(defn init!
  "Initialize Wolframite and the underlying wolfram Kernel - required once before any eval calls."
  ([] (init! defaults/default-options))
  ([opts]
   (when-not (some-> (jlink-instance/get) (proto/kernel-link?)) ; need both, b/c some tests only init jlink
     (let [jlink-inst (or (jlink-instance/get)
                          (init-jlink! kernel-link-atom opts))]
       (init-kernel! jlink-inst)
       (evaluator-init (merge {:jlink-instance jlink-inst}
                              opts))
       (let [{:keys [wolfram-version]}
             (doto (kernel-info!)
               (->> (deliver kernel-info)))]
         (when (and (number? wolfram-version)
                    (number? w/*wolfram-version*)
                    (> wolfram-version w/*wolfram-version*))
           (log/warnf "You have a newer Wolfram version %s than the %s used to generate wolframite.wolfram
           and may want to re-create it with (wolframite.impl.wolfram-syms.write-ns/write-ns!)"
                      wolfram-version w/*wolfram-version*)))
      (:wolfram-version @kernel-info))
     nil)
   nil))

(comment (init!))

(defn eval
  "Evaluate the given Wolfram expression (a string, or a Clojure data) and return the result as Clojure data.

    The `opts` map may contain `:flags [kwd ...]` and is passed e.g. to the `custom-parse` multimethod.

    Example:
    ```clojure
    (wl/eval \"Plus[1,2]\")
    ; => 3
    (wl/eval '(Plus 1 2))`
    ; => 3
    ```

    See also [[load-all-symbols]], which enable you to make a Wolfram function callable directly.

    Tip: Use [[->wl]] to look at the final expression that would be sent to Wolfram for evaluation."
  ([expr] (eval expr {}))
  ([expr eval-opts]
   (if-let [jlink-inst (jlink-instance/get)]
     (let [with-eval-opts (merge {:jlink-instance jlink-inst}
                                 (:opts jlink-inst)
                                 eval-opts)
           expr' (un-qualify (if (string? expr) (express/express expr with-eval-opts) expr))]
       (cep/cep expr' with-eval-opts))
     (throw (IllegalStateException. "Not initialized, call init! first")))))

(def ^:deprecated wl "DEPRECATED - use `eval` instead." eval)

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
  "BEWARE: You shouldn't need to use this, as they are already loaded into wolframite.wolfram; use
  `(wolframite.impl.wolfram-syms.write-ns/write-ns!)` if you want to refresh that file with new
  functions in your version of Wolfram.

  ### Old docstring
  Loads all WL global symbols as vars with docstrings into a namespace given by symbol `ns-sym`.
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
  (wolfram-syms/load-all-symbols eval ns-sym))

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
