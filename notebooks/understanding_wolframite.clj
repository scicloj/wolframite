;; # Understanding Wolframite {#sec-understanding-wolframite}
;;
;; Where we learn a little more about how Wolframite works, so that you can use it effectively.
;;
;; If you are in a hurry, you can just skim it to know what answers you can find here, when you need them.
;; We will refer to it from other parts of the documentation.
;;
;; First, some required requires:
(ns understanding-wolframite
  (:require [scicloj.kindly.v4.kind :as k]
            [wolframite.core :as wl]
            [wolframite.wolfram :as w]
            wolframite.runtime.defaults))

(k/md "Next, we need to actually start a [Wolfram Kernel](https://reference.wolfram.com/language/ref/program/WolframKernel.html)
and connect to it:")

(wl/start)

(k/md "## How it all works

Wolframite works by translating Clojure data into Wolfram JLink [Expr](https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/Expr.html)(essions)
 and sending them to an external, Wolframite-managed Wolfram process for evaluation, then translating the response back into data.
")

(k/md "## Expressions

Wolframite expressions consist of stuff that can be translated to Wolfram: data structures (`[]` for Lists and `{}` for Associations),
constants, and Wolfram symbols (plus Wolframite aliases). There are also some \"Wolframite-isms\" that
 we support, namely `w/fn` for defining ad-hoc functions, primarily for use with `Map`.

There are three ways of writing these expression.

### Raw form {#sec-raw-form}

The first one is the _raw (quoted) data form_,with symbols corresponding to Wolfram symbols or Wolframite _aliases_
that are mapped to such symbols:")

(wl/eval '(+ 1 (Minus 1)))

;; ### Aside: Wolframite aliases {#sec-aliases-table}
;;
;; Aside of symbols that directly correspond to Wolfram symbols, you can also use some Wolframite aliases, which
;; provide alternative names for these symbols. We have used above `+`, which is an alias of `Plus`. Here are
;; all the built-in aliases that we currently support:

(k/table {:column-names [:Alias :Wolfram],
          :row-vectors (-> wolframite.runtime.defaults/all-aliases
                           (dissoc '-)
                           (assoc '- "Minus or Subtract"))})

(k/md "We could also use the aliased symbol directly:")

(wl/eval '(Plus 1 (Minus 1)))

;; ### Evaluated form {#sec-evaluated-form}
;;
;; The second is the _evaluated form_, which uses vars from the [`wolframite.wolfram`](https://github.com/scicloj/wolframite/blob/main/src/wolframite/wolfram.clj)
;; namespace and is much more convenient, enabling autocomplete and mixing with evaluated Clojure code:)

(wl/eval (w/+ (clojure.core/- 5 4) (w/Minus 1)))

; Notice that we have convenience vars for both Wolfram symbols and Wolframite aliases. We could also rewrite it without the alias:

(wl/eval (w/Plus (clojure.core/- 5 4) (w/Minus 1)))

;; Notice that we can mix Clojure and Wolfram - but the Clojure parts are evaluated on our side,
;; before the expression is translated and sent to Wolfram.
;;
;; The evaluated form is actually translated into the raw form before being evaluated, as we can see if
;; we run it on its own, without passing it to `wl/eval`:
(w/Plus (clojure.core/- 5 4) (w/Minus 1))

;; You can see here that the convenience functions from the `w/` namespace essentially evaluate to
;; themselves in the symbolic, unevaluated form.

;; ### Wolfram string form {#sec-wolfram-string-form}
;;
;; There is one more form, the _Wolfram string form_, which is the raw Wolfram code as a string:

(wl/eval "Plus[1,Minus[1]]")

;; This is useful when you just want to paste in some Wolfram of when you need to use a feature that
;; we don't yet support.

;; ### Mixing different kinds of forms
;;
;; The other forms may be nested the evaluated one. You'd typically need that when Wolframite evaluated form
;; does not support (yet) what you are trying to do. When nesting a Wolfram string form, we need to
;; explicitly tell Wolframite to treat it as an expression and not just as a primitive string:

(wl/eval (w/Plus '(Internal/StringToMReal "-1.5") (wl/wolfram-expr "Minus[3]")))

;; ### Aside: Wolfram modules and fully qualified names
;;
;; Most Wolfram functions are global, but they can also be placed inside modules and need to be referred to by their
;; fully qualified name. While Wolfram using `` ` `` to separate module and symbol, we use `/`. Thus, these two are equivalent:

(wl/eval "Internal`StringToMReal[\"-1.5\"]")

(wl/eval '(Internal/StringToMReal "-1.5"))

;; (Of course, you normally do not want to use the Internal/* functions, as they may disappear or change between Wolfram versions.)

;; -----------------------------------------------------------------------------

(k/md "## Aside: Translating Wolfram to Wolframite

When looking up how to do a certain thing in Wolfram, you will get a Wolfram answer.
Thus you need to know how to translate it from Wolfram to Wolframite. Let's say you have
this Wolfram snippet:

```wolfram
Import[\"demo.csv.gz\", {\"Data\", 1 ;; 3}, \"HeaderLines\" -> 1]
```

How do you turn it into Wolframite Clojure? Let's ask for help!

")

(wl/->clj "Import[\"demo.csv.gz\", {\"Data\", 1 ;; 3}, \"HeaderLines\" -> 1]")

(k/md "
Now, we either need to quote that to turn it into the raw form, or rewrite it using the
convenience functions of the evaluated form:
")

^:kindly/hide-code
(quote
 (wl/eval (w/Import "demo.csv.gz"
                    ["Data" (w/Span 1 3)],
                    (w/-> "HeaderLines" 1))))

(k/md "## Errors

When there is a syntactic or semantic error in your expression, you often get the same, unevaluated expression back:")

(wl/eval (w/FromDigits "-87.6"))

;; Correct:
(wl/eval (w/FromDigits "87"))
