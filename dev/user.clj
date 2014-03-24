(ns user
  (:use [clojuratica])
  (:import [com.wolfram.jlink MathLinkFactory]))

;; http://reference.wolfram.com/mathematica/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html

(defn init-osx []
  (def cmdline "-linkmode launch -linkname '\"/Applications/Mathematica.app/Contents/MacOS/MathKernel\" -mathlink'")

  #_(def cmdline "-linkmode connect -linkname 9999 -linkprotocol TCPIP")

  (def kernel-link (MathLinkFactory/createKernelLink cmdline))
  (.discardAnswer kernel-link)
  (defonce math-evaluate (math-evaluator kernel-link))
  (def-math-macro math math-evaluate))
