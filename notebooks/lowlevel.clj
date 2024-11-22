(ns lowlevel
  "Documenting operations that are carried out at a low level of abstraction."
  (:require
   [wolframite.api.v1 :as wl]
   [wolframite.wolfram :as w]
   [wolframite.impl.jlink-instance :as jlink-instance]
   [wolframite.base.parse :as parse]
   [scicloj.kindly.v4.kind :as k]))

(k/md "# Low level interoperation

Here we list features that should only be used in rare use cases. Although up to date, these operations will not necessarily be preserved in the future.")

(k/md "## Explicit interning

Although it shouldn't normally be necessary, we can also intern Wolfram functions more directly i.e. to effectively define Wolfram functions as Clojure functions.

The standard way of doing this is something like")
(def greetings
  (wl/!
   (w/fn [x] (w/StringJoin "Hello, " x "! This is a Mathematica function's output."))))
(greetings "Stephen")

(k/md "But this can also be done at a lower level, e.g.")
(def W:Plus
  (parse/parse-fn 'Plus {:jlink-instance (jlink-instance/get)}))
(W:Plus 1 2 3)
