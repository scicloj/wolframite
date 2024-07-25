^{:clay {:kindly/options {:kinds-that-hide-code #{:kind/md :kind/hiccup :kind/html :kind/tex}}}}
(ns for-scientists.cavity-physics
  "An example namespace of how you might present some physics."
  (:require
   [scicloj.kindly.v4.kind :as k]
   [wolframite.impl.wolfram-syms.write-ns :as write]
   [wolframite.core :as wl]
   [wolframite.wolfram-extended :as w]))

;; TODO:
;; - contents page
;; - automatically hide nils
;; - how do/can I reference sections within a clay document?
;; - credit authors
;; - are clay kinds executed outside of a clay environment, i.e. are they treated like 'comment's under 'normal' circumstances?
;; - Move general notes and examples out from the Cavity section
;; - should probably be split into two (the cavity part can be quite self-contained)

(k/html "<span style='color:red'><b>SPOILER WARNING</b>: This is a work in progress...</span>
")
(k/md "# Wolframite for scientists")
(k/md "## Abstract
We introduce you, the motivated scientist (likely the mathematical sort), to using the Wolfram programming language as a Clojure library. Following some brief inspiration (why on earth should you do this?), and getting started notes, we then outline a 'real' workflow using the example of optical cavities.")
(k/md "## Motivation - Why on earth?
The Wolfram programming language is 'best-in-class' for certain specialist tasks, like manipulating equations, but would you really want to use a language with 6000 functions in a single global namespace to do a normal project, like building a website? Clojure, on the other-hand, is a first-class, general-purpose, programming language whose core namespace and dynamical, functional, paradigm is well suited to data exploration and manipulation. So why don't we call Wolfram from Clojure? It seems like the atypical best of both worlds. ")

(k/md "## Getting Started (for scientists)
This tutorial aims to be as comprehensive as possible for the average user and, as such, is not concise. There are other quick-fire instructions available at (REF). If you still insist on working through this document, you can always search through the text for the relevant advice or examples. We won't judge you!

### Clojure
There are many useful online tools for getting started with Clojure. For an overview of the language itself, see REF. If you'd like to try it out in practice then jump in with the [koans](http://clojurescriptkoans.com/). If you'd like to see why a physicist might be interested in Clojure then have a look [here](https://www.youtube.com/watch?v=SE5Ge4QP4oY).

#### Notation
For those who are still fairly new to clojure, the first thing to get used to is the style of notation. It might seem strange at first, but a functional LisP can be more efficient (symbolically) than a lot of object-oriented languages and even standard mathematical notation. For example, why write 1+1+1+2 and not (+ 1 1 1 2)? Theoretically, they have the same number of characters and yet even here it's arguable that the signal to noise ratio is higher for the second one. Where the second form really shines however, is in it's scalability. As soon as you add another operator, e.g. 1+1+1+2/3, we have a problem. Okay so you made it through primary school and know that 1+1+1+2/3 is really 1+1+1+(2/3) and not (1+1+1+2)/3, but the mental complexity is still there, you're just used to it. If we introduce another operator, e.g. 1+1+1+2✝3, now what do you do? The truth is that if we stick to one simple rule, i.e. use brackets, then we completely solve a whole range of problems in advance, with the 'cost' of having to write two characters. In fact, although it might not be obvious, the Wolfram language is actually inspired by/built on the LisP syntax (underneath). The outer layer is just to make it look more like the inefficient notation that mathematicians already know and love...

In summary, BODMAS is six rules and incomplete. (function argument ...) is a single rule and complete. Be kind to yourself, just write (+ 1 1 1 (/ 2 3)) :). If you're still unconvinced about the benefits of a simple syntax then consider how easy it is in LisP to generate/generalize your source code with [macros](http://lists.warhead.org.uk/pipermail/iwe/2005-July/000130.html).


### Data science
If you're sold on Clojure and interested in problems close to data science then an overview of some of the key analysis libraries can be found here (REF). If you're ready to use Wolfram, then read on!")

(k/md "## Wolfram basics")
(k/md "### Let's define our terms!
 One of the really nice things about using Wolfram as a library is that our middle man can simplify verbose terms like (w/Power 'x 2) to (w/**2 'x), for readability. What's even nicer however, is that we can make our own symbolic shortcuts: the setup is your own!

We mention this here because the best time to define your terms is at the beginning. We define some aliases below, where the first few give you an insight into how I'd have changed some of the wolfram symbols for symbolic expressiveness and ease of use. The last two go a step furter. Here, we make use of Clojure's modernity and all round good decision to support the full unicode character set. Depending on your editor, it can be just as easy to enter these unicode characters as typing the long function name, but the real win is in the readability. Remember, we read our documents far more often than we write them (even if it's just peer review...).

Therefore, we enter the following.
")

(def aliases
  '{** Power
    ++ Conjugate
    .> Replace
    .>> ReplaceAll
    <-> Expand
    <<->> ExpandAll
    ++<-> ComplexExpand
    >< Simplify
    >><< FullSimplify
    ⮾ NonCommutativeMultiply
    √ Sqrt
    ∫ Integrate})

(wl/restart {:aliases aliases})

(k/md "Here you see than we can define new aliases by simply using the core clojure functions/macros. This works fine, but the downside is that we have just defined symbol replacements and so won't benefit from using these entities in macros or from editor autocompletion. For the best experience therefore, we recommend using 'write-ns!'.")
(comment
  (write/write-ns! "src/wolframite/wolfram_extended.clj"
                   {:aliases aliases}))

(k/md "By using this command, we can automatically create a namespace at the given location and so refer to the new symbol aliases more directly. The command appears here in a comment block to avoid unnecessary recomputation. See the 'Literate programming' section for an explanation.")

(k/html "<span style='color:red'>The next section should probably be moved later.</span>")
(k/md "### Access to knowledge
With the recent emergence of ChatGPT and similar AI systems, direct access to knowledge from free-form linguistic input is in demand. Wolfram was an early pioneer of this, launching its product, Wolfram|Alpha, in 2009. Wolfram occupies a different space however, focusing mainly on what it describes as 'computational knowledge'. Rather than strictly using an LLM, it combines its knowledge databases with the ability to perform Wolfram operations on the retrieved data before returning the result.

For example, we can make both knowledge-based requests and perform complicated calculations (requires internet access).")

(wl/eval (w/WolframAlpha "What is the mass of 5 rubidium atoms?"))
(wl/eval (w/WolframAlpha "What is the relativistic momentum of a 0.8c electron?"))

(k/md "In each case, Wolfram does not just provide the answer, but returns the data in different formats and units in an attempt to anticipate different usecases. This verbosity might seem frustrating, but of course adds flexibility: leaving the user free to filter or present the results in any way that they wish.")

(k/md "### Defining expressions
Rather than just querying the Wolfram servers however, the vast majority of people who use this library will want to write their own programs.

For those coming from Mathematica (the official, graphical front-end for the Wolfram language), a good place to start is defining expressions.

This is where the main cost of 'Wolfram as a library' lies. In a Wolfram environment, then all non-language characters are treated as global symbols by default. This works well for symbol and expression manipulation (Mathematica's assumed raison d'être), but not for any other purpose. To successfully embed Wolfram expressions into a general purpose programming language we must make choices.

In this library, there are two approaches. For all official functions, the cleanest way of referring to them is to import the base symbol namespace, i.e. wolframite.wolfram or, as in this example, a customized one: wolframite.wolfram-extended. This allows the user to manipulate expressions like other clojure functions (and to access the associated Wolfram documentation from your editor) e.g.
")
(-> 2
    (w/Power 1)
    (w/Subtract 2))

(k/md "To calculate/retrieve the result we then simply add 'eval' from the Wolframite core namespace:")
(-> 2
    (w/Power 1)
    (w/Subtract 2)
    wl/eval)
(k/md "Note, separating expression chains from automatic eval was done for efficiency. Previously, all Wolfram expressions were individually evaluated and this was considered a performance problem.


To deal with general symbols, we return to one of LisPs' strengths: controlled evaluation. Historically necessitated by LisPs' 'code-as-data' paradigm, all LisPs can deal with general symbols by simply not evaluating them. This makes it easy to create and manipulate arbitrary Wolfram expressions, as we can simply treat them as unevaluated symbols.")
(-> 'x
    (w/** 1)
    (w/- '5)
    wl/eval)

(k/md "This comes at the cost of having to 'mark' symbols, lists and functions manually, but it's a choice between being unevaluated by default (e.g. Wolfram, Maple), unevaluated when marked (LisPs) or no easy to work with symbols (the vast majority of programming languages). It also comes at the cost of some 'gotchas' (see below), but these are avoided for the most part by using a wolframite.wolfram namespace (and 'write-ns!' function), as introduced in the beginning.")

(k/md "### Defining functions
Functions are slightly more complicated. Functions can be defined in many different ways. The easiest way is to keep functions, where appropritae, as standard Clojure expressions.

If you're used to using Wolfram/Mathematica, then
f[x_]:=x^2 becomes
")
(defn square [x] (wl/eval (w/Power x 2)))
(square 2)
(square 'x)

(k/md "If you want to define functions inside the Wolfram kernel and attach them to arbitrary symbols, then you can write")
(wl/eval (w/..= 'f (w/fn [x] (w/Power x 2))))
(wl/eval '(f 5))

(k/md "Rather than a direct alias, w/fn is a special form that allows you to define Wolfram functions in a convenient way. Note that 'f' is a new symbol and therefore cannot be expected to be part of wolframite.wolfram or similar namespaces. Therefore, we must call the function using an unevaluated Clojure list.")

(k/md "
Once you can define your own aliases and create arbitrary expressions and functions, then you're basically good to go. You might have noticed however, that the above function assignment used an unfamiliar symbol...

### Gotchas...
This brings us to some of the 'gotchas' in this library. Although we try to avoid such things, sometimes, when you're fighting the host language, it's just not practical. Here we will try to keep an up-to-date list of surprises that are not easy to 'fix'.

- := (SetDelayed) is actually '..=' in Wolframite. This is because ':' is a reserved character in Clojure for creating keywords. It's worth noting though that this is actually nicer, in a way, because '..' looks like an ellipsis, which implies a delay!
- :> (RuleDelayed) similarly, is '..>'
- Symbols within threading macros. After spending so much time playing with symbols, be careful of slipping into things like
(-> 'x
    '(Power 1))
This will not work because threads are macros and so combining them with unevaluated functions will lead to unexpected errors.
- Symbols passed to Mathematica must be alphanumeric, i.e. r_2 is not allowed.
")

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
- How do I translate Mathematica's syntax sugar?
   - Since Mathematica hides its internals by default it can sometimes be difficult to work out what functions are being called. If you're in Mathematica, you can use 'FullForm@Unevaluated[...]' to get a more understandable (if less concise) expression. Of course, you can also use Wolframite, i.e. (wl/->) !
")

(k/md "## Function shortcuts
The next step up from making our symbols more readable, is to make our code more readable: let's see how some UX conveniences are built.
")

(defn ||2
  "The intensity: the value times the conjugate of the value or, equivalently, the absolute value squared."
  [x]
  (w/* x (w/++ x)))

(wl/eval (||2 (w/+ 5 (w/* 3 w/I))))

(defmacro eval->
  "Extends the threading macro to automatically pass the result to wolframite eval.

  TODO: Should we automatically quote everything inside this?"
  [& xs]
  `(-> ~@xs wl/eval))

(defmacro eval->>
  "Extends the threading macro to automatically pass the result to wolframite eval.

  TODO: Should we automatically quote everything inside this?"
  [& xs]
  `(->> ~@xs wl/eval))

(defn TeX
  " UX fix. Passes the Wolfram expression to ToString[TeXForm[...]] as the unsuspecting traveller might not realise that 'ToString' is necessary.
  "
  [tex-form]
  (w/ToString (w/TeXForm tex-form)))

(defmacro TeX->
  "Extends the threading macro to automatically pass the result to wolframite eval."
  [& xs]
  `(-> ~@xs TeX wl/eval k/tex))

(defmacro TeX->>
  "Extends the threading macro to automatically pass the result to wolframite eval.
  "
  [& xs]
  `(->> ~@xs TeX wl/eval k/tex))

(k/md "Now we can implicitly chain operations together and still get a nice result in the browser.")
(TeX-> 'x ||2 (w/+ 'y))

(k/md "
# Cavity Maths (A physics example)
Okay, so that was a lot of introduction. If you read all of that in one sitting then remember to take a break. Put the kettle on; do some press-ups and then sit-down for a worked example.

## Cavities?
Outside of the operating room, the most common notion of cavities is, what we might otherwise call, an optical resonator. One way of looking at it, although quantum mechanics makes this more difficult, is that an optical cavity is simply just a light trap, such that once light gets in, then it bounces around for a while before it leaves.

For demo purposes, our question is then 'how can we model this?' and 'how does the light intensity, inside and outside of the cavity, depend on some experimental variables'?

## Setup
Assuming a simple two-mirror setup, with independent reflectivities and
transmission, the system can be described as a ray oscillating between four key interfaces, as illustrated in the figure.")
(k/hiccup [:img {:src "notebooks/for_scientists/Cavity-MirrorScatteringLoss.png"}])
(k/md "
We must therefore consider the electric field at each interface, before solving for the intensity both inside and outside.

## Interfaces
Setting the incoming field amplitude to unity, then the value of the field at position one, after one round trip, is the sum of the transmission coefficient and the field travelling in the opposing direction e.g.
")

(def E1
  (w/+ 't1 (w/* 'r1 (w/- 'E4))))

(k/md "At position two, the same field has travelled the full length of the cavity, L, and so has picked up a change in phase,")
(def phi
  (w/* 'k 'L))
(k/md ", where k is the field wavenumber:")

(def E2
  (w/* E1 (w/Exp (w/* w/I phi))))

(k/md "At position three, the field undergoes reflection and so now carries an additional reflection coefficient as well as an extra π phase change.

Note how we are able to seamlessly mix Clojure and Wolfram expressions in these expressions.")

(def E3
  (w/* (w/- 'r2) E2))
(k/md "By position four, the field has travelled a further distance L and so is now equal to ")
(def E4
  (w/* E3 (w/Exp (w/* w/I phi))))
(TeX-> E4)

(k/md " ## Fields
 Using these expressions, we can now calculate the inner, transmitted and reflected fields of the cavity (with some subtle phase assumptions). Note how easy it is to, first of all, form an equation from a symbol 'E4 and the clojure variable E4, and, second of all, to rearrange the equation for a solution.

Substitution and simplification can then also be used to arrive at the transmission and reflection, respectively.
")

(def e4 (-> (w/== 'E4 E4)
            (w/Solve 'E4)
            w/First w/First))
(eval-> e4)

(def T (-> (w/.>> (w/* E2 't2)
                  e4)
           w/>><<))
(TeX-> T)

(def R (-> (w/+ (w/* E4 't1) 'r1)
           (w/.>> e4)
           w/>><<
           w/Together))
(TeX-> R)

(k/md "## Observables
 Taking the square of the fields (using the complex conjugate), gives the corresponding observables, i.e. things that can be actually measured, for the case of no loss.")

(def I4 (-> (w/.>> 'E4 e4) ||2
            w/++<->
            w/>><<))
(TeX-> (w/== 'I4 (w/** (w/Abs 'E4) 2) I4))

(def Tsq (-> T ||2
             w/++<->
             w/>><<))
(TeX-> (w/== (w/** 'T 2) Tsq))

(def Rsq (-> R ||2
             w/++<->
             w/>><<
             w/Together))
(TeX-> (w/== (w/** 'R 2) Rsq))

(k/md "## Observables with loss
The above result is a perfectly reasonable, but simple, model. More realistically, we want to be able to understand mirrors that have losses, i.e. that don't just reflect or transmit light, but that actually absorb or 'waste' light.

This is one of the places that Wolfram really shines. Almost the entire engine is designed around replacement rules. And so we just have to define a substitution, which says that reflection + transmission + loss is equal to 1:
")

(def losses
  [(w/-> (w/** 'r1 2)
         (w/+ 1
              (w/- 'l1)
              (w/- (w/** 't1 2))))
   (w/-> (w/** 'r2 2)
         (w/+ 1
              (w/- 'l2)
              (w/- (w/** 't2 2))))])

(k/md "and substitute these into the equations. We're going to go a step further however...")

(k/md "## Approximations
Rather than just substitute the new rule into our expressions, we're going to take this opportunity to define some other replacement rules. Now so far, we've been fairly mathematically pure, but a lot of physics is about knowing when (and being able) to make good approximations.
")

(def approximations
  [(w/-> (w/* 'r1 'r2)
         (let [vars [(w/** 't1 2) (w/** 't2 2) 'l1 'l2]]
           (->  (w/.>> (w/* (w/√ (w/** 'r1 2))
                            (w/√ (w/** 'r2 2)))
                       losses)
                (w/Series ['t1 0 2]
                          ['t2 0 2]
                          ['l1 0 2]
                          ['l2 0 2])
                w/Normal
                w/<<->>
                (w/.>> (mapv #(w/-> % (w/* 'temp %)) vars))
                (w/Series ['temp 0 1])
                w/Normal
                (w/.>> (w/-> 'temp 1)))))])
(TeX-> approximations)

(k/md "What's happening here is that we are simplifying the expression for r1*r2 by assuming that the transmission and loss for light going through a mirror is small. So small, that higher powers of these variabes are negligible. And so, we substitute the values into the expression, expand the functions in a power series and neglect any higher powers. The neglect is done by inserting ['big O' notation](https://mathworld.wolfram.com/Big-ONotation.html) and then restricting the series to a single power in that variable. As we want to do this for multiple variables, then we map over each one. This is a complicated mathematical procedure, but Wolframite allows us to do this quite concisely, apart from some necessary Wolfram datatype conversions (e.g. using the w/Normal function).

Note that Wolfram can also be used to define quite general approximations, using the 'Pattern' system. For example, let us assume that we want to expand Cos(x) as a polynomial when x is small. First, we need to know what the expansion is. We can find out by using Wolfram similarly to the above, i.e. ")

(eval-> (w/Series (w/Cos 'x)
                  ['x 0 2])
        w/Normal)

(k/md "Now that we know (or have remembered!) the form, we can create a general rule that is not limited to specific symbol definitions.")

(def small-angle
  (w/..> (w/Cos (w/Pattern 'x (w/Blank)))
         (w/+ 1 (w/* -1/2 (w/** 'x 2)))))

(k/md "The rule can now be named and used for any Cos function with any argument:")

(eval-> (w/.>> (w/Cos 'phi) small-angle))
(eval-> (w/.>> (w/Cos (w/+ 1 'phi)) small-angle))

(k/md "## Independent mirrors
With these approximations we can formulate our final expression for the optical intensity inside the cavity: ")
(TeX-> I4
       (w/.>> (w/-> (w/Cos (w/* 2 'k 'L))
                    (w/Cos 'phi)))
       (w/.>> approximations)
       (w/.>> (w/-> (w/** (w/* 'r1 'r2) 2)
                    (-> (w/.>> (w/* 'r1 'r2) approximations)
                        (w/** 2))))
       (w/.>> (conj losses small-angle))
       w/><
       (->> (w/== (w/** (w/Abs 'E4) 2))))
