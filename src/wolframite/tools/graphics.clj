(ns wolframite.tools.graphics
  "Displaying WL graphics with Java Swing"
  (:require
   [wolframite.core :as wl]
   [wolframite.impl.jlink-instance :as jlink-instance]
   [wolframite.impl.protocols :as proto])
  (:import
   [com.wolfram.jlink MathGraphicsJPanel]
   [java.awt Dimension]
   [javax.swing JFrame JPanel Timer]))

(defonce ^:private default-app (promise))

(defrecord GraphicsWindow [math frame])

(defn- make-app! []
  (JFrame/setDefaultLookAndFeelDecorated true)
  (let [math ^JPanel (MathGraphicsJPanel.)
        scroll-pane (javax.swing.JScrollPane. math)
        frame (doto (JFrame. "Wolframite")
                (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
                (-> .getContentPane (.add scroll-pane "Center"))
                (.setLocation 100 100)
                (.setMinimumSize (Dimension. 300 300)))]
    (->GraphicsWindow math frame)))

(defn ensure-default-app! []
  (when-not (realized? default-app)
    (deliver default-app (make-app!)))
  @default-app)

(defn show!
  "Display a graphical Wolfram expression result in a window - such as that of  `Plot[...]`.
  - `window` - pass `nil` to visualize the expression in a new window, or pass in the return value
               from a previous call to show it in that same window. The former is useful e.g. if
               you want to display multiple plots at the same time for comparison.
  - `wl-expr` - Wolfram in a string or a Wolframite expression

  NOTE: It can take few seconds to prepare and render the graphics.

  You can dispose of the window by closing it.

  NOTE: The 1-arg version uses a single default window for all displays.

  Returns a 'window' thing representing the window displaying the expression."
  ;; NOTE: Contrary to the legacy graphics, this uses the newer Swing and JLink's own math canvas component
  ;; TODO Sometimes, the graphics is scaled to the frame, sometimes not; why/when?!
  ;; TODO: When we've multiple windows, should we try to position them not all at the same place?!
  ;; TODO: consistent call signature
  ([wl-expr] (show! (ensure-default-app!) (jlink-instance/get) wl-expr {}))
  ([window wl-expr] (show! window (jlink-instance/get) wl-expr {}))
  ([window wl-expr opts] (show! window (jlink-instance/get) wl-expr opts))
  ([window jlink-instance wl-expr {:keys [scale-with-window?] :or {scale-with-window? true}}]

   (let [{:keys [math frame] :as app} (or window (make-app!))
         wl-expr-str (if (string? wl-expr)
                       wl-expr
                       (str (wl/->wl wl-expr {:jlink-instance jlink-instance})))]
     (doto math
       (.setLink (proto/kernel-link jlink-instance))
       (.setMathCommand wl-expr-str)
       (.revalidate)
       (.repaint))

     (doto frame
       ;(.pack) ; we'd want this, if the MathCanvas had any size info :'(
       (.setVisible true)
       ;; TODO The window does not jump to the front, despite .toFront
       ;; (depends on win. managers; may only work for windows in the same app...)
       (.toFront))

     (let [resize-timer (atom nil)
           kernel-link (proto/kernel-link jlink-instance)
           base-expr wl-expr]

       (when scale-with-window?
         (.addComponentListener
          frame
          (proxy [java.awt.event.ComponentAdapter] []
            (componentResized [_]
              (when-let [t @resize-timer]
                (.stop t))
              (reset! resize-timer
                      (doto (Timer. 200
                                    (proxy [java.awt.event.ActionListener] []
                                      (actionPerformed [_]
                                        (let [size (.getSize frame)
                                              w (.width size)
                                              h (.height size)
                                              resized-expr (str "Show[" base-expr ", ImageSize -> {" w "," h "}]")]
                                          (doto math
                                            (.setLink kernel-link)
                                            (.setMathCommand (str resized-expr))
                                            (.revalidate)
                                            (.repaint))))))
                        (.setRepeats false)
                        (.start)))))))

       app))))

(comment
  (-> (seq (java.awt.Frame/getFrames)) first (.dispose))
  ;; Display in the default window
  (show! "Plot[Sin[x], {x, 0, 6 Pi}]")
  (show! "Plot[Sin[x], {x, 0, 4 Pi}]")
  (show! '(Plot (Sin x) [x 0 (* 6 Math/PI)]))

  (def win (show! "Plot[Sin[x], {x, 0, 2 Pi}]"))
  (show! win "Plot[Cos[x], {x, 0, 2 Pi}]")
  (show! win "Plot[Sin[x], {x, 0, 2 Pi}]")
  (show! nil "Plot[Sin[x], {x, 0, 4 Pi}]"))
