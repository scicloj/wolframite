;; ^{:clay {:kindly/options {:kinds-that-hide-code #{:kind/md :kind/hiccup :kind/html :kind/tex}}}}
(ns packages
  "A notebook demonstrating how to import and use Wolfram packages in Wolframite."
  (:require
   [wolframite.core :as wl]
   [wolframite.wolfram :as w]
   [wolframite.base.package :as wp]

   [scicloj.kindly.v4.kind :as k]))

^:kindly/hide-code
(-> (scicloj.kindly-advice.v1.api/add-advisor! (fn [{:keys [value]}] (when (nil? value) [[:kind/hidden]])))
    :k/hidden)

(k/md "# How to work with packages

Working with packages has never been easier! Following the Wolfram symbol convention, we introduce Wolframite's version of '<<', a.k.a 'Get', to both load the given context into the working kernel and to attach the Wolfram context's symbols (including functions) onto a clojure namespace: either using the context name as a symbol or the clojure symbol provided.

The functions inside the  Wolfram package can then be used just like any other Wolfram functions.

For example, try loading 'WolframPackageDemo'. There are two functions defined inside:
```wolfram
  tryIt[x_] :=
    Module[{y},
      x^3
    ]

  additional[y_]:=3*y
```
")

(wl/start)
(wp/<< "resources/WolframPackageDemo.wl" "WolframPackageDemo")

(wl/eval (WolframPackageDemo/tryIt 10))
(wl/eval (WolframPackageDemo/additional 10))
(wl/eval  (w/Information WolframPackageDemo/tryIt "Usage"))

(k/md "That's it! As you can see, the functions are callable and the documentation is available too.

If you want to change the context name, e.g. to make it shorter, then this is also simple:")

(wp/<< "resources/WolframPackageDemo.wl" "WolframPackageDemo" 'pck)
(wl/eval (pck/tryIt 10))
(wl/eval  (w/Information pck/additional "Usage"))

(k/md "And so, you have the whole power of Wolfram packages at your fingertips. And to be honest, this is actually easier to work with than using Wolfram's contexts directly. Clojure does namespaces well.")
