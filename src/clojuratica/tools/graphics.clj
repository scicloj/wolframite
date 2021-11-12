(ns clojuratica.tools.graphics
  (:require [clojuratica.jlink]
            [clojuratica.core :as wl])
  (:import (com.wolfram.jlink MathCanvas KernelLink)
           (java.awt Color Frame)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io ByteArrayInputStream)
           (java.awt.event WindowAdapter ActionEvent)))

(defn scaled
  [x factor]
  (* x (or factor 1)))

(defn make-math-canvas! [kernel-link & {:keys [scale-factor]}]
  (doto (MathCanvas. kernel-link)
    (.setBounds 25, 25, (scaled 280 scale-factor), (scaled 240 scale-factor))
    (.setUsesFE true)
    (.setImageType MathCanvas/GRAPHICS)))

(defn make-app! [math-canvas & {:keys [scale-factor]}]
  (.evaluateToInputForm @wl/kernel-link-atom (str "Needs[\""  KernelLink/PACKAGE_CONTEXT "\"]") 0)
  (.evaluateToInputForm @wl/kernel-link-atom "ConnectToFrontEnd[]" 0)
  (let [app (Frame.)]
    (doto app
      (.setLayout nil)
      (.setTitle "Wolframite Graphics")
      (.add math-canvas)
      (.setBackground Color/white)
      (.setSize (scaled 300 scale-factor) (scaled 400 scale-factor))
      (.setLocation 50 50)
      (.setVisible true)
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowClosing [^ActionEvent e]
                              (.dispose app)))))))

(defn show!
  [math-canvas wl-form]
  (.setMathCommand math-canvas wl-form))

(comment

  (def canvas (make-math-canvas! wl/kernel-link-atom :scale-factor 1.5))
  (def app (make-app! canvas :scale-factor 1.5))

  (show! canvas "GeoGraphics[]")

  (.dispose app))

  ;; TODO: improve
  ;; - better api (?)
  ;; - accept options
  ;; TODO: patch WL macro adding :show option
  ;; e.g.
  ;;
  ;; (WL :show (GeoGraphics))



(comment ;; fun is good

  (show! canvas "GeoGraphics[]")
  (show! canvas "Graph3D[GridGraph[{3, 3, 3}, VertexLabels -> Automatic]]")
  (show! canvas "GeoImage[Entity[\"City\", {\"NewYork\", \"NewYork\", \"UnitedStates\"}]]")

  )

(comment ;; better quality images
  (import '[com.wolfram.jlink KernelLink])
  (.evaluateToImage wl/kernel-link-atom "GeoGraphics[]" 300 300) ;; this has another arity where you can set `dpi`
  ;; then byte array -> java.awt.Image
  ;; and (.setImage canvas)

  (let [{:keys [height width]} (bean (.getSize app))]
    (prn [width height])
    (.setImage canvas
               (ImageIO/read (ByteArrayInputStream. (.evaluateToImage wl/kernel-link-atom "GeoGraphics[]" (int width) (int height) 600 true)))))

  ;; doesn't make much difference (maybe a bit), seems like we can go lower dpi, but we already get maximum by default (?)

  )
