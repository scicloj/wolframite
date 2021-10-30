(ns dev
  (:require [clojuratica :as wl])
  (:import [com.wolfram.jlink MathLinkFactory]))

;; http://reference.wolfram.com/mathematica/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html

(defn init-osx []
  (def cmdline "-linkmode launch -linkname '\"/Applications/Mathematica.app/Contents/MacOS/MathKernel\" -mathlink'")

  #_(def cmdline "-linkmode connect -linkname 9999 -linkprotocol TCPIP")

  (def kernel-link (MathLinkFactory/createKernelLink cmdline))
  (.discardAnswer kernel-link)
  (defonce math-evaluate (wl/math-evaluator kernel-link))
  (wl/def-math-macro math math-evaluate))

(defn init-win []
  (def cmdline "-linkmode launch -linkname '\"/c:/Program Files/Wolfram Research/Mathematica/11.3/MathKernel.exe\"'")

  (def kernel-link (MathLinkFactory/createKernelLink cmdline))
  (.discardAnswer kernel-link)
  (defonce math-evaluate (wl/math-evaluator kernel-link))
  (wl/def-math-macro math math-evaluate))

(defn init-linux []
  (def cmdline "-linkmode launch -linkname '\"/usr/local/Wolfram/Mathematica/12.0/Executables/MathKernel\" -mathlink'")
  (def kernel-link (MathLinkFactory/createKernelLink cmdline))
  (.discardAnswer kernel-link)
  (defonce math-evaluate (wl/math-evaluator kernel-link))
  (wl/def-math-macro math math-evaluate))
