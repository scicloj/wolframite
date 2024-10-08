(ns advanced.packages
  "A notebook demonstrating how to import and use Wolfram packages in Wolframite."
  (:require
    [scicloj.kindly.v4.kind :as k]
    [wolframite.core :as wl]
    [wolframite.wolfram :as w]))

(k/md "# Packages

Working with packages has never been easier! Following the Wolfram symbol convention, we introduce Wolframite's version of `<<`, *a.k.a.* `Get`, to both load the given file (`*.wl` or `*.m`) into the working kernel and to attach the Wolfram context's symbols (including functions) to a Clojure namespace. The name of the namespace defaults to the context's name, but you can also define a custom name, passed in as a Clojure symbol.

The functions inside the  Wolfram package can then be used just like any other Wolfram functions.

For example, try loading `WolframPackageDemo.wl` and running the two functions defined inside:
```
  tryIt[x_] :=
    Module[{y},
      x^3
    ]
```
and
```
additional[y_]:=3*y
```
.
")

(wl/restart!)
(wl/<<! "resources/WolframPackageDemo.wl")

(wl/eval
  (WolframPackageDemo/tryIt 10))
(wl/eval
  (WolframPackageDemo/additional 10))
(wl/eval
  (w/Information WolframPackageDemo/tryIt "Usage"))

(k/md "That's it! As you can see, the functions are callable and the documentation is available too.

If you want to change the context name, *e.g.* to make it shorter, then this is also simple. In general, we allow for the package name and context name to be different, so the full call is `(... path.wl context alias)`, *i.e.*")
(wl/<<! "resources/WolframPackageDemo.wl" "WolframPackageDemo" 'pck)
(wl/eval (pck/tryIt 10))
(wl/eval  (w/Information pck/additional "Usage"))

(k/md "And so, you have the whole power of Wolfram packages at your fingertips. And to be honest, this is actually easier to work with than using Wolfram's contexts directly. Clojure does namespaces well.")
