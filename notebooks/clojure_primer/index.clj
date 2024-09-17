^:kindly/hide-code
(ns clojure-primer.index
  "A very brief introduction to Clojure for a would-be users of Wolframite not familiar with the language"
  (:require [scicloj.kindly.v4.kind :as k]
            [clojure.math :as math]
            [wolframite.core :as wl]
            [wolframite.wolfram :as w]))

;; Let's start Wolfram to be ready for our examples underneath:
(wl/start)

(k/md "# Clojure Primer

A very brief introduction to Clojure for a would-be users of Wolframite not familiar with the language

## The essence of Clojure

It's impossible to summarize any language in just a few words. But we can say that simplicity and expressiveness
are some of the core values of Clojure. It has been designed to allow you to express your thoughts concisely,
to write programs that avoid some common sources of errors (especially those caused by shared mutable data),
and produce code that is well suited to evolve over time.

## Elementary syntax

Let's start by comparing how **adding a few elements to a list**
looks like in Wolfram, Python, and Clojure:
")

^:kindly/hide-code
(k/table
  [["Wolfram" (k/md "```wolfram
  TODO
  ```")]
   ["Python" (k/md "```python
  TODO
  ```")]
   ["Clojure" '(conj [] "first" 2 {"name" "Ava"})]])

;; We see here a few basic data structures: a `[vector]`, similar to Wolfram/Python lists, and a map `{"key": "value"}`,
;; similar to Python dictionaries / Wolfram associations. One interesting difference is that `,` commas in Clojure are optional.
;;
;; `"Strings"` and numbers `1, 2, ...` are the same.
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

Lists and vectors are both sequential data structures, but vectors support efficient access of any element, while lists are always read from start.

Keywords are similar to strings, but tuned for use as keys in maps. (And a keyword may be used as a function that takes a map and looks up the corresponding value).

This is the syntax, i.e. how characters become data. Now, on to _semantics_, i.e. what does the data mean:")

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-2.svg" :alt "Clojure syntax 1" :width "586" :height "193"}])

(k/md "A list is _interpreted_ as an invocation (e.g. a function call).

For our purposes here, all invocable things - functions, macros, and special forms - are ± the same thing.

The same code structure is used for everything, including defining functions:")

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-3.svg" :alt "Clojure syntax 1" :width "640" :height "286"}])

(k/md "Highlights:

`>`, `-` are just function calls. `if` is a \"special form\", i.e. just like a function call for our purposes.

No `else`, no reserved words in Clojure. Everything returns a value.")

(k/md "## Evaluation

Code-as-data, ', quote ....")

;; **TO BE CONTINUED**

(+ (* 3 (math/pow 2 2))) ; 12.0
(wl/eval (w/+ (w/* 3 (w/Power 2 2))))