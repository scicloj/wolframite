(ns graphics
  (:require [clojuratica.init :as init])
  (:import (com.wolfram.jlink MathCanvas KernelLink)
           (java.awt Color Frame)
           (java.awt.event WindowAdapter ActionEvent)))

(defn make-math-canvas! [kernel-link]
  (doto (MathCanvas. kernel-link)
    (.setBounds 10, 25, 280, 240)
    (.setImageType MathCanvas/GRAPHICS)))

(defn make-app! [math-canvas]
  (.evaluateToInputForm init/kernel-link (str "Needs[\""  KernelLink/PACKAGE_CONTEXT "\"]") 0)
  (.evaluateToInputForm init/kernel-link "ConnectToFrontEnd[]" 0)
  (let [app (Frame.)]
    (doto app
      (.setLayout nil)
      (.setTitle "Wolframite Graphics")
      (.add math-canvas)
      (.setBackground Color/white)
      (.setSize 300 400)
      (.setLocation 50 50)
      (.setVisible true)
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowClosing [^ActionEvent e]
                              (.dispose app)))))))

(defn show!
  [math-canvas wl-form]
  (.setMathCommand math-canvas wl-form))

(comment

  (def canvas (make-math-canvas! init/kernel-link))
  (def app (make-app! canvas))

  (show! canvas "GeoGraphics[]")

  (.dispose app)

  ;; TODO: improve
  ;; - better api (?)
  ;; - accept options
  ;; TODO: patch WL macro adding :show option
  ;; e.g.
  ;;
  ;; (WL :show (GeoGraphics))

  )

(comment ;; fun is good

  (show! canvas "GeoGraphics[]")
  (show! canvas "Graph3D[GridGraph[{3, 3, 3}, VertexLabels -> Automatic]]")
  (show! canvas "GeoImage[Entity[\"City\", {\"NewYork\", \"NewYork\", \"UnitedStates\"}]]")

  )
