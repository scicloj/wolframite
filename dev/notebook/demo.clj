(ns notebook.demo
  (:require [wolframite.core  :refer [eval] :as w]
            [wolframite.base.convert :as cv]
            [wolframite.jlink :as jlink]
            [wolframite.lib.helpers :refer [help!]]
            [wolframite.tools.clerk-helper :refer [view]]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [nextjournal.clerk :as nb]
            [nextjournal.beholder :as beholder]
            [nextjournal.clerk.webserver :as webserver]
            [nextjournal.clerk.viewer :as v]
            [cemerick.pomegranate :as pom]
            [clojure.string :as str]
            [clojure.repl :refer [doc find-doc apropos]]))


;; # Wolfram Language Graphics


;; ## Numbers

(view '(BarChart (EntityValue (EntityClass "Planet" All) "Radius")))


;; ## Time

(view '(TimelinePlot
        [(Entity "HistoricalEvent" "WorldWar1")
         (Entity "HistoricalEvent" "WorldWar2")
         (Entity "HistoricalEvent" "VietnamWar")
         (Entity "HistoricalEvent" "KoreanWarBegins")]))

;; ## Geo

(view '(GeoGraphics))

(view '(GeoImage
        (Entity "City" ["NewYork" "NewYork" "UnitedStates"])))

(view '(GeoGraphics
        [Red (GeoPath [(Entity "City" ["Portland" "Oregon" "UnitedStates"])
                       (Entity "City" ["Orlando" "Florida" "UnitedStates"])
                       (Entity "City" ["Boston" "Massachusetts" "UnitedStates"])]
                      "Geodesic")]))


;; ## 3D

(view '(MoleculePlot3D (Molecule "O=C(C1CCC1)S[C@@H]1CCC1(C)C")))

;; # Clojure Graphs

(nb/vl
 (hc/xform ht/point-chart
           :UDATA "https://raw.githubusercontent.com/vega/vega/master/docs/data/cars.json"
           :X "Horsepower"
           :Y "Miles_per_Gallon"
           :WIDTH 500
           :HEIGHT 400
           :COLOR "Origin"))

;; # Sunday evening family friendly fun

(defn image [[_ img]]
  ((last (last img)) "URL"))

(def movie-ents
   (eval
    '(Map (Function [m] [m (m "Image")])
          (Keys
           (Take
            (Reverse
             (Sort
              (DeleteMissing (MovieData
                              (MovieData ["RandomEntities" 300])
                              "DomesticBoxOfficeGross"
                              "EntityAssociation"))))
            2)))))

(nb/html
 [:div.guess-the-movie
  (into [:div [:center [:h2 "Charades: Guess The Movie 📺"]]]
        (mapv (fn [[m _ :as mdata]]
                [:div.movie {:style {:background-color "rgba(255, 255, 3, 0.07)"
                                     :border-radius "1rem"
                                     :padding "1rem"}}
                 [:center
                  [:h5.title {:style {:font-family "Courier"
                                      :margin-bottom "0.5rem"}}
                   (first (str/split (last m) #"::"))]
                  [:img {:src (image mdata)}]]])
              movie-ents))])

(view '(Animate (Plot (Sin (+ x a)) [x 0 10]) [a 0 5] (-> AnimationRunning true))
      :folded? true)

(w/->clj "Plot[Evaluate[Table[BesselJ[n, x], {n, 4}]], {x, 0, 10},
               Filling -> Axis]")

(view '(Plot (Evaluate (Table (BesselJ n x) [n 4]))
             [x 0 10]
             (-> Filling Axis)))
