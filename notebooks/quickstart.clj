;; # Quickstart {#sec-quickstart}
;;
;; A brief demonstration of what Wolframite can do. It's a quick read, but you can also jump to any section of interest.
;
;; To be extra 'meta', this page is itself a demonstration of a literate programming workflow and, as such, is simply a Clojure
;; namespace annotated using the [Kindly](https://scicloj.github.io/kindly-noted/kindly) system. Currently, this is the recommended
;; way of using Wolframite: for the ability to clearly display equations and plots. To use this system, simply look at the relevant
;; [source](https://github.com/scicloj/wolframite/blob/main/notebooks/quickstart.clj). It is not necessary however; it is still possible,
;; and productive, to use the Clojure REPL (→ @sec-clojure-repl) directly.
;;
;; First, let's require few Wolframite namespaces
(ns quickstart
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [wolframite.api.v1 :as wl]
   [wolframite.core :as wc]
   [wolframite.lib.helpers :as h]
   [wolframite.runtime.defaults :as defaults]
   [wolframite.tools.hiccup :as wh]
   [wolframite.wolfram :as w :refer :all
    :exclude [* + - -> / < <= = == > >= fn
              Byte Character Integer Number Short String Thread]]
   [scicloj.kindly.v4.kind :as k]))

(k/md "## Init

Before we do anything else, we must initialize Wolframite and connect it to a Wolfram kernel,
which will perform the computations:")

(wl/start!) ; (Assuming you've downloaded and activated Wolfram Engine / Mathematica.)

(k/md "If you get an error then please refer to the [Wolframite README](https://github.com/scicloj/wolframite/blob/main/README.md) for further instructions. Your Wolfram installation is probably just in an unusual place and so you will have to provide the correct path.")

(k/md "## First things first

To check that the kernel is working, try the following command:")
(wl/! (w/Dot [1 2 3] [4 5 6]))

(k/md "
`wl/!` asks Wolframite to translate the following expression to Wolfram and send it to a Wolfram kernel for evaluation. ")
(k/md "We could also use one of our fancy aliases,")
(wl/! (w/<*> [1 2 3] [4 5 6]))
(k/md ", which may be more familiar to the Mathematically inclined. If you're interested in adding your own aliases, then have a look at
@sec-wolfram-basics.

Here, the `w` namespace is a preconfigured, but configurable, intermediary to Wolfram's built-in symbols. This allows you to manipulate Wolfram functions just like any other Clojure symbol (and get reliable editor autocompletion).")

(k/md "## Code strings

The above examples are the preferred ways for Clojure and Wolfram to interoperate. You can however, use Wolfram command strings directly, e.g.")

(wl/! "{1 , 2, 3} . {4, 5, 6}")

(k/md "More info in Understanding Wolframite > Wolfram string form @sec-wolfram-string-form")

(k/md "## Bidirectional translation (experimental)

Code translation in both directions is more difficult and is still somewhat fragile (especially in the wl->clj direction), but the basics work as expected, e.g.")

(wl/->clj "GridGraph[{5, 5}]")
(wl/->wl (w/GridGraph [5 5]))

(k/md "Both these functions may be helpful when writing and troubleshooting your Wolframite code.")

(k/md "## Help!

To learn more about Wolfram symbols, use docstrings attached to their vars, either in your IDE or manually:
")

(comment
  ;; Commented out b/c clojure.repl is only available in the REPL
  (clojure.repl/doc w/Dot))
; =>
^:kindly/hide-code
(k/hiccup [:code "-------------------------\nwolframite.wolfram/Dot\n  a.b.c or Dot[a, b, c] gives products of vectors, matrices, and tensors.\n"])

;; You can also open Wolfram documentation of the function, or just print its URL:
(wl/help! w/Dot :return-links true)

;; We can even pass it a whole form, and get a link for each symbol:
(wl/help! (w/Plus (w/Sqrt 1600) 2) :return-links true)

(k/md "## Graphics

The above code however, within Mathematica, actually produces graphics. Does Wolframite support this?

Yes!
")

(wh/show (w/GridGraph [5 5]))

;; We could also show it in a Java window (commented out, because we're building a web page):
(comment
  ((requiring-resolve 'wolframite.tools.graphics/show!)
   (w/GridGraph [5 5])))

(wh/show (w/ChemicalData "Ethanol" "StructureDiagram"))

(wh/show (w/TextStructure "The cat sat on the mat."))

(k/md "The above graphics were created using the `show` function from `wolframite.tools.hiccup`, as required above, and assumes that graphics are to be displayed in a browser.

There are other also ways to display graphics, for example using `wolframite.tools.graphics` to display it in a Java window,
or using `wl/Export` to export it into an image file.")

(k/md "## Computational knowledge

Wolfram is also known for its dynamic or 'computational knowledge' engine.

This can be accessed by many functions directly, ")

(wh/show (w/GeoNearest (w/Entity "Ocean") w/Here))
(k/md ", or by posting a request to its online platform, Wolfram Alpha.")

(wl/! (w/WolframAlpha "number of moons of Saturn" "Result"))

(wh/show (w/WolframAlpha "number of moons of Saturn" "Result"))
(k/md "Here, we've shown a response with and without the show function, for reference.")

(k/md "## Mathematics and beyond

In a nutshell, this is how Wolframite is normally used, but, of course, this barely scratches the surface.

In particular, the flagship product of Wolfram, the one you've probably heard of, is Mathematica. And, as the name suggests, this entire system was built around the performance of abstract calculation and the manipulation of equations, e.g.
")
(k/tex
 (-> (w/== 'E (w/* 'm (Power 'c 2)))
     TeXForm
     ToString
     wl/!))

(k/md "originally answered the question 'what is mass?'")

(k/tex
 (-> (w/== 'E (w/* 'm (Power 'c 2)))
     (Solve 'm)
     First First

     TeXForm
     ToString
     wl/!))

(k/md "This is where Wolfram, and so Wolframite, really shines. And if you're interested in exploring this further, have a look at one of our longer tutorials.
")

(k/md (format "
## Productivity tip

When you write a lot of Wolframite, it may be a little annoying with all the `w/` prefixes. Therefore,
we recommend to use this require form instead:
```clojure
(ns whatever
  (:require [wolframite.wolfram :as w :refer :all
             :exclude %s]))
```

Then you can refer to most symbols directly, with the exception of those that conflict with Clojure's core functions
and Java classes, which could lead to confusion and mistakes.

With this, you can write:
"
              (pr-str (wc/ns-exclusions :assert-as-expected))))

(wl/! (Map (w/fn [x] (Power x 2)) (Table 'i ['i 1 3])))

(k/md "## Wolfram has a lot to offer

You may also want to browse the [thousands of functions from various domains that Wolfram provides](https://www.wolfram.com/language/core-areas/).
These domains include Machine Learning, Calculus & Algebra, Optimization, Geography, and many more.")

(k/md "## Beware gotchas

@sec-gotchas describes some traps you want to avoid. It may be a good time to quickly scan it.")
