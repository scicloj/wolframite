(ns gotchas
  [:require
   [scicloj.kindly.v4.kind :as k]])

(k/md "# Gotchas...
Although we try to avoid such things, sometimes, when you're fighting the host language, it's just not practical to carry over the original conventions. Here we will try to keep an up-to-date list of possible surprises, when coming from Wolfram to Wolframite (that are not easy to 'fix').

**^** (`Power`) - Despite allowing a wider range of characters, we cannot use `^` in Wolframite because it is reserved for adding metadata to virtually all Clojure symbols. `(^ x 2)` will probably not do what you expect! Better to use `(w/Power 'x 2)` or a legal alias.

**:=** (`SetDelayed`) - Is actually `_=` in Wolframite. This is because `:` is usually a reserved in Clojure for creating keywords. It's worth noting though that this is possibly nicer, in a way, because `_` looks like a placeholder, and defining a placeholder for the expression is what it does. It also sort of implies a delay...

**:>** (`RuleDelayed`) - Is `_>` in Wolframite.

**.** (`Dot`) - Has been changed to `<*>`, in the spirit of the inner product, because `.` is a key character in Clojure, and most object-oriented systems, for namespacing.

**/.** (`ReplaceAll`) -  Similarly, has been changed to `x>>` (and `Replace` to `x>`) for consistency.

**=.** (`Unset`) -  has also been changed to `=!`; be careful not to confuse this with `!=`!

Symbols within threading macros. - After spending so much time playing with symbols, be careful of slipping into things like
`(-> 'x '(Power 1))`
This will not work because `'(Power 1)` is not evaluated, and so will be treated like any other symbol.

*Symbols passed to Wolfram must be alphanumeric* - In the end, when they get passed to the Wolfram kernel, symbols must be strictly alphanumeric (apart from forward slashes and dollar signs), *i.e.* `r_2` is currently not allowed. This is due to underlying limitations of the Wolfram language. Much like with Mathematica however, we can get around this in general by using Wolframite's aliasing system (see the relevant tutorials).

*Vectors, `[]`, vs lists, `()`.* - Lists are used to represent function calls and so when combining Clojure and Wolfram expressions, make sure that data literals are vectors. For example, `(wh/view (w/ListLinePlot (range 10)))` will fail (otherwise unexpectedly), but `(wh/view (w/ListLinePlot (into [] (range 10))))` will give you what you expect.
")
