(ns demo
  "Demonstrate key features of Wolframite.
  See also the explainer ns."
  ;; FIXME Why do we have both demo and explainer? Could we settle on one? If not then clarify the difference!
  (:require
   [wolframite.api.v1 :as wl]
   [wolframite.impl.jlink-instance :as jlink-instance]
   [wolframite.wolfram :as w]
   [wolframite.tools.graphics-legacy-awt :as graphics]
   [wolframite.base.parse :as parse]
   [wolframite.runtime.defaults :as defaults]
   [wolframite.base.convert :as convert]
   [wolframite.base.evaluate :as evaluate]
   [wolframite.base.express :as express]))

(comment

  ;; !!! INIT WOLFRAMITE !!!
  (wl/start!)

  ;; Low-level evaluate of a Wolfram expression (here, `Plus[1,2]`)
  ;; You normally don't want this - just use wl/!. Notice that all the connection details explicitly specified.
  ((parse/parse-fn 'Plus (merge {:jlink-instance (jlink-instance/get)}
                                defaults/default-options))
   1 2))

;; * Clojure a 1min intro
;; ** Interactive developement
;; *** Evaluating forms from a text editor connected to a REPL

(+ 1 1)

;; ** Syntax a.k.a. data structures
;; *** Interesting scalar datatypes

;; - symbols: a b c d

;; unqoted
;; x

;; (def x "hello")

;; quoted
'a

;; - keywords

:hello

;; namespaced

:project.media/image

;; *** Interesting compound datatypes

;; vector

[1 3 :a :b 'foo 'bar]

;; hash map

{:a 23
 :b 73}

;; * Wolframite (nee Clojuratica) -- name is a WIP
;; ** Init (base example)

(wl/! (w/Dot [1 2 3] [4 5 6]))

;; ** Strings of WL code

(wl/! "{1 , 2, 3} . {4, 5, 6}")
;; (convert/convert "{1 , 2, 3} . {4, 5, 6}" {:jlink-instance (wolframite.impl.jlink-instance/get)}) ;; FIXME: this doesn't work, probably shouldn't, maybe there is a path to remove in `convert/convert`
;; You should use this
;; (express/express "{1 , 2, 3} . {4, 5, 6}" {:jlink-instance (wolframite.impl.jlink-instance/get)})

;; NOTE: this is a hack to get this to work

;; ** Def / intern WL fns, i.e. effectively define WL fns as clojure fns:

(def W:Plus (parse/parse-fn 'Plus {:jlink-instance (jlink-instance/get)}))

(W:Plus 1 2 3) ; ... and call it (delegating its evaluation to the Wolfram kernel)

(def greetings
  (wl/!
   (w/fn [x] (w/StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings "Stephen")

;; ** Bidirectional translation
;; (Somewhat experimental, especially in the wl->clj direction)

(wl/->clj "GridGraph[{5, 5}]")
(wl/->wl '(GridGraph [5 5]) {:output-fn str})

;; ** Graphics

;; *** Init math canvas & app

(def canvas (graphics/make-math-canvas!))
(def app (graphics/make-app! canvas))

;; *** Draw Something!

(graphics/show! canvas (wl/->wl (w/GridGraph [5 5]) {:output-fn str}))
(graphics/show! canvas (wl/->wl (w/ChemicalData "Ethanol" "StructureDiagram") {:output-fn str}))

;; *** Make it easier (Dev Helper: closing over the canvas)

(defn quick-show [clj-form]
  (graphics/show! canvas (wl/->wl clj-form {:output-fn str})))

;; *** Some Simple Graphiscs Examples

(quick-show (w/ChemicalData "Ethanol" "StructureDiagram"))
(quick-show (w/GridGraph [5 5]))
(quick-show (w/GeoImage (w/Entity "City" ["NewYork" "NewYork" "UnitedStates"])))

;; ** More Working Examples

(wl/! (w/GeoNearest (w/Entity "Ocean") 'Here))

(wl/! (w/TextStructure "The cat sat on the mat."))

;; - Wolfram Alpha

(wl/! (w/WolframAlpha "number of moons of Saturn" "Result"))

;; - more interesting examples...

(wl/! (w/TextStructure "You can do so much with the Wolfram Language." "ConstituentGraphs")) ; returns a graph as data
