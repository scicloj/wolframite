(ns clojuratica.init
  "NOTE: this is a side effecting namespace!
  Loading this namespaces will:
  * modify the classpath (adding JLink jar)
  * run the `init!`
    * creating KernelLink
    * exposing a `WL` macro"
  (:require [clojuratica.jlink :as jlink]))

(jlink/add-jlink-to-classpath)

(require '[clojuratica :as wl])
(import 'com.wolfram.jlink.MathLinkFactory)

;; http://reference.wolfram.com/mathematica/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html

(declare kernel-link
         math-evaluate
         WL)

(defn kernel-link-opts [platform]
  (format "-linkmode launch -linkname '\"%s\" -mathlink'" (jlink/get-mathlink-path platform)))

(defn init!
  "Provide platform identifier as one of: `:linux`, `:macos` or `:windows`
  Defaults to platform identifier based on `os.name`"
  ([]
   (init! (jlink/platform-id (System/getProperty "os.name"))))
  ([platform]
   {:pre [(jlink/supported-platform? platform)]}
   #_{:clj-kondo/ignore true}
   (def kernel-link (MathLinkFactory/createKernelLink (kernel-link-opts platform)))
   (.discardAnswer kernel-link)

   #_{:clj-kondo/ignore true}
   (def math-evaluate (wl/math-evaluator kernel-link))
   (wl/def-math-macro WL clojuratica.init/math-evaluate)))

(init!)
