(ns user
  (:use [clojuratica])
  (:import [com.wolfram.jlink MathLinkFactory]))

;; http://reference.wolfram.com/mathematica/JLink/ref/java/com/wolfram/jlink/MathLinkFactory.html

(defn init []
  (def osx-cmdline "-linkmode launch -linkname '\"/Applications/Mathematica.app/Contents/MacOS/MathKernel\" -mathlink'")

  #_(def osx-cmdline "-linkmode connect -linkname dog2")
  #_(def osx-cmdline "-linkmode connect -linkname 9999 -linkprotocol TCPIP")

  #_(System/setProperty "com.wolfram.jlink.libdir" "/Applications/Mathematica.app/SystemFiles/Links/JLink/")

  (def kernel-link (MathLinkFactory/createKernelLink osx-cmdline))
  (.discardAnswer kernel-link)
  (defonce math-evaluate (math-evaluator kernel-link))
  (def-math-macro math math-evaluate))
