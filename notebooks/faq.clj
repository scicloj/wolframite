(ns faq
  "Anticipated and, potentially literally, frequently asked questions."
  (:require
   [scicloj.kindly.v4.kind :as k]
   [wolframite.api.v1 :as wl]
   [wolframite.wolfram :as w :refer :all
    :exclude [* + - -> / < <= = == > >= fn
              Byte Character Integer Number Short String Thread]]))

(wl/start!)

(k/md "# FAQ {#sec-FAQ}
**Why Wolfram?** - From the horse's [mouth](https://www.wolfram.com/language/):

> Wolfram Language is a symbolic language, deliberately designed with the breadth and unity needed to develop powerful programs quickly. By integrating high-level forms—like Image, GeoPolygon or Molecule—along with advanced superfunctions—such as ImageIdentify or ApplyReaction—Wolfram Language makes it possible to quickly express complex ideas in computational form.

More concretely, the Wolfram language, and associated ecosystem, is a powerful way of getting stuff done. As a high-level functional programming language, with [more than 6000 functions from across mathematics and data processing](https://reference.wolfram.com/language/), it is sometimes the only practical way of of getting things done. Particularly in explicitly scientific fields. What makes it potentially unique is its place in the Venn diagram of symbolic manipulation, built-in factual and computational knowledge and niche, fully implemented algorithms.

Check out the [Wolfram Language Code Gallery](https://www.wolfram.com/language/gallery/) for some amazing examples of what you can achieve with Wolfram much more easily than with any other programming language under the sun.

**Why Wolframite?**

'Why Wolframite?' is really just the combination of 'Why Wolfram?' (above) and '[Why Clojure?](https://clojure.org/about/rationale)'.

In a nutshell, the Wolfram programming language is 'best-in-class' for certain specialist tasks, like manipulating equations, but, to paraphrase [Randall Munroe](https://xkcd.com/1270/),
> Wolfram combines the flexibility and power of abstract mathematics with the intuitive clarity of abstract mathematics.

Clojure, on the other-hand, is a first-class, general-purpose, programming language whose core namespace and dynamical, functional, paradigm is well suited to data exploration and manipulation. So why don't we call Wolfram from Clojure? It seems like the atypical 'best of both worlds'.

To put it another way, Wolfram is *a powerful tool in need of a toolbox*. And so what language is best placed to be that 'box'? Well, Wolfram is, underneath, built on LisP and so will naturally work better with a similar design philosophy (and way of thinking) than, say, with an object-oriented or scripting language. Furthermore, somewhat ironically, it has a very good 'link' to the Java ecosystem. In [fact](https://reference.wolfram.com/language/JLink/tutorial/Introduction.html#25629),

>  For all types of WSTP programs, J/Link provides a higher-level layer of functionality than the traditional C WSTP programming interface. This makes Java the easiest and most convenient language for writing programs to interact with the Wolfram Language.

Even at a fundamental level therefore, our toolbox language should be a LisP with strong Java interop: mmm...!

At the *usability* layer, Clojure is a well-designed, ergonomic language that leaves its users [happy](https://www.computerworld.com/article/1379902/clojure-developers-are-the-happiest-developers.html)! When it comes to working with mathematical expressions specifically, there are a few key features. First of all, for many people, Clojure was the first language to introduce ergonomic chaining of function expressions, *e.g.*
")

(-> (w/* 'x I)
    (w/+ 'y)
    Abs
    (** 2)
    ComplexExpand

    TeXForm
    ToString
    wl/!
    k/tex)

(k/md ". In my view, chaining Wolfram function calls together with [threading macros](https://clojure.org/guides/threading_macros) (*e.g.* `->` and `->>` etc.) is actually a big usability improvement. Wolfram expressions can get pretty involved (it's common to end up with expressions that hold 10s of symbols and operators and 100s are not unheard of) and trying to read and manipulate these from the inside out is just not natural for the average human. It stands to reason then that chaining functions together (and debugging them!) can really be a pain. In fact, Wolfram recognised this problem when it introduced the prefix operator, `@`, to help with function composition, *e.g.* `f@g@h`. Unfortunately however, this doesn't work with multiple arguments. It is possible to do things like `f@@args`, and even things like `f@@@{{a, b}, {c, d}}`, but the readability quickly becomes dire. On the other hand, Clojure's threading is simple, clear and scalable.
Secondly, by using a LisP, you are automatically thinking (and evaluating) at the level of the *symbolic expression* (a.k.a. s-expression). This is in contrast to thinking at the level of 'files', lines or 'cells' of a notebook. This maps very well to the exploration of *mathematical* expressions. In fact, it's almost surprising that Wolfram had such a big influence on the idea of *notebook*-style programming, considering that, at heart, it's all LisP. In our view, Wolframite brings Wolfram back to a more fundamental form of **literate programming**.


**Why literate programming?**

One of the key motivations of Wolframite is to integrate the power of Wolfram within a more general, more ergonomic programming environment. When it comes to solving complex problems however, it is also important to be able to document and visualise the process. In fact, this is a core part of the academic sciences and consumes a lot of time. Largely however, solving problems and documenting the results have been considered orthogonal processes: duplicating huge amounts of work

In contrast, what if

> Programs are meant to be read by humans and only incidentally for computers to execute. [*D. Knuth*]?

This is the heart of *literate programming*.

The immediate question that flows from this however, is *how*? In TeX systems, the answer was to include executable code within running text. In Mathematica, which largely popularized the *notebook* model, a fully integrated IDE product was built around the underlying language. Jupyter carried on this idea, but universalized the concept by bringing the interaction into the browser.

And yet, for programmers, both of these systems are the wrong way around. The core content and information is in the namespaces and the right level of abstraction is not at arbitrarily chosen *cells*, but rather evaluation at the level of *expressions*. Such is the case, a LisP-style evaluation model is one of the biggest advantages of Woframite, particularly when paired with *notebooks as a namespace*-style literate programming, *e.g.* [clay](https://github.com/scicloj/clay) and [clerk](https://github.com/nextjournal/clerk). When it comes to analysis, being able to interrogate data interactively is often as, if not more, important than being able to run long simulations. With LisP-style literate programming this becomes even more powerful and the other thing to bear in mind is that with literate programming, the docs double as tests!

For more information about the downsides of the *notebook*-model, have a look at this [paper](https://www.microsoft.com/en-us/research/uploads/prod/2020/03/chi20c-sub8173-cam-i16.pdf), as cited by the Clerk document system.


**Why not Emmy?**

You *should* use [Emmy](https://github.com/mentat-collective/emmy)! Emmy is a fully Clojure(script) symbolic algebra engine that is built on the [SCMUTILS](https://groups.csail.mit.edu/mac/users/gjs/6946/refman.txt) system, orginally developed by G.J. Sussman and J. Wisdom. Emmy is a very exciting prospect for symbolic manipulation, but is only a subset of what Wolfram is capable of. In the future, we hope to integrate the two more closely. Our current advice though, is that where you have to do a lot of `Solve`-ing (*i.e.* rearranging of equations), or where you need a more powerful simplifier, use Wolframite.


**How do I translate Mathematica's syntax sugar?**

Since *Mathematica* hides the internals of the *Wolfram Language* by default, it can sometimes be difficult to work out what functions are being called. If you're in Mathematica, you can use `FullForm@Unevaluated[...]` to get a more understandable (if less concise) expression. Of course, you can also use Wolframite, *i.e.* `(wl/->clj ...)` !
")
