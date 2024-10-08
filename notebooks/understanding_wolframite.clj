;; # Understanding Wolframite {#sec-understanding-wolframite}
;;
;; Where you learn a little more about how Wolframite works, so that you can use it effectively.
;;
;; If you are in a hurry, you can just skim through this to know what answers you can find here when you need them.
;; Though make sure to learn about the three types of expressions → @sec-expressions that Wolframite supports.
;; We will refer to this chapter from other parts of the documentation.
;;
;; First, we need some namespaces:
(ns understanding-wolframite
  (:require
    [clojure.repl :as repl]
    [scicloj.kindly.v4.api :as kindly]
    [scicloj.kindly.v4.kind :as kind]
    [scicloj.kindly.v4.kind :as k]
    [wolframite.core :as wl]
    [wolframite.lib.helpers :as h]
    [wolframite.runtime.defaults]
    [wolframite.wolfram :as w :refer :all :exclude [* + - -> / < <= = == > >= Byte Character Integer Number Short String Thread fn]]))

(k/md "Next, we need to actually start a [Wolfram Kernel](https://reference.wolfram.com/language/ref/program/WolframKernel.html)
and connect to it:")

(wl/start!)

(k/md "## How it all works

Wolframite works by translating Clojure data into Wolfram JLink [Expr](https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/Expr.html)(essions)
 and sending them to an external, Wolframite-managed Wolfram process for evaluation, then translating the response back into data.
")

(k/md "## Expressions {#sec-expressions}

Wolframite expressions consist of stuff that can be translated to Wolfram: data structures (`[]` for Lists and `{}` for Associations),
constants, and Wolfram symbols (plus Wolframite aliases → @sec-aliases-table). There are also some \"Wolframite-isms\" that
 we support, namely `w/fn` for defining ad-hoc functions, primarily for use with `Map`.

There are three ways of writing these expressions.
")

(k/md "### Evaluated form {#sec-evaluated-form}

The primary form you will encounter is the _evaluated form_, which uses vars from the [`wolframite.wolfram`](https://github.com/scicloj/wolframite/blob/main/src/wolframite/wolfram.clj)
namespace as proxies for the actual Wolfram functions and symbols. It is the most convenient form, with support for code completion and mixing with evaluated Clojure code:")
(wl/eval (w/+ (clojure.core/- 5 4) (w/Minus 1)))

; When requiring `wolframite.wolfram`, we can "refer" most of its symbols, excluding those that conflict with Clojure or Java (such as `+`;
; have a look at the `require` at the top of this page), and thus we can also write:
(wl/eval (w/+ (- 5 4) (Minus 1)))

; Notice that we have convenience vars for both Wolfram symbols and Wolframite aliases (see below) and thus both of the following work:

(=
  (wl/eval (w/+ 1 (w/- 1)))
  (wl/eval (Plus 1 (Minus 1))))

;; We can also mix Clojure and Wolfram - but the Clojure parts are evaluated on our side,
;; before the rest of the expression is translated and sent to Wolfram.
;;
;; The evaluated form is actually translated into the _raw form_ (see @sec-raw-form) before being evaluated, as we can see if
;; we run it on its own, without passing it to `wl/eval`:
(w/Plus (- 5 4) (w/Minus 1))

;; You can see here that the convenience functions from the `w/` namespace essentially evaluate to
;; themselves in the symbolic, raw form - `(w/Plus arguments...)` becomes `'(Plus arguments...)`.

(k/md "### A word on aliases

When we come back to our original expression, `'(+ 1 (Minus 1))`, you may notice that `+` is not actually a Wolfram function.
It is a Wolframite alias, which we replace with `Plus` before we send it to Wolfram.
You can read about it further down this document, in @sec-aliases-table.")

(k/md "### Raw form {#sec-raw-form}

The second form is the _raw (quoted) data form_, using actual Clojure symbols corresponding to Wolfram symbols or Wolframite _aliases_.
This is what Wolframite uses internally.

Notice the `'` quote in front of the expression, telling Clojure not to evaluate it but return it as-is:")

(wl/eval '(+ 1 (Minus 1)))

;; We could also be more explicit and replace `'` with the `quote` that it turns into under the hood:

(wl/eval (quote (+ 1 (Minus 1))))

;; However, quoting the whole form does not allow us to have any evaluations inside the expression.
;; We can instead build the form manually, quoting only the symbols:

(wl/eval (list 'Plus 1 (list 'Minus 1)))

;; Notice that you will leverage quoting also with the evaluated form, namely when you create and refer to Wolfram-side
;; variables, such as those created by `(w/= 'myVar ...)`.

;; ### Advanced: Syntax quote
;;
;; As mentioned, quoting a form makes it impossible to refer to any vars or evaluate any code within the form.
;; We can bypass the problem by not quoting the whole form but replacing `(...)` with `(list ...)` and quoting each symbol.
;; But there is yet another way, used by Clojure macros - namely [the syntax quote](https://clojure.org/reference/reader#syntax-quote) ``` ` ```.
;; It makes it possible to evaluate things within the quoted form by prefixing them with the "unquote" `~` (or "splicing unquote" `~@`).
;; The only problem here is that ``` ` ``` automatically fully qualifies all symbols, as we can see here:

(do `(Minus 42))

;; This would of course break our translation to Wolfram. This expression would be converted to ```wolframite.wolfram`Minus[42]```
;; and Wolfram knows no module called `wolframite.wolfram`.
;; There is fortunately one trick to tell the Clojure Reader to keep a symbol unqualified
;; by combining `~'`, essentially telling it "evaluate this expression, which returns a simple symbol":

(do `(~'Minus 42))

(k/md "### Wolfram string form {#sec-wolfram-string-form}

There is one more form, the _Wolfram string form_, which is the raw Wolfram code in a string:")

(wl/eval "Plus[1,Minus[1]]")

;; This is useful when you just want to paste in some Wolfram of when you need to use a feature that
;; we don't yet support.

;; ### Mixing different kinds of forms
;;
;; The evaluated form may also contain sections in the other forms. You'd typically need that when the Wolframite evaluated form
;; does not (yet) support that which you are trying to do.
;;
;; When nesting a Wolfram string form, we need to
;; explicitly tell Wolframite to treat it as an expression and not just as a primitive string, by passing it through `wl/wolfram-expr`:

(wl/eval (w/Plus
           '(Internal/StringToMReal "-1.5")
           (wl/wolfram-expr "Minus[3]")))

;; ### Aside: Wolframite aliases {#sec-aliases-table}
;;
;; Aside of symbols that directly correspond to Wolfram symbols, you can also use Wolframite aliases. The aliases
;; provide alternative names for Wolfram symbols. We have used above `+`, which is an alias for `Plus`. Here are
;; all the built-in aliases that we currently support:

^:kindly/hide-code
(k/hidden (def recommended-exclusions (set (wl/ns-exclusions))))

(k/table {:column-names [:Alias :Wolfram "Can be used without `w/` prefix?"]
          ,:row-vectors (-> wolframite.runtime.defaults/all-aliases
                            (dissoc '-)
                            (assoc '- "Minus or Subtract")
                            (->> (map (fn [[k v]] [k v (when-not (contains? recommended-exclusions k) "✅")]))))})

(k/md "Thus, the following two expressions are equivalent")

(wl/eval '(+ 1 (- 1)))

(wl/eval '(Plus 1 (Minus 1)))

;; You can even add your own aliases, as discussed in @sec-custom-aliases.

(k/md "### Aside: Wolfram modules and fully qualified names

Most Wolfram functions are global, but they can also be placed inside modules and need to be referred to by their
fully qualified names. While Wolfram uses `` ` `` to separate the module and the symbol, we write it as `/`. Thus, these two are equivalent:")

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
  (wl/eval (Import "demo.csv.gz"
                    ["Data" (Span 1 3)]
                    ,(w/-> "HeaderLines" 1))))

(k/md "## Errors

We try to detect when Wolfram wasn't able to evaluate your expression and throw an exception. However, sometimes
we are not able to detect that. In such cases, the failure will often be indicated by the fact that you get the same, unevaluated expression back.

Here it works as designed:")

(try (wl/eval (FromDigits "-87.6"))
     (catch Exception e
       (k/hiccup [:blockquote (ex-message e)])))

;; Correct:
(wl/eval (FromDigits "87"))

(k/md "## Documentation

We can leverage Clojure repl's documentation support with the Wolfram convenience vars:")

^:kindly/hide-code
(defmacro show-stdout [expr]
  `(kind/hiccup [:pre (let [s# (with-out-str ~expr)]
                        (if (< (count s#) 501)
                          s#
                          (str (subs s# 0 (min 500 (count s#))) "...")))]))

(show-stdout
  (repl/doc GeoGraphics))

;; Search for symbols (case-insensitive):
(->> (repl/apropos #"(?i)geo")
     (drop 2)
     (take 3))

;; Search complete docstrings for a pattern:
(show-stdout
  (repl/find-doc "two-dimensional"))

(k/md "
If we evaluate `(h/help! 'ArithmeticGeometricMean)` then it will open the Wolfram documentation page for `ArithmeticGeometricMean`.

We could instead ask for the link(s):")
(h/help! w/ArithmeticGeometricMean :links true)
#_"TODO(Jakub) the below shall replace the above when https://github.com/scicloj/clay/issues/171 fixed"
#_
(kindly/hide-code
  (k/md (h/help! w/ArithmeticGeometricMean :links true))
  false)

(k/md "`h/help!` also works on whole expressions, providing docs for each symbol:")

(h/help! '(GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"])) :links true)

;; (Notice that help! works both with symbols and our convenience vars.)
