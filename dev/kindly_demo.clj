;; # Wolframite Kindly demo

;; This notebook demonstrates basic usage of [Wolframite](https://github.com/scicloj/wolframite/) in a way that would work in visual tools supporting [Kindly](https://scicloj.github.io/kindly-noted/kindly).

;; It is mostly copied and adpated from [the official Wolframite demo](https://github.com/scicloj/wolframite/blob/main/dev/demo.clj).

(ns kindly-demo
  (:refer-clojure
   ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.core :as wl :refer [wl]]
   [wolframite.tools.hiccup :refer [view]]
   [wolframite.base.parse :as parse]
   [wolframite.jlink]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.kindly.v4.api :as kindly])
  (:import (com.wolfram.jlink MathCanvas KernelLink)
           (java.awt Color Frame)
           (java.awt.event WindowAdapter ActionEvent)))

^:kindly/hide-code
(def md
  (comp kindly/hide-code kind/md))

(md "In interactive Clay interaction, the HTML output will be saved under \"temp\",
as configured in `clay.edn`.

To render under \"docs\", you may do the following for HTML and Quarto-based HTML targets, respectively.

Note that the Quarto-based target requires the [Quarto CLI](https://quarto.org/docs/get-started/).")

(comment
  (require '[scicloj.clay.v2.api :as clay])
  ;; Make HTML:
  (clay/make! {:source-path "dev/kindly_demo.clj"
               :base-target-path "docs"})
  ;; Make Quarto-based HTML
  ;; (requores the Quarto CLI installed):
  (clay/make! {:source-path "dev/kindly_demo.clj"
               :base-target-path "docs"
               :format [:quarto :html]}))

(md "## Init (base example)")
^:note-to-test/skip

(wl/eval '(Dot [1 2 3] [4 5 6]))

(md "## Strings of WL code")

(wl/eval "{1 , 2, 3} . {4, 5, 6}")

(md "## Def // intern WL fns, i.e. effectively define WL fns as clojure fns:")

(def W:Plus (parse/parse-fn 'Plus {:kernel/link @wl/kernel-link-atom}))

(W:Plus 1 2 3) ; ... and call it


(def greetings
  (wl/eval
   '(Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings "Stephen")

(md " ## Bidirectional translation
(Somewhat experimental, especially in the wl->clj direction)")


(wl/->clj! "GridGraph[{5, 5}]")
(wl/->wl! '(GridGraph [5 5]) {:output-fn str})

(md "## Graphics")

(view
 '(GridGraph [5 5]))

(view
 '(GridGraph [5 5])
 {:folded? true})

(view
 '(ChemicalData "Ethanol" "StructureDiagram"))

(md "## More Working Examples")

(wl/eval '(GeoNearest (Entity "Ocean") Here))

(md " TODO: Make this work with `view` as well.")

(view '(TextStructure "The cat sat on the mat."))

(md "## Wolfram Alpha")

(wl/eval '(WolframAlpha "number of moons of Saturn" "Result"))

(view '(WolframAlpha "number of moons of Saturn" "Result"))
