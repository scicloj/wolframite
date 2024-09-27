(ns advanced.customizing-wolframite
  (:require [scicloj.kindly.v4.kind :as k]
            [wolframite.core :as wl]))

(k/md "#### Customizing Wolframite

A big advantage of Wolframite (as opposed to its earlier incarnations) is that we can now individually tailor the user experience at the level of initialization:")
(wl/restart! {:aliases '{🔋 Power}})
(wl/eval '(🔋 2 5))

;; , and function call,
(wl/restart!)
(wl/eval '(🔋🔋 2 5) {:aliases '{🔋🔋 Power}})

;; Use it how you want to!

(k/md "
TIP: You can also get convenience vars for your aliases in `wolframite.wolfram` by running something like `(wolframite.impl.wolfram-syms.write-ns/write-ns! <path> {:aliases '{** Power}})`. After you load the file, you'll be able to use `(wl/eval (w/** 2 5) {:aliases '{** Power}})`.
")
