(ns demo
  (:require
   [clojuratica.core :as wl]
   [clojuratica.tools.graphics :as graphics]
   [clojuratica.base.parse :as parse]
   [clojuratica.runtime.defaults :as defaults]
   [clojuratica.base.convert :as convert]
   [clojuratica.base.evaluate :as evaluate]
   [clojuratica.base.express :as express])
  (:import [com.wolfram.jlink Expr MathLinkFactory]))

(comment

  ;; Low-level evaluate of a Wolfram expression (here, `Plus[1,2]`)
  ;; You normally don't want this - just use wl/eval. Notice that all the connection details explicitly specified.
  ((parse/parse-fn 'Plus (merge {:kernel/link @wl/kernel-link-atom}
                                defaults/default-options))
   1 2)

  )

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


;; *** Interesting compund datatypes

;; vector

[1 3 :a :b 'foo 'bar]

;; hash map

{:a 23
 :b 73}

;; * Wolframite (nee Clojuratica) -- name is a WIP
;; ** Init (base example)

(wl/eval '(Dot [1 2 3] [4 5 6]))

;; ** Strings of WL code

(wl/eval "{1 , 2, 3} . {4, 5, 6}")
;; (convert/convert "{1 , 2, 3} . {4, 5, 6}" {:kernel/link @wl/kernel-link-atom}) ;; FIXME: this doesn't work, probably shouldn't, maybe there is a path to remove in `convert/convert`
;; You should use this
;; (express/express "{1 , 2, 3} . {4, 5, 6}" {:kernel/link @wl/kernel-link-atom})

;; NOTE: this is a hack to get this to work

;; ** Def // intern WL fns, i.e. effectively define WL fns as clojure fns:

(def W:Plus (parse/parse-fn 'Plus {:kernel/link @wl/kernel-link-atom}))

(W:Plus 1 2 3) ; ... and call it

(wl/clj-intern 'Plus {}) ; a simpler way to do the same -> fn Plus in this ns

(map wl/clj-intern ['Dot 'Plus])

(comment
  ;; Call interned Wl functions:
  (Dot [1 2 3] [4 5 6])
  (Plus 1 2 3)
  (Plus [1 2] [3 4]))

(def greetings
  (wl/eval
   '(Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings "Stephen")


;; ** Bidirectional translation
;; (Somewhat experimental, especially in the wl->clj direction)

(wl/->clj! "GridGraph[{5, 5}]")
(wl/->wl! '(GridGraph [5 5]) {:output-fn str})


;; ** Graphics

;; *** Init math canvas & app

(def canvas (graphics/make-math-canvas! @wl/kernel-link-atom))
(def app (graphics/make-app! canvas))

;; *** Draw Something!

(graphics/show! canvas (wl/->wl! '(GridGraph [5 5]) {:output-fn str}))
(graphics/show! canvas (wl/->wl! '(ChemicalData "Ethanol" "StructureDiagram") {:output-fn str}))

;; *** Make it easier (Dev Helper: closing over the canvas)

(defn quick-show [clj-form]
  (graphics/show! canvas (wl/->wl! clj-form {:output-fn str})))

;; *** Some Simple Graphiscs Examples

(quick-show '(ChemicalData "Ethanol" "StructureDiagram"))
(quick-show '(GridGraph [5 5]))
(quick-show '(GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"])))


;; ** More Working Examples

(wl/eval '(GeoNearest (Entity "Ocean") Here))

(wl/eval '(TextStructure "The cat sat on the mat."))

;; - Wolfram Alpha

(wl/eval '(WolframAlpha "number of moons of Saturn" "Result"))

;; * Issues
;; - WL syntactic sugar
;; FIXME: doesn't work
(WL @(a b c d))

;; - more interesting examples...

;; FIXME: some loopiness and NPE
(wl/eval '(TextStructure "You can do so much with the Wolfram Language." "ConstituentGraphs"))
