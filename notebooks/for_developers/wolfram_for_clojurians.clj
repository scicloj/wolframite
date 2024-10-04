;; # Brief introduction into Wolfram Language for Clojure developers {#sec-wolfram-for-clojurians}

(ns for-developers.wolfram-for-clojurians
  (:require [scicloj.kindly.v4.kind :as k]
            [wolframite.core :as wl]
            [wolframite.wolfram :as w :refer :all
             :exclude [* + - -> / < <= = == > >= fn
                       Byte Character Integer Number Short String Thread]]))

(wl/start!)

(k/md "## What is Wolfram?

According to Wikipedia,

> The Wolfram Language is a proprietary, general, very high-level multi-paradigm programming language developed by Wolfram Research.
> It emphasizes symbolic computation, functional programming, and rule-based programming and can employ arbitrary structures and data.
> It is the programming language of the mathematical symbolic computation program Mathematica.

Moreover, the Wolfram Language has the unique position of being not only a programming language but also a full-scale computational language,
that incorporates vast amounts of computable knowledge and lets one broadly express things computationally.

\"Symbolic\" means that everything is a symbolic expression and you can manipulate these expressions themselves - somewhat reminiscent of how you
can transform code with Clojure macros.

## Pitfalls

In Wolfram, everything is global by default and you need to take care to avoid that, when necessary. `w/Block`and `w/Module` may be useful here.
")

(k/md "## Building blocks

#### [Symbolic expressions](https://www.wolfram.com/language/fast-introduction-for-programmers/en/symbolic-expressions/)
")
; Expressions have the generic form `head[arguments...]`, which becomes Wolframite `(head arguments...)`
;
; Ex.: `Plus[Power[x, 2], Times[3, Power[y, 3]]]`. Notice that we can use undefined symbols, since this
; is just a symbolic expression, not (yet) a computation. In Clj:
(wl/->clj "Plus[Power[x, 2], Times[3, Power[y, 3]]]")

; And if we try to evaluate this:
(wl/eval (w/+ (w/** 'x 2) (w/* 3 (w/** 'y 3))))
; we get the same expression back, because x and y aren't defined (yet).
;
; An expression's _head_ identifies the type of data or operation being represented - f.ex. `List` or `Plus`.

(k/md "#### [Functions](https://www.wolfram.com/language/elementary-introduction/2nd-ed/40-defining-your-own-functions.html)

There are multiple ways to create a function.

The canonical way of defining a named function is using _patterns_ (see @sec-patterns): `f[x_] := x^2`, which defines the fn `f`.

To create an ad-hoc function, we can use `Function` similar to Clojure's `fn` or anonymous function literals with _body_`&`, where the body
may use `#, #1, #2, ...` or `(Slot 1), (Slot 1), (Slot 2), ...` equivalent to Clojure's `%, %1, %2, ...`. Ex.: `Map[# + 2&,{1,2,3}]`.

In Wolframite, you'll typically use `w/fn` or leverage the _operator form_ of functions (see @sec-operator-form).
")


(k/md "#### [Lists](https://www.wolfram.com/language/fast-introduction-for-programmers/en/lists/)
")
; A Wolfram List `{1, "hello", 3.14}` becomes a vector in Wolframite: `[1, "hello", 3.14]`.
;
; List access by indexing (from 1) via `[[idx or a range a.k.a. Span]]`. Here we access the first element:
(wl/->clj "{1,2,3}[[1]]")
(wl/eval (Part [1 2 3] 1))

; Here we extract a sublist:
(wl/->clj "{1,2,3}[[1 ;; 2]]")
; →
(wl/eval (Part [1 2 3] (Span 1 2)))

; Many operations "thread" over lists, applying to each element:
(wl/eval (Plus [1 2 3] 10))

(k/md "#### [Iterators](https://www.wolfram.com/language/fast-introduction-for-programmers/en/iterators/) simplify repetitive operations
")
; `Table[x^2, {x, 4, 20, 2}]` in Wolfram is equivalent to Clojure's `(map (fn [x] (math/pow x 2)) (range 4 20 2))`,
; while `Table[x, n]` functions as `(repeat n x)` in Clojure.

; See also the [List Manipulation reference](https://reference.wolfram.com/language/guide/ListManipulation.html).

(k/md "#### [Associations](https://www.wolfram.com/language/fast-introduction-for-programmers/en/associations/)

Similar to Clojure maps, with a unique syntax using Rules (see @sec-rules). Fortunately, in Clojure we can just use maps:")
(wl/->clj "<|\"a\" -> x, \"b\" -> y|>")

(k/md "#### Rules {#sec-rules}
")
; Rules, or rewrite rules, of the form `key -> value` predate associations and are used where you'd have expected a map,
; often to define [options to functions](https://www.wolfram.com/language/fast-introduction-for-programmers/en/options/),
; as in here: `Import["demo.csv.gz", {"Data", 1}]) ;; 3}, "HeaderLines" -> 1]`
; (Think of this as saying "when evaluating the operation, replace HeaderLines with a truthy value.)

(k/md "#### Patterns {#sec-patterns}
")
; are used to transform symbolic expressions into other symbolic expressions. F.ex. here we replace anything that matches
; `f[singleArg]` with the arg + 5:
(wl/eval "Replace[f[100], f[x_] -> x + 5]")
;  Here, `_` a.k.a. Blank is a pattern that matches any expression and a double blank `__` matches any sequence of expressions.
; We can name the captured value by prepending a name, as in `x_`.
; There is also `|` for alternatives, `_h` to capture expressions with the head `h`, `:>` for delayed rules.
;
; Notice that this provides one way to define what we would call functions. `Function` and lambdas are another way.

(k/md "#### [Real-World Entities](https://www.wolfram.com/language/fast-introduction-for-programmers/en/real-world-entities/)
")
; Real-world entities are symbolic expressions representing information about concepts, things etc. such as countries and chemicals.
;
; We can use _entity_["Properties"] to find a list of properties and `EntityValue[entity, "Population"]` to get the value of a property.
;
; We have two ways to represent entities in Wolframite, which both may be useful:
(def LA-expr (Entity "City" ["LosAngeles" "California" "UnitedStates"]))
(wl/eval (EntityValue LA-expr "Population"))
(def LA-evaled (wl/eval LA-expr))
(wl/eval (EntityValue LA-evaled "Population"))

;; To get entity properties, we need a small workaround - Wolfram allows `someEntity["Properties"]` but in our case it would mean
;; trying to use the list `(Entity ...)` as a function, which wouldn't work. So we construct an expression list explicitly:
(take 3 (wl/eval (list (Entity "City" ["LosAngeles" "California" "UnitedStates"])
                       "Properties")))

(k/md "#### Various
")
; * [Assignments](https://www.wolfram.com/language/fast-introduction-for-programmers/en/assignments/) - `=` and `:=`; `Module` for scoping
; * [Applying Functions](https://www.wolfram.com/language/fast-introduction-for-programmers/en/applying-functions/) - `Map` with the shorthand `/@`,
;   `Apply` with the shorthand `@@`
; * Use `;` to separate different side-effecting operations, as `(do ...)` would in Clojure (Wolframite: `w/do`)
; * Booleans: `True`, `False` (Wolframite: `true`, `false`)
; * String: "..."
; * Note: Indices in Wolfram start from 1, not 0

(k/md "##### The \"operator\" form of functions {#sec-operator-form}

Many functions are similar to Clojure transducers such as `map` in the regard that you can invoke them without the data they are
intended to operate on, and they will return a function that can be applied to the data later. This is called the \"operator\" form.
Examples are `Map` and `AllTrue`.

See [Functionals & Operators](https://www.wolfram.com/language/fast-introduction-for-programmers/en/functionals-and-operators/) for more info.")

(k/md "#### Clojure <-> Wolfram
")
^:kindly/hide-code
(k/table {:column-names [:Clojure :Wolfram :Comments],
          :row-vectors [["apply" "Apply"]
                        ["comp" "Composition"]
                        ["count" "Length"]
                        ["filter" "Select"]
                        ["nth" "Part" "1-based indexing"]
                        ["map" "Map"]
                        ["partial" "operator form" "(see above)"]
                        ["reduce" "Fold"]
                        ["take" "Part"]]})

(k/md "#### Additional resources

Read more in the online booklet [The Wolfram Language: Fast Introduction for Programmers](https://www.wolfram.com/language/fast-introduction-for-programmers/en/),
which we have borrowed heavily from.

The dense one-page [Wolfram Language Syntax](https://reference.wolfram.com/language/guide/Syntax.html) may also be of use, especially when reading Wolfram code.
")