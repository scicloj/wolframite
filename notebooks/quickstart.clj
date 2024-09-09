;; # Quickstart {#sec-quickstart}
;;
;; A brief demonstration of what Wolframite can do. It's a quick read, but you can also jump to any section of interest.
;
;; To be extra 'meta', this page is itself a demonstration of a literate programming workflow and, as such, is simply a Clojure
;; namespace annotated using the [Kindly](https://scicloj.github.io/kindly-noted/kindly) system. Currently, this is the recommended
;; way of using Wolframite: for the ability to clearly display equations and plots. To use this system, simply look at the relevant
;; [source](https://github.com/scicloj/wolframite/). It is not necessary however; it is still possible, and productive, to use the
;; REPL directly.
;;
;; First, let's require few Wolframite namespaces
(ns quickstart
  (:require
   [wolframite.core :as wl]
   [wolframite.wolfram :as w]
   [wolframite.tools.hiccup :as wh]
   [scicloj.kindly.v4.kind :as k]))

(k/md "## Init

Before we do anything else, we must initialize Wolframite and connect it to a Wolfram kernel,
which will perform the computations:")

(wl/start!)

(k/md "If you get an error then please refer to the [Wolframite README](https://github.com/scicloj/wolframite/blob/main/README.md) for further instructions. Your Wolfram installation is probably just in an unusual place and so you will have to provide the correct path.")

(k/md "## First things first

To check that the kernel is working, try the following command:")
(wl/eval (w/Dot [1 2 3] [4 5 6]))

(k/md "
`wl/eval` asks Wolframite to translate the following expression to Wolfram and send it to a Wolfram kernel for evaluation. ")
(k/md "We could also use one of our fancy aliases,")
(wl/eval (w/<*> [1 2 3] [4 5 6]))
(k/md ", which may be more familiar to the Mathematically inclined. If you're interested in adding your own aliases, then have a look at @sec-wolfram-basics.

Here, the `w` namespace is a preconfigured, but configurable, intermediary to Wolfram's built-in symbols. This allows you to manipulate Wolfram functions just like any other Clojure symbol (and get reliable editor autocompletion).")

(k/md "## Code strings

The above examples are the preferred ways for Clojure and Wolfram to interoperate. You can however, use Wolfram command strings directly, e.g.")

(wl/eval "{1 , 2, 3} . {4, 5, 6}")

(k/md "## Bidirectional translation (experimental)

Code translation in both directions is more difficult and is still somewhat fragile (especially in the wl->clj direction), but the basics work as expected, e.g.")

(wl/->clj "GridGraph[{5, 5}]")
(wl/->wl (w/GridGraph [5 5]) {:output-fn str})

(k/md "Both these functions may be helpful when writing and troubleshooting your Wolframite code.")

(k/md "## Graphics

The above code however, within Mathematica, actually produces graphics. Does Wolframite support this?

Yes!
")

(wh/view (w/GridGraph [5 5]))

(wh/view (w/ChemicalData "Ethanol" "StructureDiagram"))

(wh/view (w/TextStructure "The cat sat on the mat."))

(k/md "The above graphics were created using the `view` function from `wolframite.tools.hiccup`, as required above, and assumes that graphics are to be displayed in a browser.")

(k/md "## Computational knowledge

Wolfram is also known for its dynamic or 'computational knowledge' engine.

This can be accessed by many functions directly, ")

(wh/view (w/GeoNearest (w/Entity "Ocean") w/Here))
(k/md ", or by posting a request to its online platform, Wolfram Alpha.")

(wl/eval (w/WolframAlpha "number of moons of Saturn" "Result"))

(wh/view (w/WolframAlpha "number of moons of Saturn" "Result"))
(k/md "Here, we've shown a response with and without the view function, for reference.")

(k/md "## Mathematics and beyond

In a nutshell, this is how Wolframite is normally used, but, of course, this barely scratches the surface.

In particular, the flagship product of Wolfram, the one you've probably heard of, is Mathematica. And, as the name suggests, this entire system was built around the performance of abstract calculation and the manipulation of equations, e.g.
")
(-> (w/== 'E (w/* 'm (w/Power 'c 2)))

    w/TeXForm
    w/ToString
    wl/eval
    k/tex)

(k/md "originally answered the question 'what is mass?'")

(-> (w/== 'E (w/* 'm (w/Power 'c 2)))
    (w/Solve 'm)
    w/First w/First

    w/TeXForm
    w/ToString
    wl/eval
    k/tex)

(k/md "This is where Wolfram, and so Wolframite, really shines. And if you're interested in exploring this further, have a look at one of our longer tutorials.")
