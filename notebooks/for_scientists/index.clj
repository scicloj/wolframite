(ns for-scientists.index
  "An introduction to the library, that might even be suitable for physicists."
  (:require
   [scicloj.kindly.v4.kind :as k]
   [wolframite.core :as wl]
   [wolframite.impl.wolfram-syms.write-ns :as write]
   [wolframite.tools.hiccup :as wh]
   [wolframite.wolfram :as w]))

^:kindly/hide-code
(comment
  ;; ## TODO: ##
  ;; - check that displaying tex source by default doesn't cause any problems
  ;; - Move general notes and examples out from the Cavity section
  ;; - check that the gotchas are viewable
  ;; - check that the packages namespace is visible
  ;; - how to access the docs
  ;; - should probably be split into two (the cavity part can be quite self-contained)
  ;; - how do/can I reference sections within a clay document?
  )

(k/md "# Wolframite for scientists")
(k/md "## Abstract
We introduce you, the motivated scientist (likely the mathematical sort), to using the Wolfram programming language as a Clojure library. Following some brief inspiration (why on earth should you do this?), and getting started notes, we then outline a 'real' workflow using the example of optical cavities.")
(k/md "## Motivation - Why on earth?
The Wolfram programming language is 'best-in-class' for certain specialist tasks, like manipulating equations, but, as Randall Munroe might have said,
> Wolfram combines the flexibility and power of abstract mathematics with the intuitive clarity of abstract mathematics.

Clojure, on the other-hand, is a first-class, general-purpose, programming language whose core namespace and dynamical, functional, paradigm is well suited to data exploration and manipulation. So why don't we call Wolfram from Clojure? It seems like the atypical 'best of both worlds'. ")

(k/md "## Getting Started (for scientists)
This tutorial aims to be as comprehensive a reference as possible for the average scientist and, as such, is not concise (and will probably only get longer). For a quick-fire introduction, see the 'demo' page. If the demo is too short and this is too long however, you can always run a 'search' on the text for the relevant examples. We won't judge you!

### Clojure

There are many useful online tools for getting started with Clojure. For an overview of the language itself, have a look at [Eric Normand's materials](https://ericnormand.me/). If you'd like to jump straight-in with interactive examples then try the [koans](http://clojurescriptkoans.com/).

#### Clojure physics

If you'd like to see why a physicist might be interested in Clojure then have a look at the video below.
")
(k/video {:youtube-id  "SE5Ge4QP4oY"})

(k/md "#### Clojure notation

For those who are still fairly new to clojure, the first thing to get used to is the style of notation. It might seem strange at first, but a functional LisP can be more efficient (symbolically) than a lot of object-oriented languages and even standard mathematical notation. For example, why write 1+1+1+2 and not (+ 1 1 1 2)? They have around the same number of characters and yet even here it's arguable that the signal to noise ratio is higher for the second one. Where the second form really shines however, is in it's scalability. As soon as you add another operator, e.g. 1+1+1+2/3, we have a problem. Okay so you made it through primary school and know that 1+1+1+2/3 is really 1+1+1+(2/3) and not (1+1+1+2)/3, but the mental complexity is still there, you're just used to it. If we introduce another operator, e.g. 1+1+1+2✝3, now what do you do? The truth is that if we stick to one simple rule, i.e. use brackets, then we completely solve a whole range of problems in advance, with the 'cost' of having to write two characters. In fact, although it might not be obvious, the Wolfram language is actually inspired by/built on the LisP syntax (underneath). The outer layer is just to make it look more like the inefficient notation that mathematicians already know and love...

In summary, BODMAS is six rules and incomplete. (function argument ...) is a single rule and complete. Be kind to yourself, just write (+ 1 1 1 (/ 2 3)) :).

 If you're still unconvinced about the benefits of a simple syntax then remember that LisP is a programmable programming language: and its the syntax that delivers [macros](http://lists.warhead.org.uk/pipermail/iwe/2005-July/000130.html).


### Data science
If you're sold on Clojure and interested in problems close to data science then an overview of some of the key analysis libraries can be found here.")
(k/video {:youtube-id "MguatDl5u2Q"})

(k/md "If you're really ready to use Wolfram though, then read on!

## Wolfram basics")
(k/md "### Let's define our terms!
 One of the really nice things about using Wolfram as a library is that our middle man (Wolframite) can provide default substitutions to  simplify verbose terms, e.g. '(Power x 2) can become '(** x 2) or even '(**2 x), for readability. What's even nicer however, is that we can make our own aliases at runtime: the choice is ours!

We mention this now because the best time to define our terms is before we start. The first few aliases give us an insight into how I would have designed Wolfram. The last two go a step further. Here, we make use of Clojure's modernity and decision to support a much wider character set than Wolfram. Depending on your editor, it can be just as easy to enter these unicode characters as typing the long function name, but the real win is in the readability. Remember, we read our documents far more often than we write them (even if it's just peer review...).

Therefore, we enter the following.
")

(def aliases
  '{** Power
    ++ Conjugate

    x> Replace
    x>> ReplaceAll
    <_> Expand
    <<_>> ExpandAll
    ++<_> ComplexExpand
    >_< Simplify
    >>_<< FullSimplify

    ⮾ NonCommutativeMultiply
    √ Sqrt
    ∫ Integrate})

(wl/restart {:aliases aliases})

(k/md "We can define new aliases by simply using the core clojure functions/macros. This works fine, but these are only symbol replacements and so we can't benefit from using these entities in macros or from editor autocompletion. For the best experience therefore, we recommend using 'write-ns!'.")
(comment
  (write/write-ns! "src/wolframite/wolfram.clj"
                   {:aliases aliases}))

(k/md "Using this command, we can generate a full namespace at the given location and so refer to the new symbol aliases more directly. The command appears here in a comment block to avoid unnecessary recomputation. See the 'Literate programming' section for an explanation.")

(k/md "### Access to knowledge
With the recent emergence of ChatGPT and similar AI systems, direct access to knowledge from free-form linguistic input is in demand. Wolfram was an early pioneer of this, launching its product, Wolfram|Alpha, in 2009. Wolfram occupies a different space however, focusing mainly on what it describes as 'computational knowledge'. Rather than strictly using an LLM, it combines its knowledge databases with the ability to perform Wolfram operations on the retrieved data, before returning the result.

For example, we can make both knowledge-based requests and perform complicated calculations on demand (requires internet access).")

(wh/view (w/WolframAlpha "What is the mass of 5 rubidium atoms?" "Result"))
(wh/view (w/WolframAlpha "What is the relativistic momentum of a 0.8c electron?" "Result"))

(k/md "In each case, Wolfram does not just provide the answer, but returns the data in different formats and units in an attempt to anticipate different usecases. This verbosity might seem frustrating, but of course adds flexibility: leaving the user free to filter or present the results in any way that they wish. Here, we have used the last argument to simply display the core 'Result'.")

(k/md "### Defining expressions
Rather than just querying the Wolfram servers however, the vast majority of people who use this library will want to write their own programs.

For those coming from Mathematica (the official, graphical front-end for the Wolfram language), a good place to start is defining expressions.

This is where the main cost of 'Wolfram as a library' lies. In a Wolfram environment, all non-language characters are treated as global symbols by default. This works well for symbol and expression manipulation (Mathematica's assumed raison d'être), but not for any other purpose. To successfully embed Wolfram expressions into a general purpose programming language we must make choices.

In this library, there are two approaches. For all official functions, the cleanest way of referring to them is to import the base symbol namespace, i.e. wolframite.wolfram, or a customized one, e.g. 'wolframite.wolfram-extended'. This allows the user to manipulate expressions like other clojure functions (and to access the associated Wolfram documentation from your editor) e.g.
")
(-> 2
    (w/Power 1)
    (w/Subtract 2))

(k/md "To calculate/retrieve the result we then simply add 'eval' from the Wolframite core namespace:")
(-> 2
    (w/Power 1)
    (w/Subtract 2)
    wl/eval)
(k/md "Note, separating expression chains from automatic eval was done for efficiency. Previously, all Wolfram expressions were individually evaluated with separate kernel calls and this was considered a performance problem.


To deal with general symbols, we return to one of LisPs' strengths: controlled evaluation. Historically necessitated by LisPs' 'code-as-data' paradigm, all LisPs can deal with general symbols by simply not evaluating them. This makes it easy to create and manipulate arbitrary Wolfram expressions, as we can simply treat them as unevaluated symbols (note our use of the new aliases too). ")
(-> 'x
    (w/** 1)
    (w/- '5)
    wl/eval)

(k/md "This comes at the cost of having to 'mark' symbols, lists and functions manually, but it's a choice between being unevaluated by default (e.g. Wolfram, Maple), unevaluated when marked (LisPs) or no easy way to work with symbols (the vast majority of programming languages). It also comes at the cost of some 'gotchas' (listed elsewhere in the docs), but these are avoided for the most part by using a wolframite.wolfram namespace (and 'write-ns!' function), as introduced in the beginning.")

(k/md "### Defining functions
Functions are slightly more complicated. Functions can be defined in many different ways. The easiest way is to keep functions, where appropriate, as standard Clojure expressions.

If you're used to using Wolfram/Mathematica, then f[x_]:=x^2 is simply
")
(defn f [x] (wl/eval (w/Power x 2)))
(f 2)
(f 'x)

(k/md ". If instead you want to define functions inside the Wolfram kernel, and attach them to arbitrary symbols, then the most ergonomic way is")
(wl/eval (w/_= 'f (w/fn [x] (w/** x 2))))
(wl/eval '(f 5))
(k/md ", where '_=' is the Wolframite version of ':=' (SetDelayed). See the 'Gotchas' section for why. Rather than a direct alias, w/fn is a special form that allows you to define Wolfram functions in a convenient way. Note that 'f' is a new symbol and therefore cannot be expected to be part of wolframite.wolfram or similar namespaces. Therefore, we must call the function using an unevaluated Clojure list.

In my opinion, this mixes the best of Wolfram and Clojure: and scales well. The most explicitly Wolfram way of doing it however, is to write")
(wl/eval (w/Clear 'f))
(wl/eval '(_= (f (Pattern x (Blank))) (Power x 2)))
(wl/eval '(f 5))
(k/md ", where we first removed all definitions from 'f' before reassigning the function.


Once you can define your own aliases and create arbitrary expressions and functions, then you're basically good to go.")

(k/md "## Mixing functions

Wolframite is not just a different way to typeset Wolfram functions. Indeed, there would be very little point in building an entire library around this. Instead, Wolframite wants to help the user bring Wolfram's functions into a general Clojure workflow, i.e. to mix Clojure and Wolfram together, such that there is another, potentially better way, to do science.

Such is the case, we can demonstrate a new workflow. As noted above, it is straightforward to define functions in Wolframite, as in Wolfram/Mathematica.
")

(defn ||2
  "The intensity: the value times the conjugate of the value or, equivalently, the absolute value squared."
  [x]
  (w/* x (w/++ x)))

(-> (w/* 3 w/I)
    (w/+ 5)
    ||2
    wl/eval)

(k/md "Here we make a shorthand, the absolute value squared. Defined as a clojure function, it simply abstracts two Wolfram operations, which can then be used alongside others.

And yet there are there are two meaningful improvements already. First of all, we can, [currently](https://ask.clojure.org/index.php/11627/the-pipe-char-considered-valid-symbol-constituent-character), use more mathematical characters in Clojure, such that even the raw code can approach familiar symbolic maths.
Second of all, we can exploit Clojure's 'threading' features. In my view, chaining Wolfram function calls together with threading macros is actually a big usability improvement. Wolfram expressions can get pretty involved (it's common to end up with expressions that hold 10s of symbols and operators and 100s are not unheard of) and trying to read these from the inside out is just not natural for the average human. It stands to reason then that chaining functions together (and debugging them!) can really be a pain. In fact, Wolfram recognised this problem when it introduced the prefix operator, '@', to help with function composition, e.g. f@g@h. Unfortunately however, this doesn't work with multiple arguments. It is possible to do things like f@@args, and even things like f@@@{{a, b}, {c, d}}, but the readability quickly becomes dire. On the other hand, Clojure's threading is simple, clear and scalable.

In fact, a little Clojure goes a long way. Look how easily we can add substantial UX conveniences to our workflow.")

(defmacro eval->
  "Extends the threading macro to automatically pass the result to wolframite eval."
  [& xs]
  `(-> ~@xs wl/eval))

(defn TeX
  "UX fix. Passes the Wolfram expression to ToString[TeXForm[...]], as the unsuspecting coder might not realise that 'ToString' is necessary."
  [tex-form]
  (w/ToString (w/TeXForm tex-form)))

(defmacro TeX->
  "Extends the thread-first macro to automatically eval and prepare the expression for TeX display."
  [& xs]
  `(-> ~@xs TeX wl/eval k/tex))

(k/md "Now we can implicitly chain operations together, evaluate the result and get a nice TeX display in the browser.")
(TeX-> (w/* 'x w/I)
       (w/+ 'y)
       ||2)

(k/md "It's just a small example, but LisP is designed for metaprogramming and so the sky's the limit when it comes to building more features around Wolfram functions. A sceptical reader may point out that eval and display are solved problems within the Mathematica system, but in my opinion there are solid reasons for not wanting to be confined to a specific IDE. Have a look at 'Why literate programming?' in the FAQs for more details.

Before we finish this part of the tutorial, let's consider a (slightly) more realistic example.

Wolfram can be used to define quite general approximations, using the 'Pattern' system. For example, let us assume that we want to expand Cos(x) as a polynomial when x is small. First, we need to know what the expansion is. We can actually find out by using Wolfram! i.e. ")

(eval-> (w/Series (w/Cos 'x)
                  ['x 0 2])
        w/Normal)

(k/md "Now that we know (or have remembered!) the form, we can create a general rule that is not limited to specific symbol definitions:")

(def small-angle
  (w/_> (w/Cos (w/Pattern 'x (w/Blank)))
        (w/+ 1 (w/* -1/2 (w/** 'x 2)))))

(k/md ", where we have made use of Clojure's native support for ratios. The rule can now be named and used for any Cos function with any argument. Which of course looks much nicer using TeX.")

(TeX-> (w/x>> (w/Cos 'phi)
              small-angle))
(TeX-> (w/x>> (w/Cos (w/+ 1 'phi))
              small-angle))

(k/md "# What's next?

Okay, so that was a lot of introduction. If you read all of that in one sitting then remember to take a break. Put the kettle on; do some press-ups and then sit-down for a worked example (Cavity Physics).
")

(k/md "# References
This tutorial was made by Thomas Clark, Jakub Holý and Daniel Slutsky. The cavity field derivation followed in the footsteps of one Francis 'Tito' Williams.")

^:kindly/hide-code
(comment
  (require '[scicloj.clay.v2.api :as clay])
  (clay/browse!)
  ((requiring-resolve 'scicloj.clay.v2.api/make!) {:source-path "notebooks/for_scientists/index.clj"
                                                   :format [#_:quarto :html]}))
