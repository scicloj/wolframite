;; # Understanding Wolframite {#sec-understanding-wolframite}
;;
;; Where you learn a little more about how Wolframite works, so that you can use it effectively.
;;
;; If you are in a hurry, you can just skim through this to know what answers you can find here when you need them.
;; We will refer to it from other parts of the documentation.
;;
;; First, we need some namespaces:
(ns understanding-wolframite
  (:require [scicloj.kindly.v4.kind :as k]
            [clojure.repl :as repl]
            [wolframite.core :as wl]
            [wolframite.lib.helpers :as h]
            [wolframite.wolfram :as w]
            wolframite.runtime.defaults))

(k/md "Next, we need to actually start a [Wolfram Kernel](https://reference.wolfram.com/language/ref/program/WolframKernel.html)
and connect to it:")

(wl/start!)

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

The first one is the _raw (quoted) data form_, using actual Clojure symbols corresponding to Wolfram symbols or Wolframite _aliases_.
Notice the `'` quote in front of the expression, telling Clojure Reader not to evaluate it but return it as-is:")

(wl/eval '(+ 1 (Minus 1)))

;; We could also be more explicit and replace `'` with the `quote` that it turns into under the hood:

(wl/eval (quote (+ 1 (Minus 1))))

;; However, quoting the whole form does not allow us to have any evaluations inside the whole expression.
;; We can instead build the form manually, quoting only the symbols:

(wl/eval (list 'Plus 1 (list 'Minus 1)))

;; ### Advanced: Syntax quote
;;
;; As mentioned, quoting a form makes it impossible to refer to any vars or evaluate any code within the form.
;; We can bypass the problem by not quoting the whole form but replacing `(...)` with `(list ...)` and quoting each symbol.
;; But there is yet another way, used by Clojure macros - namely [the syntax quote](https://clojure.org/reference/reader#syntax-quote) ``` ` ```.
;; It makes it possible to evaluate things within the quoted form by prefixing them with the "unquote" `~` (or "splicing unquote" `~@`).
;; The only problem here is that ``` ` ``` automatically fully qualifies all symbols, as we can see here:

(do `(Minus 42))

;; This would of course break our translation to Wolfram. There is fortunately one trick to tell the Clojure Reader to keep a symbol unqualified
;; by combining `~'`, essentially telling it "evaluate this expression, which returns a simple symbol":

(do `(~'Minus 42))

;; ### A word on aliases

;; When we come back to our original expression, `'(+ 1 (Minus 1))`, you may notice that `+` is not actually a Wolfram function.
;; It is a Wolframite alias (see @sec-aliases-table), which we replace with `Plus` before we send it to Wolfram.
;; You can read about it further down this document.

;; ### Evaluated form {#sec-evaluated-form}
;;
;; The second form is the _evaluated form_, which uses vars from the [`wolframite.wolfram`](https://github.com/scicloj/wolframite/blob/main/src/wolframite/wolfram.clj)
;; namespace and is much more convenient, enabling autocomplete and mixing with evaluated Clojure code:

(wl/eval (w/+ (clojure.core/- 5 4) (w/Minus 1)))

; Notice that we have convenience vars for both Wolfram symbols and Wolframite aliases and thus both of the following work:

(=
  (wl/eval (w/+ 1 (w/- 1)))
  (wl/eval (w/Plus 1 (w/Minus 1))))

;; Notice that we can mix Clojure and Wolfram - but the Clojure parts are evaluated on our side,
;; before the rest of the expression is translated and sent to Wolfram.
;;
;; The evaluated form is actually translated into the raw form before being evaluated, as we can see if
;; we run it on its own, without passing it to `wl/eval`:
(w/Plus (clojure.core/- 5 4) (w/Minus 1))

;; You can see here that the convenience functions from the `w/` namespace essentially evaluate to
;; themselves in the symbolic, unevaluated form - `(w/Plus arguments...)` becomes `'(Plus arguments...)`.

;; ### Wolfram string form {#sec-wolfram-string-form}
;;
;; There is one more form, the _Wolfram string form_, which is the raw Wolfram code in a string:

(wl/eval "Plus[1,Minus[1]]")

;; This is useful when you just want to paste in some Wolfram of when you need to use a feature that
;; we don't yet support.

;; ### Mixing different kinds of forms
;;
;; The evaluated form may also contain sections in the other forms. You'd typically need that when Wolframite evaluated form
;; does not (yet) support that which you are trying to do. When nesting a Wolfram string form, we need to
;; explicitly tell Wolframite to treat it as an expression and not just as a primitive string, by passing it through `wl/wolfram-expr`:

(wl/eval (w/Plus
           '(Internal/StringToMReal "-1.5")
           (wl/wolfram-expr "Minus[3]")))

;; ### Aside: Wolframite aliases {#sec-aliases-table}
;;
;; Aside of symbols that directly correspond to Wolfram symbols, you can also use Wolframite aliases. The aliases
;; provide alternative names for Wolfram symbols. We have used above `+`, which is an alias of `Plus`. Here are
;; all the built-in aliases that we currently support:

(k/table {:column-names [:Alias :Wolfram],
          :row-vectors (-> wolframite.runtime.defaults/all-aliases
                           (dissoc '-)
                           (assoc '- "Minus or Subtract"))})

(k/md "Thus, the following two expressions are equivalent")

(wl/eval '(+ 1 (- 1)))

(wl/eval '(Plus 1 (Minus 1)))

;; ### Aside: Wolfram modules and fully qualified names
;;
;; Most Wolfram functions are global, but they can also be placed inside modules and need to be referred to by their
;; fully qualified names. While Wolfram uses `` ` `` to separate module and symbol, we write it as `/`. Thus, these two are equivalent:

(wl/eval "Internal`StringToMReal[\"-1.5\"]")

(wl/eval '(Internal/StringToMReal "-1.5"))

;; (Of course, you normally do not want to use the Internal/* functions, as they may disappear or change between Wolfram versions.)

;; -----------------------------------------------------------------------------

(k/md "## Aside: Translating Wolfram to Wolframite

When asking the internets or ChatGPT how to do a certain thing in Wolfram, you will get a Wolfram answer.
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

;; On the other hand, some operations are coded to return the symbol `$Failed` - read the doc strings.

(k/md "## Documentation

We can leverage Clojure repl's documentation support with the Wolfram convenience vars:")

(with-out-str
  (repl/doc w/GeoGraphics))

;; Search for symbols (case-insensitive):
(->> (repl/apropos #"(?i)geo")
     (drop 2)
     (take 3))

;; Search complete docstrings for a pattern:
(-> (with-out-str
      (repl/find-doc "two-dimensional"))
    (subs 0 500))

(k/md "
If we evaluate `(h/help! 'ArithmeticGeometricMean)` then it will open the Wolfram documentation page for `ArithmeticGeometricMean`.

We could instead ask for the link(s):")

(h/help! w/ArithmeticGeometricMean :links true)

(k/md "`h/help!` also works on whole expressions, providing docs for each symbol:")

(h/help! '(GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"])) :links true)

;; (Notice that help! works both with symbols and our convenience vars.)