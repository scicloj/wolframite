(ns gotcha
  [:require
   [scicloj.kindly.v4.kind :as k]])

;; TODO: Check bold formatting of symbols

(k/md "# Gotchas...
Although we try to avoid such things, sometimes, when you're fighting the host language, it's just not practical to carry over the original conventions. Here we will try to keep an up-to-date list of surprises that are not easy to 'fix'.

- **^** Despite allowing a wider range of characters, we cannot use '^' in Wolframite because '^' is reserved for adding metadata to virtually all Clojure symbols. (^ x 2) will probably not do what you expect!
- := (SetDelayed) is actually '_=' in Wolframite. This is because ':' is a reserved character in Clojure for creating keywords. It's worth noting though that this is possibly nicer, in a way, because '_' looks like a placeholder, which implies a delay...
- :> (RuleDelayed) similarly, is '_>'
- . (Dot) has been changed to '<*>', in the spirit of the inner product, because '.' is a key character in Clojure and most object-oriented systems for namespacing.
- /. (ReplaceAll) similarly, has been changed to 'x>>' (and 'Replace' to 'x>' for consistency).
- =. (Unset) has also been changed to '=!', be careful not to confuse this with '!='.
- Symbols within threading macros. After spending so much time playing with symbols, be careful of slipping into things like
(-> 'x
    '(Power 1))
This will not work because threads are macros and so combining them with unevaluated functions will lead to unexpected errors.
- Symbols passed to Mathematica must be alphanumeric, i.e. r_2 is not allowed.
- Vectors vs lists. Lists are used to represent functions and so when combining clojure and Wolfram expressions, make sure that data literals are vectors. For example, (wh/view (w/ListLinePlot (range 10))) will fail strangely, but (wh/view (w/ListLinePlot (into [] (range 10)))) will give you what you expect.
")
