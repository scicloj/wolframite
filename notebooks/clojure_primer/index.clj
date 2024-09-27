^:kindly/hide-code
(ns clojure-primer.index
  "A very brief introduction to Clojure for a would-be users of Wolframite not familiar with the language"
  (:require [scicloj.kindly.v4.kind :as k]
            [clojure.math :as math]
            [wolframite.core :as wl]
            [wolframite.wolfram :as w]))

;; Let's start Wolfram to be ready for our examples underneath:
(wl/start!)

(k/md "# Clojure Primer

A very brief introduction to Clojure for a would-be users of Wolframite not familiar with the language.

## The essence of Clojure

It's impossible to summarize any language in just a few words. But we can say that simplicity and expressiveness
are some of the core values of Clojure. It has been designed to allow you to express your thoughts concisely,
to write programs that avoid some common sources of errors (especially those caused by shared mutable data),
and produce code that is well suited to evolution over time.

## Elementary syntax

Let's start by comparing how **adding a few elements to a list**
looks like in Wolfram, Python, and Clojure:
")

^:kindly/hide-code
(k/table
  [["Wolfram" (k/md "```wolfram
  Join[{}, {\"first\", 2, <|\"name\" -> \"Ava\"|>}]
  ```")]
   ["Python" (k/md "```python
  []+[\"first\",2,{\"name\": \"Ava\"}]
  ```")]
   ["Clojure" '(conj [] "first" 2 {"name" "Ava"})]])

;; We see here a few basic data structures: a `[vector]`, similar to Wolfram/Python lists, and a map `{"key": "value"}`,
;; similar to Python dictionaries / Wolfram associations. One interesting difference is that `,` commas in Clojure are optional.
;;
;; `"Strings"` and numbers `1, 2, ...` are the same. Clojure's `nil` is just like Python's `None`
;;
;; Now, how would we **define a function** that sums two numbers?

^:kindly/hide-code
(k/table
  [["Wolfram" (k/md "```wolfram
  plus[x_, y_] := Plus[x, y]
  ```")]
   ["Python" (k/md "```python
  def plus(x, y):
      return x + y
  ```")]
   ["Clojure" '(defn plus [x y] (+ x y))]])

;; Now we are ready to learn a little more about how to read Clojure code.

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-1.svg" :alt "Clojure syntax 1" :width "422" :height "150"}])

(k/md "Clojure is a Lisp, i.e. the code consists of the same data you use as a programmer: lists, vectors, maps, symbols, etc.

Lists and vectors are both sequential data structures. Vectors support efficient access to any element, while lists are always read from start.

Keywords are similar to strings, but tuned for use as keys in maps. Additionally, a keyword may be used as a function that takes a map and looks up the corresponding value:")

(:name {:name "Ava"})

(map :name [{:name "Ava"} {:name "Boudicca"}])

;; This is the syntax, i.e. how characters become data. Now, on to _semantics_, i.e. what does the data mean:

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-2.svg" :alt "Clojure syntax 1" :width "586" :height "193"}])

(k/md "
A list is _interpreted_ as an invocation (e.g. a function call), where the first element is a symbol that resolves to something invocable
(a function, a special form, or a macro; we don't need to care about their differences here). All the other elements are arguments to the function.

The same code structure is used for everything, including defining functions:")

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-3.svg" :alt "Clojure syntax 1" :width "640" :height "286"}])

(k/md "
The `defn`here defines a new function called `subtract-smaller`, taking two arguments `x` and `y`.

Noteworthy:

`>`, `-` are just function calls. `if` is a \"special form\" and `defn` is a macro, but they behave very similarly to functions.

Notice there is no `else`. There are no reserved/special words in Clojure.
")

(k/md "## Basics

* Everything returns a value (which may be `nil`). F.ex. `if` returns whatever the evaluated branch returned.
* Clojure is [very tolerant of `nil`](https://ericnormand.me/article/nil-punning), _most_ functions treat it as an empty value - you can
  `map` over it (getting `nil` back), append to it (getting back a list with the appended element) etc.
* Whitespace is not significant. Commas are treated as a whitespace, and used occasionally for readability
* Booleans: `true` and `false`. All predicates and conditional expressions treat both `false` and `nil` as `false` and
 everything else as `true` (also called \"truthy\").
* Clojure runs on the Java Virtual Machine (JVM) and you can directly call Java methods. (Learn more about [Clojure ↔ Java interoperability](https://clojure.org/reference/java_interop).)
* Clojure is intended for [_interactive development_](https://clojure.org/guides/repl/introduction),
  also known as REPL-driven development, where you build and evolve your application while it is running.
  You may be used to a very similar this style of development from Python or Wolfram notebooks.
")

(k/md "## Evaluation

Now is a good time to revisit [Understanding Wolframite](@sec-understanding-wolframite) and read about the three forms of Wolframite expressions
(raw, evaluated, and Wolfram string).

A point worth re-iterating is that Wolframite expressions are not evaluated by Clojure but are sent ± as-is to Wolfram for evaluation.
Thus, you may not mix Clojure and Wolframite expressions freely. But there is one exception to that - a Wolframite expression may contain Clojure expressions
that can be evaluated fully on the Clojure side and replaced with the result, before being sent to Wolfram. Let's have a look at a few examples:")

;; Clojure-side only:
(+ (* 3 (math/pow 2 2))) ; 12.0

;; Wolfram-only:
(wl/eval (w/+ (w/* 3 (w/Power 2 2))))

;; Mixed, with Clojure evaluated before sending the form to Wolfram:
(wl/eval (w/+ (w/* 3 (w/Power (+ 1 1) 2))))

;; This is how the expression is evaluated before we resolve aliases and turn it into Wolfram and send it over to the kernel:

(w/+ (w/* 3 (w/Power (+ 1 1) 2)))

;; Notice that you may nest Clojure-only expression, which does not depend on the surrounding Wolfram context, inside a Wolframite expression,
;; but you cannot do the opposite, i.e. nest a Wolframite expression inside a Clojure expression:
(try
 (wl/eval (+ (w/* 3 (w/Power (+ 1 1) 2))))
 (catch Exception e
   (str e)))

;; This fail because we are passing a Wolframite expression (a list) to the Clojure `+` function, but it only works with numbers. We'd need to evaluate the expression first:

(+ (wl/eval (w/* 3 (w/Power (+ 1 1) 2))))

(k/md "## Resources for further learning

* The [Clojure cheatsheat](http://jafingerhut.github.io/cheatsheet/clojuredocs/cheatsheet-tiptip-cdocs-summary.html)\nis a good place to look for functions.
* [The 100 Most Used Clojure Expressions](https://ericnormand.me/article/100-most-used-clojure-expressions)")