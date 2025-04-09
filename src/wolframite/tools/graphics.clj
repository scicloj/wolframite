(ns wolframite.tools.graphics
  "Displaying WL graphics with Java Swing"
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
                (.setLocation 100 100)
                (.setMinimumSize (Dimension. 300 300)))]
    {:math math, :frame frame}))

(defn show!
  "Display a graphical Wolfram expression result in a window - such as that of  `Plot[...]`.
  - `wl-expr` - string or Wolframite expression"
  ;; Contrary to tool.graphics, this uses the newer Swing and JLink's own math canvas component
  ;; TODO Sometimes, the graphics is scaled to the frame, sometimes not; why/when?!
  ([wl-expr] (show! (jlink-instance/get) wl-expr))
  ([jlink-instance wl-expr]
   (when-not (realized? app)
     (deliver app (make-app!)))
   (let [{:keys [math frame]} @app
         wl-expr-str (if (string? wl-expr)
                       wl-expr
                       (str (wl/->wl wl-expr {:jlink-instance jlink-instance})))]
     (doto math
       (.setLink (proto/kernel-link jlink-instance))
       (.setMathCommand wl-expr-str))
     (doto frame
       ;(.pack) ; we'd want this, if the MathCanvas had any size info :'(
       (.setVisible true)
       ;; TODO The window does not jump to the front, despite .toFront
       ;; (depends on win. managers; may only work for windows in the same app...)
       (.toFront)))))

(comment
  (-> (seq (java.awt.Frame/getFrames)) first (.dispose))
  (show! (jlink-instance/get) "Plot[Sin[x], {x, 0, 6 Pi}]")
  (show! (jlink-instance/get) "Plot[Sin[x], {x, 0, 4 Pi}]"))
