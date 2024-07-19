(ns explainer
  "A demo ns used for explaining the basics of using Wolframite.
  See also the demo ns." ;; FIXME how does demo and this one differ?
  (:require
   [wolframite.core :as wl]
   [wolframite.wolfram :as w]
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

;; ### Init Wolframite
(wl/start)

;; * Syntax

;; Wolfram: RulePlot[CellularAutomaton[30]]
;; Clojure: (RulePlot (CellularAutomaton 30))

;; * Eval

(eval    '(map (fn [x] (+ x 1)) [1 2 3])) ; clojure eval

(wl/eval (w/Map (fn [x] (w/+ x 1)) [1 2 3])) ; Wolframite eval ;; FIXME(jakub) broken, doesn't call +; likely b/c we send fail to process the fn body correctly

(wl/eval "Map[Function[{x}, x + 1], {1, 2, 3}]") ; can also pass in Wolfram as string

;; |/////////////////////////|
;; |Convert >> Eval >> Parse |
;; |/////////////////////////|

;; * Intern a Wolfram function into a Clojure namespace

;; ** define a Clojure fn, which will evaluate Wolfram code:

(def greetings
  (wl/eval
   (w/fn [x] (w/StringJoin "Hello, " x "! This is a Mathematica function's output."))))

(greetings, "folks") ; => "Hello, folks! This is a Mathematica function's output."

;; ** create a var for each Wolfram symbol, with docstrings, which resolves into a symbol suitable for `wl/eval`:

(def age 42)
;; Without symbols loaded, when we need interpolation, we need to deal with ` and prevent namespacing of symbols:
(wl/eval `(~'Floor (~'Plus ~age ~'Pi))) ; => 45
;; But better to use loaded symbols:
(wl/eval (w/Floor (w/Plus age w/Pi))) ; => 45

;; *** REPL - load-all-symbols includes docstrings, so we can use repl to show them

(require '[clojure.repl :as repl])

(repl/doc w/GeoGraphics)

(repl/find-doc "two-dimensional")

(repl/apropos #"(?i)geo")

(h/help! 'Axes) ;; open a Wolfram docs page for Axes
(h/help! w/Axes) ; thx to loaded symbols, this works too

;; Liks to all the symbols in this form:
(h/help! '(Take
           (Sort
            (Map
             (Function [gene]
                       [(GenomeData gene "SequenceLength") gene])
             (GenomeData)))
           n)
         :return-links true)
(h/help! (w/Take
            (w/Sort
              (w/Map
                ;(w/Function ['gene] [(w/GenomeData 'gene "SequenceLength") 'gene])
                (w/fn [gene] [(w/GenomeData gene "SequenceLength") gene])
                (w/GenomeData)))
            'n)
         :return-links true)

(wl/eval (w/Information w/GenomeData))

(wl/eval '((WolframLanguageData "GenomeData") "Association")) ; FIXME broken? returns a few 'Missing "NotAvailable'

;; * Graphics

;; Init
(def canvas (graphics/make-math-canvas!))
(def app (graphics/make-app! canvas))
(defn quick-show [clj-form]
  (graphics/show! canvas (wl/->wl clj-form {:output-fn str})))

(quick-show (w/ChemicalData "Ethanol" "StructureDiagram"))
(quick-show (w/GridGraph [5 5]))
(quick-show (w/GeoImage (w/Entity "City" ["NewYork" "NewYork" "UnitedStates"])))

;; * Custom Parse

(wl/eval (w/Hyperlink "foo" "https://www.google.com"))

(defmethod custom-parse 'Hyperlink [expr opts]
  (-> (second (.args expr))
      (parse/parse opts)
      java.net.URI.))

(wl/eval (w/Hyperlink "foo" "https://www.google.com"))
;; * More

;; WordFrequency[ExampleData[{"Text", "AliceInWonderland"}], {"a", "an", "the"}, "CaseVariants"]

(wl/eval (w/WordFrequency (w/ExampleData ["Text" "AliceInWonderland"]) ["a" "an" "the"] "CaseVariants"))

;; Wrapping in a function
(defn wf [& {:keys [prop]
             :or   {prop "CaseVariants"}}]
  (wl/eval (w/WordFrequency (w/ExampleData (conj ["Text"] "AliceInWonderland"))
                  (vector "a" "an" "the")
                  prop)))

(wf :prop "Total")

(h/help! 'WordFrequencyData)
