;; # Brief introduction into Wolfram Language for Clojure developers

(ns for-developers.wolfram-for-clojurians
  (:require [scicloj.kindly.v4.kind :as k]
            [wolframite.core :as wl]
            [wolframite.wolfram :as w]))

(wl/start)

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

In Wolfram, everything is global by default and you need to take care to avoid that, when necessary.

## Building blocks
")

; #### [Symbolic expressions](https://www.wolfram.com/language/fast-introduction-for-programmers/en/symbolic-expressions/)
; Expressions have the generic form `head[arguments...]`, which becomes Wolframite `(head arguments...)`
;
; Ex.: `Plus[Power[x, 2], Times[3, Power[y, 3]]]`. Notice that we can use undefined symbols, since this
; is just a symbolic expression, not (yet) a computation. An expression's _head_ identifies the type of data or operation being represented.

; #### [Lists](https://www.wolfram.com/language/fast-introduction-for-programmers/en/lists/)
; Wolfram's list `{1, "hello", 3.14}` becomes a vector in Wolframite: `[1, "hello", 3.14]`
;
; List access by indexing (from 1) via `[[idx or a range a.k.a. Span]]`:
(wl/->clj "{1,2,3}[[1 ;; 2]]")
; →
(wl/eval (w/Part [1 2 3] (w/Span 1 2)))

; Many operations "thread" over lists, applying to each element:
(wl/eval (w/Plus [1 2 3] 10))

; #### [Iterators](https://www.wolfram.com/language/fast-introduction-for-programmers/en/iterators/) (to simplify repetitive operations)
; `Table[x^2, {x, 4, 20, 2}]` in Wolfram is equivalent to Clojure's `(map (fn [x] (math/pow x 2)) (range 4 20 2))`

; #### Rules
; Rules, or rewrite rules, of the form `key -> value` predate associations and are used where you'd expect a map,
; often to define [options to functions](https://www.wolfram.com/language/fast-introduction-for-programmers/en/options/),
; as in here: `Import["demo.csv.gz", {"Data", 1}]) ;; 3}, "HeaderLines" -> 1]`
; (Think of this as saying "when evaluating the operation, replace HeaderLines with a truthy value.)

; #### [Associations](https://www.wolfram.com/language/fast-introduction-for-programmers/en/associations/)
; Similar to Maps, just with a unique syntax using Rules. Fortunately, in Clojure we can just use maps:
(wl/->clj "<|\"a\" -> x, \"b\" -> y|>")

; #### Patterns
; are used to transform symbolic expressions into other symbolic expressions, f.ex.
(wl/eval "Replace[f[100], f[x_] -> x + 5]")
;  Here, `_` a.k.a. Blank is a pattern that matches any expression and a double blank `__` matches any sequence of expressions.
; We can name the captured value by prepending a name, as in `x_`.
; There is also `|` for alternatives, `_h` to capture expressions with the head `h`, `:>` for a delayed rules.

; #### [Real-World Entities](https://www.wolfram.com/language/fast-introduction-for-programmers/en/real-world-entities/)
; Real-world entities are symbolic expressions representing information about entities such as countries, chemicals etc. Ex.:
; Use _entity_["Properties"] to find a list of properties and `EntityValue[entity, "Population"]`
(def LA (w/Entity "City" ["LosAngeles" "California" "UnitedStates"]))
(wl/eval (w/EntityValue LA "Population"))
; In Wolframite, we represent these entities as keywords, but currently they are only one-way, from Wolfram to Clojure:
(wl/eval LA)
;; Cannot do: `(wl/eval (w/EntityValue (wl/eval LA) "Population"))`

(wl/eval (w/= 'LAent (w/Entity "City" ["LosAngeles" "California" "UnitedStates"])))
(take 3 (wl/eval (list 'LAent "Properties")))
; Or, in Wolfram: `{ EntityProperty[City, ActiveHomeListingCount], EntityProperty[City, AdministrativeDivision], ... }`
;
; (Note: You may want to override `wolframite.base.parse/custom-parse` for `'Entity` and `'EntityProperty` to modify how these
; get read from Wolfram.

; #### Various
; * [Assignments](https://www.wolfram.com/language/fast-introduction-for-programmers/en/assignments/) - `=` and `:=`; `Module` for scoping
; * [Applying Functions](https://www.wolfram.com/language/fast-introduction-for-programmers/en/applying-functions/) - `Map` with the shorthand `/@`,
;   `Apply` with the shorthand `@@`
; * [Functionals & Operators](https://www.wolfram.com/language/fast-introduction-for-programmers/en/functionals-and-operators/) - Wolfram
; supports currying functions, f.ex. `f[1][2]` is equivalent to `f[1, 2]`
; * Use `;` to separate different side-effecting operations, as `(do ...)` would in Clojure
; * Booleans: `True`, `False`
; * String: "..."


;; Read more in the online booklet [The Wolfram Language: Fast Introduction for Programmers](https://www.wolfram.com/language/fast-introduction-for-programmers/en/),
;; which we have borrowed heavily from.