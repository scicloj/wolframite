; ***** BEGIN LICENSE BLOCK *****
; Version: MPL 2.0/GPL 2.0/LGPL 2.1
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; The Original Code is the Clojure-Mathematica interface library Clojuratica.
;
; The Initial Developer of the Original Code is Garth Sheldon-Coulson.
; Portions created by the Initial Developer are Copyright (C) 2009
; the Initial Developer. All Rights Reserved.
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
  "The main user-facing namespace of Wolframite (in conjunction with wolframite.wolfram).

  Public functions start! and stop! the kernel, load packages and perform conversions to and from the Wolfram language.
  "
  (:refer-clojure :exclude [eval])
  (:require
   [babashka.fs :as fs]
   [clojure.tools.logging :as log]
   [wolframite.base.cep :as cep]
   [wolframite.base.convert :as convert]
   [wolframite.base.evaluate :as evaluate]
   [wolframite.base.express :as express]
   [wolframite.base.package :as package]
   [wolframite.base.parse :as parse]
   [wolframite.impl.jlink-instance :as jlink-instance]
   [wolframite.impl.kindly-support :as kindly-support]
   [wolframite.impl.protocols :as proto]
   [wolframite.runtime.defaults :as defaults]
   [wolframite.runtime.jlink :as jlink]
   [wolframite.runtime.system :as system]
   [wolframite.wolfram :as w]))

(defonce ^{:deprecated true, :private true} kernel-link-atom (atom nil)) ; FIXME (jakub) DEPRECATED, access it via the jlink-instance instead

(defn- kernel-link-opts [{:keys [platform mathlink-path]}]
  ;; See https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html#createKernelLink(java.lang.String%5B%5D)
  ;; and https://reference.wolfram.com/language/tutorial/RunningTheWolframSystemFromWithinAnExternalProgram.html for the options
  ["-linkmode" "launch"
   "-linkname"
   (format "\"/%s\" -mathlink"
           (or mathlink-path
               (system/path--kernel)
               (throw (IllegalStateException. "mathlink path neither provided nor auto-detected"))))])

