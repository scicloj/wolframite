(ns demo
  (:require
   [clojuratica :as wl]
   [clojuratica.base.convert :as convert]
   [clojuratica.base.kernel :as kernel]
   [clojuratica.init :as init]
   [clojuratica.runtime.dynamic-vars :as dynamic-vars]
   [clojuratica.base.convert :as convert]
   [clojuratica.runtime.default-options :as default-options]))

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

;; Init

(require '[clojuratica.init :as init :refer [WL]]
         '[clojuratica :as wl])

(WL (Dot [1 2 3] [4 5 6]))

(WL '"{1 , 2, 3} . {4, 5, 6}")

(WL (Plus 23 '"{1 , 2, 3} . {4, 5, 6}"))

(wl/math-intern init/math-evaluate Plus)

(Plus [1 2] [3 4])

(def greetings
  (WL
   (Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings "Stephen")

;; threading fun

(-> "Ocean"
    Entity
    (GeoNearest Here)
    WL)

;; Wolfram Alpha

(WL (WolframAlpha "number of moons of Saturn" "Result"))

;; more examples

;; dealing with WL's syntactic sugar
(WL @(a b c d))

;; more interesting examples...

(WL (TextStructure "The cat sat on the mat."))

(WL (TextStructure "You can do so much with the Wolfram Language." "ConstituentGraphs"))

(wl/math-intern init/math-evaluate :scopes)

#_{:clj-kondo/ignore true}
(Let [t (-> ["Text" "AliceInWonderland"]
            ExampleData
            ContentObject
            Snippet)]
     (WordFrequency ~t (TextWords ~t)))

(wl/mathematica->clojure "GridGraph[{5, 5}]" init/math-evaluate)
;; => (GridGraph [5 5])

(defn clojure->mathematica [clj-form & {:keys [output-fn]}]
  (binding [dynamic-vars/*options* default-options/*default-options*
            dynamic-vars/*kernel*  (kernel/kernel init/kernel-link)]
    (cond-> (convert/convert clj-form)
      (ifn? output-fn) output-fn)))

(def clj->wl clojure->mathematica)


(graphics/show! graphics/canvas (clj->wl '(GridGraph [5 5]) :output-fn str))
(graphics/show! graphics/canvas (clj->wl '(ChemicalData "Ethanol" "StructureDiagram") :output-fn str))

(defn quick-show [clj-form]
  (graphics/show! graphics/canvas (clj->wl clj-form :output-fn str)))

(quick-show '(ChemicalData "Ethanol" "StructureDiagram"))
(quick-show '(GridGraph [5 5]))
(quick-show '(GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"])))

(comment
  (wl/mathematica->clojure
   "GeoImage[Entity[\"City\", {\"NewYork\", \"NewYork\", \"UnitedStates\"}]]"
   init/math-evaluate)
  ;; => (GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"]))
  )
