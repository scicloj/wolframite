;; # Gotchas... {#sec-gotchas}
(ns gotchas
  [:require
   [scicloj.kindly.v4.kind :as k]
   [wolframite.api.v1 :as wl]
   [wolframite.wolfram :as w :refer :all
    :exclude [* + - -> / < <= = == > >= fn
              Byte Character Integer Number Short String Thread]]])


(wl/start!)

(k/md "
## What _not_ to do when using Wolfram via Wolframite

### Don't: Transfer huge data unnecessarily
 
 By default, `(wl/! <expr>)` will transfer the return value back to Clojure side and turn it into Clojure data.
 You _don't_ want to do that if the data is big.
 
 **Do**: Keep the data on Wolfram-side, assigning it to a symbol. Example:
")

(def csv 'csv) ; Make the Wolfram-side symbol easier to use in Clojure
(wl/! (w/do  (w/= csv "some really big value, read from a file...")
             nil))

;; We use `w/=` to assign the value to a Wolfram-side name, so that we can use it in subsequent expression.
;; This also returns the value, which we want to ignore, so we wrap it in `w/do` and return something else - length,
;; a small subset, ... .

(k/md "## Language differences between Wolfram and Wolframite to be aware of

Although we try to avoid such things, sometimes, when you're fighting the host language, it's just not practical to carry over the original conventions. Here we will try to keep an up-to-date list of possible surprises, when coming from Wolfram to Wolframite (that are not easy to 'fix').

**^** (`Power`) - Despite allowing a wider range of characters, we cannot use `^` in Wolframite because it is reserved for adding metadata to virtually all Clojure symbols. `(^ x 2)` will probably not do what you expect! Thus we use `(w/** 'x 2)` instead.

**:=** (`SetDelayed`) - Is actually `_=` in Wolframite. This is because `:` is usually a reserved in Clojure for creating keywords. It's worth noting though that this is possibly nicer, in a way, because `_` looks like a placeholder, and defining a placeholder for the expression is what it does. It also sort of implies a delay...

**:>** (`RuleDelayed`) - Is `_>` in Wolframite.

**.** (`Dot`) - Has been changed to `<*>`, in the spirit of the inner product, because `.` is a key character in Clojure, and most object-oriented systems, for namespacing.

**/.** (`ReplaceAll`) -  Similarly, has been changed to `x>>` (and `Replace` to `x>`) for consistency.

**=.** (`Unset`) -  has also been changed to `=!`; be careful not to confuse this with `!=`!

Symbols within threading macros. - After spending so much time playing with symbols, be careful of slipping into things like
`(-> 'x '(Power 1))`
This will not work because `'(Power 1)` is not evaluated, and so will be treated like any other symbol. However, if you use `w/Power` and friends then you do not have this problem.

*Symbols passed to Wolfram must be alphanumeric* - In the end, when they get passed to the Wolfram kernel, symbols must be strictly alphanumeric (apart from forward slashes and dollar signs), *i.e.* `r_2` is currently not allowed. This is due to underlying limitations of the Wolfram language. Much like with Mathematica however, we can get around this in general by using Wolframite's aliasing system (see the relevant tutorials).

*Vectors, `[]`, vs lists, `()`.* - Lists are used to represent function calls and so when combining Clojure and Wolfram expressions, make sure that data literals are vectors. For example, `(wh/show (w/ListLinePlot (range 10)))` will fail (otherwise unexpectedly), but `(wh/show (w/ListLinePlot (into [] (range 10))))` will give you what you expect.
")
