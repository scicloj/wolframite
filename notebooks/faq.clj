(ns faq
  "Anticipated frequently asked questions."
  (:require
   [scicloj.kindly.v4.kind :as k]))

(k/md "## FAQ
- Why Clojure?
(need some getting started references)
-- chaining expressions (so much better than in Wolfram)
- Why Wolfram?
thinking (and evaluating) at the level of the expression
- Why not Emmy?
- Why literate programming?
  - clay, clerk
  - why not notebooks?
One might argue that these eval and display conveniences are unnecessary when using a tool like Mathematica, and there is truth in this, but Mathematica is an expensive, proprietary, product with nearly forty years of development. And it suffers

- How do I translate Mathematica's syntax sugar?
   - Since Mathematica hides its internals by default it can sometimes be difficult to work out what functions are being called. If you're in Mathematica, you can use 'FullForm@Unevaluated[...]' to get a more understandable (if less concise) expression. Of course, you can also use Wolframite, i.e. (wl/->) !
")
