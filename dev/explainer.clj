(ns explainer
  "A demo ns used for explaining the basics of using Wolframite.
  See also the demo ns."
  (:require
   [wolframite.core :as wl]
   [wolframite.tools.graphics :as graphics]
   [wolframite.base.parse :as parse :refer [custom-parse]]
   [wolframite.lib.helpers :as h]))

"Hello Everyone!"

;; ### Start a clerk web server and file watcher

(comment
  (require '[nextjournal.clerk :as clerk])
  (clerk/serve! {:browse? true :watch-paths ["src"]})

  (clerk/serve! {:browse? true})
  (clerk/show! "dev/explainer.clj")

  )

;; * Syntax

;; Wolfram: RulePlot[CellularAutomaton[30]]
;; Clojure: (RulePlot (CellularAutomaton 30))

;; * Eval

(eval    '(map (fn [x] (+ x 1)) [1 2 3])) ; clojure eval

(wl/eval '(Map (fn [x] (+ x 1)) [1 2 3])) ; Wolframite eval

(wl/eval "Map[Function[{x}, x + 1], {1, 2, 3}]") ; can also pass in Wolfram as string

;; |/////////////////////////|
;; |Convert >> Eval >> Parse |
;; |/////////////////////////|

;; * Intern a Wolfram function into a Clojure namespace, so that you can call it:

;; ** def

(def W:Plus (parse/parse-fn 'Plus {:kernel/link @wl/kernel-link-atom}))
(W:Plus 1 2 3) ; => 6

(def greetings
  (wl/eval
   '(Function [x] (StringJoin "Hello, " x "! This is a Mathematica function's output."))))

;; ** intern

(wl/clj-intern 'Plus {})

(map wl/clj-intern ['Dot 'Plus])

;; BEWARE: This is somewhat slow
(wl/load-all-symbols 'w) ; intern all Wolfram functions and entities into the namespace `w`; with docstrings
(wl/load-all-symbols (.name *ns*)) ; some, but into the current ns

;; * REPL - load-all-symbols includes docstrings so we can use repl to show them

(require '[clojure.repl :as repl])

(repl/doc w/GeoGraphics)

(repl/find-doc "two-dimensional")

(repl/apropos #"(?i)geo")

(h/help! 'Axes)

(h/help! '(Take
           (Sort
            (Map
             (Function [gene]
                       [(GenomeData gene "SequenceLength") gene])
             (GenomeData)))
           n)
         :return-links true)

(Information 'GenomeData)

(wl/eval '((WolframLanguageData "GenomeData") "Association"))

;; * Graphics

;; Init
(def canvas (graphics/make-math-canvas! @wl/kernel-link-atom))
(def app (graphics/make-app! canvas))
(defn quick-show [clj-form]
  (graphics/show! canvas (wl/->wl! clj-form {:output-fn str})))

(quick-show '(ChemicalData "Ethanol" "StructureDiagram"))
(quick-show '(GridGraph [5 5]))
(quick-show '(GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"])))

;; * Custom Parse

(wl/eval '(Hyperlink "foo" "https://www.google.com"))

(defmethod custom-parse 'Hyperlink [expr opts]
  (-> (second (.args expr))
      (parse/parse opts)
      java.net.URI.))

(wl/eval '(Hyperlink "foo" "https://www.google.com") {:flags [:custom-parse]
                                                      :parse/custom-parse-symbols ['Hyperlink]})
;; * More

;; WordFrequency[ExampleData[{"Text", "AliceInWonderland"}], {"a", "an", "the"}, "CaseVariants"]

(WordFrequency (ExampleData ["Text" "AliceInWonderland"]) ["a" "an" "the"] "CaseVariants")

;; Wrapping in a function
(defn wf [& {:keys [prop]
             :or   {prop "CaseVariants"}}]
  (WordFrequency (ExampleData (conj ["Text"] "AliceInWonderland"))
                 (vector "a" "an" "the")
                 prop))

(wf :prop "Total")

(h/help! 'WordFrequencyData)
