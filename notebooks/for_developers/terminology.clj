(ns for-developers.terminology
  (:require [scicloj.kindly.v4.kind :as k]
            [wolframite.core :as wl]
            [wolframite.wolfram :as w]
            wolframite.runtime.defaults))

;; TODO Lists, associations, ranges, ...

(wl/start)

(k/md "# Intro

Wolframite works by translating Clojure data into Wolfram JLink Expr(essions) and sending them to
a Wolfram process for evaluation, then translating the response back into data.

There are two ways of writing these expression. The first one is the _raw (quoted) data form_, with symbols corresponding to Wolfram symbols or Wolframite
_aliases_ that are mapped to such symbols. Example:")

(wl/eval '(+ 1 (Minus 1)))

(k/md "This uses Wolframite aliases, including `+ -> Plus`:")

(k/table
  [wolframite.runtime.defaults/all-aliases])

(k/md "We could also use the aliased symbol directly:")

(wl/eval '(Plus 1 (Minus 1)))

(k/md "
The second is the _evaluated form_, which uses vars from `wolframite.wolfram` and is much more convenient.")

(wl/eval (w/+ 1 (w/Minus 1))) ; w/ alias

(wl/eval (w/Plus 1 (w/Minus 1)))

(k/md "# Expressions

Wolframite expressions consist of stuff that can be translated to Wolfram: data structures (`[]` and `{}`),
constants, and Wolfram symbols (plus Wolframite aliases). There are also few more \"Wolframite-isms\" that
 we support, namely `w/fn, let, do`")

(wl/eval '(do (- 1) (+ 1 2)))
(wl/eval '(let [x 1, y 2] (+ x y))) ; FIXME not working?!

;; -----------------------------------------------------------------------------

(k/md "
In

```wolfram
Import[\"/Users/holyjak/tmp/delme/demo.csv.gz\", {\"Data\", 1 ;; 3}, \"HeaderLines\" -> 1]
```

the `\"HeaderLines\" -> 1` part is a _Rule_.
") ; TODO What are Rules ?!

(k/md "## Aside: Translating Wolfram to Wolframite

How do we translate from Wolfram:

```wolfram
Import[\"demo.csv.gz\", {\"Data\", 1 ;; 3}, \"HeaderLines\" -> 1]
```

to Wolframite Clojure? Let's ask for help!

")

(wl/->clj "Import[\"demo.csv.gz\", {\"Data\", 1 ;; 3}, \"HeaderLines\" -> 1]")

(k/md "
Now, we would need to quote that, as discussed in the Wolframite Clojure Primer, and thus prefer our
convenience functions of the evaluated form:

To:
```clojure
(wl/eval (w/Import \"demo.csv.gz\"
                   [\"Data\" (w/Span 1 3)],
                   (w/-> \"HeaderLines\" 1)))
```
")

(k/md "## Errors

When there is a syntactic or semantic error in your expression, you often get the same, unevaluated expression back:")

(wl/eval (w/FromDigits "-87.6")) ; wrong

(wl/eval (w/FromDigits "87")) ; correct