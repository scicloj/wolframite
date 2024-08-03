^:kindly/hide-code
(ns clojure-primer.index
  "A very brief introduction to Clojure for a would-be users of Wolframite not familiar with the language"
  (:require [scicloj.kindly.v4.kind :as k]))


(k/md "# Clojure Primer

A very brief introduction to Clojure for a would-be users of Wolframite not familiar with the language

## Elementary syntax

Fortunately, Clojure syntax is very simple and consistent:
")

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-1.svg" :alt "Clojure syntax 1" :width "422" :height "150"}])

(k/md "Clojure is a Lisp, i.e. the code consists of the same data you use as a programmer.

Vectors are similar to JavaScript arrays, keywords to strings (but they’re magical and can be also used as a function that takes a map and looks up its value).

This is the syntax, i.e. how characters become data. Now, on to _semantics_, i.e. what does the data mean:")

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-2.svg" :alt "Clojure syntax 1" :width "586" :height "193"}])

(k/md "A list is _interpreted_ as an invocation (e.g. a function call).

For our purposes here, all invocable things - a function, a macro, a special form - are ± the same thing.")

^:kindly/hide-code
(k/hiccup [:img {:src "./notebooks/clojure_primer/clj-syntax-slide-3.svg" :alt "Clojure syntax 1" :width "640" :height "286"}])

(k/md "Defining a function looks just the same. Highlights:

`>`, `-` are function calls - there are no operators in Clojure. `if` is a \"special form\", i.e. just like a function call for our purposes.

No `else`, no reserved words in Clojure. Everything returns a value.")

(k/md "## Evaluation

Code-as-data, ', quote ....")