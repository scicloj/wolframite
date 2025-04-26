(ns wolframite.tools.graphics
  "Displaying WL graphics with Java Swing"
  (:require
   [wolframite.core :as wl]
   [wolframite.impl.jlink-instance :as jlink-instance]
   [wolframite.impl.protocols :as proto])
  (:import
    [com.wolfram.jlink MathGraphicsJPanel]
    [java.awt Component Dimension]
    [javax.swing JFrame JPanel JScrollPane Timer]))

(defonce ^:private default-app (promise))

(defrecord GraphicsWindow [math frame])

(defn- make-app! []
  (JFrame/setDefaultLookAndFeelDecorated true)
  (let [math ^JPanel (MathGraphicsJPanel.) ; sadly, this provides no preferred size ino :'(
        scroll-pane (JScrollPane. math)
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

(defn- fit-graphic-expr-to-frame ^String [^String wl-expr-str ^Component container]
  (let [size (.getSize container)]
    (str "Show[" wl-expr-str ", ImageSize -> {" (.width size) "," (.height size) "}]")))

(defn set-resize-timer [{:keys [^JFrame frame, ^MathGraphicsJPanel math] :as _app} wl-expr-str]
  (let [resize-timer (atom nil)]
   (.addComponentListener
     frame
     (proxy [java.awt.event.ComponentAdapter] []
       (componentResized [_]
         (swap! resize-timer
                (fn start-new-timer [^Timer prev-timer]
                  (some-> prev-timer .stop)
                  (doto (Timer. 200 ; wait 200ms before resizing, to make sure the user is done dragging
                                (proxy [java.awt.event.ActionListener] []
                                  (actionPerformed [_]
                                    (doto math
                                      ;(.setLink kernel-link) ; unnecessary, we've set it already at start
                                      (.setMathCommand (fit-graphic-expr-to-frame wl-expr-str math))
                                      (.revalidate)
                                      (.repaint)))))
                    (.setRepeats false)
                    (.start)))))))))

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
  ([wl-expr] (show! (ensure-default-app!) wl-expr nil))
  ([window wl-expr] (show! window wl-expr nil))
  ([window
    wl-expr
    {:keys [jlink-instance scale-with-window?]
     :or {scale-with-window? true}
     :as _opts}]
   (let [jlink-instance' (or jlink-instance (jlink-instance/get))
         {:keys [math frame] :as app} (or window (make-app!))
         wl-expr-str (if (string? wl-expr)
                       wl-expr
                       (str (wl/->wl wl-expr {:jlink-instance jlink-instance'})))]

     (do
       ;; Essential: this ensures sizes are >= min. size and thus non-zero, which we
       ;; need for the resizing call below
       (.pack frame)
       (doto math
         (.setLink (proto/kernel-link jlink-instance'))
         (.setMathCommand (fit-graphic-expr-to-frame wl-expr-str math))
         (.revalidate)
         (.repaint)))

     (doto frame
       (.setVisible true)
       ;; TODO The window does not jump to the front, despite .toFront
       ;; (depends on win. managers; may only work for windows in the same app...)
       (.toFront))

     (when scale-with-window?
       (set-resize-timer app wl-expr-str))

     app)))

(comment
  (-> (seq (java.awt.Frame/getFrames)) first (.dispose))
  ;; Display in the default window
  (show! "Plot[Sin[x], {x, 0, 6 Pi}]")
  (show! "Plot[Sin[x], {x, 0, 4 Pi}]")
  (show! '(Plot (Sin x) [x 0 (* 6 Math/PI)]))

  (def win (show! "Plot[Sin[x], {x, 0, 2 Pi}]"))
  (show! win "Plot[Cos[x], {x, 0, 2 Pi}]")
  (show! win "Plot[Sin[x], {x, 0, 2 Pi}]")
  (show! nil "Plot[Sin[x], {x, 0, 4 Pi}]")
  )