(defn-
  evaluator-init [opts]
  (let [wl-convert #(convert/convert   % opts)
        wl-eval    #(evaluate/evaluate % opts)]
    (wl-eval (wl-convert 'init))
    (wl-eval (wl-convert '(Needs "Parallel`Developer`")))
    (wl-eval (wl-convert '(Needs "Developer`")))
    (wl-eval (wl-convert '(ParallelNeeds "Developer`")))))

(comment
  (evaluator-init (merge {:kernel/link @kernel-link-atom} defaults/default-options)))

(defn- init-jlink! [kernel-link-atom opts]
  (or (jlink-instance/get)
      (do (jlink/add-jlink-to-classpath!)
          (reset! jlink-instance/jlink-instance
                  ;; req. res. since we can't load this code until the JLink JAR has been loaded
                  ((requiring-resolve 'wolframite.impl.jlink-proto-impl/create)
                   kernel-link-atom opts)))))

(defn- init-kernel!
  ([jlink-impl]
   (init-kernel! jlink-impl {:os (system/detect-os)}))
  ([jlink-impl {:keys [os] :as init-opts}]
   {:pre [(some-> os system/supported-OS) jlink-impl]}
   (->> (kernel-link-opts init-opts)
        (proto/create-kernel-link jlink-impl))))

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
  Requires [[start!]] to be called first."
  []
  (zipmap
   [:wolfram-version :wolfram-kernel-name :max-license-processes]
   (eval '[$VersionNumber
           (SystemInformation "Kernel", "ProductKernelName")
           (SystemInformation "Kernel", "MaxLicenseProcesses")])))

(defn start!
  "Initialize Wolframite and start! the underlying Wolfram Kernel - required once before you make any eval calls.

  - `opts` - a map that is passed to `eval` and other functions used in the convert-evaluate-parse
             cycle, which may contain, among others:
     - `:aliases` - a map from a custom symbol to a symbol Wolfram understands, such as the built-in
        `{'* 'Times, ...}` - added to the default aliases from `wolframite.runtime.defaults/all-aliases`
        and used when converting symbols at _function position_ to Wolfram expressions.
        You may add your own ones, to be able to use them in your Wolfram expressions and get those
        translated into Wolfram ones. See Wolframite docs.
     -  `:flags [kwd ...]` - various on/off toggles for how Wolframite processes inputs/results,
        passed e.g. to the `custom-parse` multimethod; see also wolframite.runtime.defaults/default-flags

  See also [[stop!]]"
  ([] (start! defaults/default-options))
  ([opts]
   (if (some-> (jlink-instance/get) (proto/kernel-link?)) ; need both, b/c some tests only init jlink
     {:status :ok
      :wolfram-version (:wolfram-version (deref kernel-info 1 :N/A))}
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
       {:status :ok
        :wolfram-version (:wolfram-version (deref kernel-info 1 :N/A))
        :start!ed? true}))))

(defn stop!
  "Sends a request to the kernel to shut down.

  See https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/KernelLink.html#terminateKernel()"
  []
  (some-> (jlink-instance/get) (proto/terminate-kernel!))
  (jlink-instance/reset!)
  (reset! kernel-link-atom nil)
  {:status :ok})

(defn restart!
  "Same as calling [[stop!]] and then [[start!]]."
  ([] (stop!) (start!))
  ([opts] (stop!) (start! opts)))

(defn eval
  "Evaluate the given Wolfram expression (a string, or a Clojure data) and return the result as Clojure data.

   Args:
   - `opts` - same as those for [[start!]], especially `:aliases` and `:flags` (see
      wolframite.runtime.defaults/default-flags)

    Example:
    ```clojure
    (wl/eval \"Plus[1,2]\")
    ; => 3
    (wl/eval '(Plus 1 2))`
    ; => 3
    ```

    Tip: Use [[->wl]] to look at the final expression that would be sent to Wolfram for evaluation."
  ([expr] (eval expr {}))
  ([expr eval-opts]
   (if-let [jlink-inst (jlink-instance/get)]
     (let [with-eval-opts (merge {:jlink-instance jlink-inst}
                                 (:opts jlink-inst)
                                 eval-opts)
           expr' (if (string? expr) (express/express expr with-eval-opts) expr)]
       (some-> (cep/cep expr' with-eval-opts)
               kindly-support/maybe-add-kindly-meta))
     (throw (IllegalStateException. "Not initialized, call start! first")))))

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

(defn wolfram-expr
  "Mark the string `str` as containing a Wolfram expression, which should be evaluated as such.
  This is useful when you need to nest a raw Wolfram expression in a bigger Wolframite expression
  to work around a yet unsolved limitation of Wolframite."
  [^String str]
  {:pre [(string? str)]}
  (convert/->wolfram-str-expr str))

(defn ->clj
  "Turn the given Wolfram expression string into its Clojure data structure form.

  Ex.: `(->clj \"Power[2,3]\") ; => (Power 2 3)`"
  [s]
  ;; A hack to force interpretation
  (eval (convert/->wolfram-str-expr s)
        {:flags [:no-evaluate]})) ; the flag tells us to conver the string to Expr then parse to Clj

(defn ->wl
  "Convert Clojure forms to instances of Wolfram's Expr class.
  Generally useful, especially for working with graphics - or for troubleshooting
  what will be sent to Wolfram for evaluation."
  ([clj-form] (->wl clj-form {:output-fn str}))
  ([clj-form {:keys [output-fn] :as opts}]
   (cond-> (convert/convert clj-form (merge {:jlink-instance (jlink-instance/get)
                                             :kernel/link @kernel-link-atom} opts))
     (ifn? output-fn) output-fn)))

(defn load-package!
  "An extended version of Wolfram's 'Get'. Gets a Wolfram package and makes the constants/functions etc. accessible via a Clojure namespace (the given `alias`, by default the same as `context`).

  Example:  `(<<! \"./resources/WolframPackageDemo.wl\" \"WolframPackageDemo\" 'wp)`

 - `path` - string pointing to the package file
 - `context` - string for Wolfram context (essentially Wolfram's version of a namespace)
 - `alias` - Clojure symbol to be used for accessing the Wolfram context. This will effectively become a Clojure namespace

See `package/intern-context!` for details of turning the Wolfram context into a Clojure namespace."
;; TODO: Should check that the symbol isn't already being used.
  ([path]
   (let [context (-> path fs/file-name fs/strip-ext)]
     (package/load! eval path context (symbol context))))

  ([path context]
   (package/load! eval path context (symbol context)))

  ([path context alias]
   (eval (w/Get path))
   (package/intern-context! eval context alias)))

(def <<!
  "A Wolfram-like alias to load-package!. An extended version of Wolfram's 'Get'. Gets a Wolfram package and makes the constants/functions etc. accessible via a Clojure namespace (the given `alias`, by default the same as `context`)."
  load-package!)

(comment
  ;; Initialization/alias test
  (start! {:aliases
           '{** Power}})
  (eval '(** 5 2))
  (eval (w/Dot [1 2 3] [4 5 6]))
  (stop!))

(comment
  ;; Package test
  (start!)

  (<<! "resources/WolframPackageDemo.wl")
  (load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo")
  (load-package! "resources/WolframPackageDemo.wl" "WolframPackageDemo" 'wd)

  (eval  (w/Information wd/tryIt "Usage"))
  (eval (wd/tryIt 10))
  (eval  (w/Information WolframPackageDemo/additional "Usage"))
  (eval (WolframPackageDemo/additional 10)))
