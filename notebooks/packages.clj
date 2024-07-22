(ns packages
  (:require
   [wolframite.core :as wl]
   [wolframite.wolfram :as w]
   [wolframite.base.package :as wp]

   [scicloj.kindly.v4.kind :as k]))

(k/md "# How to work with packages

Working with packages has never been easier! Following the Wolfram symbol convention, we introduce Wolframite's version of '<<', a.k.a 'Get', to both load the given context into the working kernel and to attach the Wolfram context's symbols (including functions) onto a clojure namespace: either using the context name as a symbol or the clojure symbol provided.

The functions inside the  Wolfram package can then be used just like any other Wolfram functions.

For example, try loading 'WolframPackage'. There are two functions defined inside:
```wolfram
  tryIt[x_] :=
    Module[{y},
      x^3
    ]

  additional[y_]:=3*y
```
")

(wl/start)
(wp/<< "resources/wolframPackage.wl" "WolframPackage")

(wl/eval (WolframPackage/tryIt 10))
(wl/eval (WolframPackage/additional 10))
(wl/eval  (w/Information WolframPackage/additional "Usage"))

(k/md "That's it! As you can see, the functions are callable and the documentation is available too.

If you want to change the context name, e.g. to make it shorter, then this is also simple:")

(wp/<< "resources/wolframPackage.wl" "WolframPackage" 'pck)
(wl/eval  (w/Information pck/tryIt "Usage"))
(wl/eval (pck/tryIt 10))

(k/md "And so, you have the whole power of Wolfram packages at your fingertips. And to be honest, this is actually easier to work with than using Wolfram's contexts directly. ")

(comment
  (require '[scicloj.clay.v2.api :as clay]))
