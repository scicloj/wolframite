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
  (:refer-clojure :exclude [intern])
  (:require
   ;; This namespace must be loaded before any other code will work, since this is what adds the jlink jar to
   ;; the classpath
   [clojuratica.jlink :as jlink]
   [clojuratica.lib.options :as options]
   [clojuratica.runtime.dynamic-vars :as dynamic-vars]
   [clojuratica.runtime.default-options :as default-options]
   [clojuratica.base.cep :as cep]
   [clojuratica.base.convert :as convert]
   [clojuratica.base.evaluate :as evaluate]
   [clojuratica.base.kernel :as kernel]
   [clojuratica.integration.intern :as intern]
   [clojuratica.runtime.defaults :as defaults])
  (:import com.wolfram.jlink.MathLinkFactory))

;; http://reference.wolfram.com/mathematica/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html

(def kernel-link-atom (atom nil))

(defn kernel-link-opts [platform]
  (format "-linkmode launch -linkname '\"%s\" -mathlink'" (jlink/get-mathlink-path platform)))

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

#_(options/defn-let-options math-evaluator [enclosed-options default-options/*default-options*] [kernel-link & [init]]
  (let [enclosed-kernel (kernel/kernel kernel-link)]
    (binding [dynamic-vars/*options* enclosed-options
              dynamic-vars/*kernel*  enclosed-kernel]
      (evaluate/evaluate (convert/convert init))
      (evaluate/evaluate (convert/convert '(Needs "Parallel`Developer`")))
      (evaluate/evaluate (convert/convert '(Needs "Developer`")))
      (evaluate/evaluate (convert/convert '(ParallelNeeds "Developer`")))
      (evaluate/evaluate (convert/convert '(Needs "ClojurianScopes`")))
      (evaluate/evaluate (convert/convert '(ParallelNeeds "ClojurianScopes`")))
      (evaluate/evaluate (convert/convert '(Needs "HashMaps`")))
      (evaluate/evaluate (convert/convert '(ParallelNeeds "HashMaps`"))))
    ;#_{:clj-kondo/ignore {:unresolved-symbol #{'expr}}}
    (options/fn-binding-options [dynamic-vars/*options* enclosed-options] [expr]
      (binding [dynamic-vars/*kernel* enclosed-kernel]
        (cep/cep expr)))))

;; (defmacro math-intern [& args]
;;   (options/let-options [options args {#{:as-function :as-macro} :as-macro
;;                                       #{:no-scopes :scopes}     :no-scopes}]
;;                        [math-eval & opspecs]
;;      (let [opspecs (if (options/flag? options :scopes)
;;                      (into opspecs (keys (default-options/*default-options* :clojure-scope-aliases)))
;;                      opspecs)]
;;        (if (options/flag? options :as-macro)
;;          `(intern/intern :macro '~math-eval ~@(map (fn [opspec#] (list 'quote opspec#)) opspecs))
;;          `(intern/intern :fn    '~math-eval ~@(map (fn [opspec#] (list 'quote opspec#)) opspecs))))))

;; (defmacro def-math-macro [m math-eval]
;;   `(math-intern :as-macro ~math-eval [~m ~'CompoundExpression]))

;; FIXME: bring back!
;; (defn wl->clj [s math-eval]
;;   (math-eval :no-evaluate (list 'quote s)))

;; (defn clj->wl
;;   "Convert clojure forms to mathematica Expr.
;;   Generally useful, especially for working with graphics."
;;   [clj-form {:keys [kernel-link output-fn]}]
;;   (binding [dynamic-vars/*options* default-options/*default-options*
;;             dynamic-vars/*kernel*  kernel-link]
;;     (cond-> (convert/convert clj-form)
;;       (ifn? output-fn) output-fn)))

(defn init!
  "Provide platform identifier as one of: `:linux`, `:macos` or `:windows`
  Defaults to platform identifier based on `os.name`"
  ([]
   (init! (jlink/platform-id (System/getProperty "os.name"))))
  ([platform]
   {:pre [(jlink/supported-platform? platform)]}
   (let [kl (doto (MathLinkFactory/createKernelLink (kernel-link-opts platform))
              (.discardAnswer))]
     (reset! kernel-link-atom kl)
     kl)))

(defn make-wl-evaluator [opts]
  (when-not (instance? com.wolfram.jlink.KernelLink @kernel-link-atom) (init!))
  (let [initialized-opts (merge {:kernel/link @kernel-link-atom} opts)]
    (evaluator-init initialized-opts)
    (fn [expr]
      (cep/cep expr initialized-opts))))

(def wl (make-wl-evaluator defaults/default-options))
