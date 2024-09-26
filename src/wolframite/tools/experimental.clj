(ns wolframite.tools.experimental
  "Don't use. Subject to change without prior notice."
  (:require [wolframite.core :as wl]
            [wolframite.impl.jlink-instance :as jlink-instance]
            [wolframite.impl.protocols :as proto])
  (:import [javax.swing JFrame JPanel SwingUtilities]
           [java.awt Dimension]
           [com.wolfram.jlink MathGraphicsJPanel]))

(defonce ^:private app (promise))

(defn- make-app! []
  (JFrame/setDefaultLookAndFeelDecorated true)
  (let [math ^JPanel (MathGraphicsJPanel.)
        frame (doto (JFrame. "Wolframite")
                (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
                (-> .getContentPane (.add math))
                (.setLocation 100 100))]
    {:math math, :frame frame}))

(defn show!
  "Display a graphical Wolfram expression result in a window - such as that of  `Plot[...]`.
  - `wl-expr` - string or Wolframite expression"
  ;; Contrary to tool.graphics, this uses the newer Swing and JLink's own math canvas component
  ([wl-expr] (show! (jlink-instance/get) wl-expr))
  ([jlink-instance wl-expr]
   (when-not (realized? app)
     (deliver app (make-app!)))
   (let [{:keys [math frame]} @app
         wl-expr-str (if (string? wl-expr)
                       wl-expr
                       (str (wl/->wl wl-expr {:jlink-instance jlink-instance})))]
     (doto math
       (.setMinimumSize (Dimension. 200 200)) ; TODO Does not seem to have any effect?! We get a tiny window...
       (.setLink (proto/kernel-link jlink-instance))
       (.setMathCommand wl-expr-str))
     (doto frame
       (.setVisible true)
       ;; TODO The window does not jumpt to the front, despite .toFront
       (.toFront)))))

(comment
  (-> (seq (java.awt.Frame/getFrames)) first (.dispose))
  (show! (jlink-instance/get) "Plot[Sin[x], {x, 0, 6 Pi}]"))
