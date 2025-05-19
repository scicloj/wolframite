(ns wolframite.tools.graphics
  "Displaying WL graphics with Java Swing"
  (:require
    [clojure.java.io :as io]
    [wolframite.core :as wl]
    [wolframite.impl.jlink-instance :as jlink-instance]
    [wolframite.impl.protocols :as proto])
  (:import
    [com.wolfram.jlink MathGraphicsJPanel]
    [java.awt Component Dimension Image]
    (java.awt.event ActionListener ComponentAdapter MouseAdapter)
    (java.awt.image BufferedImage)
    (javax.imageio ImageIO)
    [javax.swing JFileChooser JFrame JMenuItem JOptionPane JPanel JPopupMenu JScrollPane JViewport SwingUtilities Timer]))

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

; It seems that `Show[..]` has no effect on the resulting size, contrary to math.setSize
;(defn- fit-graphic-expr-to-frame ^String [^String wl-expr-str ^Dimension size]
;  (let [scaled-expr
;        ;; With ImageSize->{w,h}, an object will always be drawn in a w×h region and will be sized to fit in the region.
;        ;; If the object has a definite aspect ratio that is not h/w, then space will be left around it.
;        (format "With[{g=%s}, If[MatchQ[g, _Graphics | _Graphics3D],Show[g,ImageSize -> {%d,%d}],g]]"
;                wl-expr-str
;                (- (.width size) 10)
;                (- (.height size) 10))]
;    scaled-expr))

(defn- available-math-size [^JFrame _frame, ^Component math]
  ;; BEWARE: In `componentResized`, it is likely that only the _frame_ & ScrollPane have been resized, not the math
  (let [scroll-pane-viewport ^JViewport (.getParent math)]
    (.getSize scroll-pane-viewport)))

(defn- ensure-math-sized-to-frame!
  ([math frame]
   (let [new-math-size (available-math-size frame math)]
     (doto math
       ;; The next two are essential to resizing, in this particular order:
       (.setSize new-math-size) ; this makes the content fit the new size
       (.setMathCommand (.getMathCommand math)) ; seems pointless but it is necessary :'(
       ;; And some supporting code
       ;; We need to adjust the preferred size so that next time we call show! and thus
       ;; frame.pack, we won't reset to the default preferred size but keep the size
       ;; we've
       (.setPreferredSize new-math-size)
       ;; Not 100% sure we need all the following ones:
       (.revalidate)
       (.repaint)))))

(defn set-resize-timer [{:keys [^JFrame frame, ^MathGraphicsJPanel math] :as _app} wl-expr-str]
  (doseq [l (.getComponentListeners frame)]
    ;; Heavy-handed, not trying to find _this_ listener, just remove them all; there is surely a better way...
    (.removeComponentListener frame l))

  (let [resize-timer (atom nil)]
   (.addComponentListener
     frame
     (proxy [ComponentAdapter] []
       (componentResized [_]
         (swap! resize-timer
                (fn start-new-timer [^Timer prev-timer]
                  (some-> prev-timer .stop)
                  (doto (Timer. 200 ; wait 200ms before resizing, to make sure the user is done dragging
                                (proxy [ActionListener] []
                                  (actionPerformed [_] (ensure-math-sized-to-frame! math frame))))
                    (.setRepeats false)
                    (.start)))))))))

(defn add-save-menu [{:keys [^JFrame frame, ^MathGraphicsJPanel math] :as _app}]
  (doseq [l (.getMouseListeners math)]
    ;; Heavy-handed, not trying to find _this_ listener, just remove them all; there is surely a better way..
    (.removeMouseListener frame l))

  (let [popup (JPopupMenu.)
        save-item (JMenuItem. "Save Image...")
        file-chooser (JFileChooser.)]

    (.addActionListener
      save-item
      (proxy [ActionListener] []
        (actionPerformed [_]
          (try
            (let [result (.showSaveDialog file-chooser frame)]
              (when (= result JFileChooser/APPROVE_OPTION)
                (let [file (.getSelectedFile file-chooser)
                      file-path (str file)
                      png-file (if (.endsWith file-path ".png")
                                 file
                                 (io/file (str file-path ".png")))
                      img ^Image (.getImage math)]
                  (if img
                    (let [w (.getWidth img nil)
                          h (.getHeight img nil)
                          buffered-img (BufferedImage. w h BufferedImage/TYPE_INT_ARGB)
                          g (.createGraphics buffered-img)]
                      (.drawImage g img 0 0 nil)
                      (.dispose g)
                      (ImageIO/write buffered-img "png" png-file))
                    (JOptionPane/showMessageDialog
                      frame
                      "No image available to save."
                      "Save Error"
                      JOptionPane/ERROR_MESSAGE)))))
            (catch Exception e
              (JOptionPane/showMessageDialog
                frame
                (str "Failed to save image: " (.getMessage e))
                "Save Error"
                JOptionPane/ERROR_MESSAGE))))))

    (.add popup save-item)

    (.addMouseListener
      math
      (proxy [MouseAdapter] []
        (mousePressed [e]
          (when (.isPopupTrigger e)
            (.show popup math (.getX e) (.getY e))))
        (mouseReleased [e]
          (when (.isPopupTrigger e)
            (.show popup math (.getX e) (.getY e))))))))

(defn- show-on-swing-thread [wl-expr-str {:keys [math frame] :as app} {:keys [jlink-instance scale-with-window?] :as _opts}]
  (do
    ;; Essential: this ensures sizes are >= min. size and thus non-zero, which we
    ;; need for the size-related call(s) below
    (.pack frame)
    (doto math
      (.setLink (proto/kernel-link jlink-instance))
      (.setMathCommand wl-expr-str #_(fit-graphic-expr-to-frame wl-expr-str (available-math-size frame math)))
      (ensure-math-sized-to-frame! frame)
      (.setFocusable true)
      (.requestFocusInWindow)))

  (doto frame
    (.setVisible true)
    ;; TODO The window does not jump to the front, despite .toFront
    ;; (depends on win. managers; may only work for windows in the same app...)
    (.toFront))

  (when scale-with-window?
    (set-resize-timer app wl-expr-str))

  (add-save-menu app))

(defn show!
  "Display a graphical Wolfram expression result in a window - such as that of  `Plot[...]`.
  - `wl-expr` - Wolfram in a string or a Wolframite expression
  - `window` - pass `nil` to visualize the expression in a new window, or pass in the return value
               from a previous call to show it in that same window. The former is useful e.g. if
               you want to display multiple plots at the same time for comparison.
  - `_opts` - options map:
    - `:scale-with-window?` - whether to scale the graphics when you resize the window

  NOTE: It can take few seconds to prepare and render the graphics.

  You can dispose of the window by closing it.

  NOTE: The 1-arg version uses a single default window for all displays.

  Returns a 'window' thing representing the window displaying the expression."
  ;; NOTE: Contrary to the legacy graphics, this uses the newer Swing and JLink's own math canvas component
  ;; TODO: When we've multiple windows, should we try to position them not all at the same place?!
  ([wl-expr] (show! wl-expr (ensure-default-app!) nil))
  ([wl-expr window] (show! wl-expr window nil))
  ([wl-expr
    window
    {:keys [jlink-instance scale-with-window?]
     :or {scale-with-window? true}
     :as _opts}]
   (let [jlink-instance' (or jlink-instance (jlink-instance/get))
         app (or window (make-app!))
         wl-expr-str (if (string? wl-expr)
                       wl-expr
                       (str (wl/->wl wl-expr {:jlink-instance jlink-instance'})))]
     (SwingUtilities/invokeAndWait
       ;; Swing is not thread-safe and changes to its components must be done on the Event Dispatch Thread (EDT)
       (fn []
         (show-on-swing-thread wl-expr-str app {:jlink-instance jlink-instance' :scale-with-window? scale-with-window?})))

     app)))

(comment
  (-> (seq (java.awt.Frame/getFrames)) first (.dispose))

  (-> @default-app :math ) ; 564x654
  ;; Display in the default window
  (show! "Plot[Sin[x], {x, 0, 6 Pi}]")
  (show! "Plot[Sin[x], {x, 0, 4 Pi}]")
  (show! '(Plot (Sin x) [x 0 (* 6 Math/PI)]))

  (def win (show! "Plot[Sin[x], {x, 0, 2 Pi}]"))
  (show! "Plot[Cos[x], {x, 0, 2 Pi}]" win)
  (show! "Plot[Sin[x], {x, 0, 2 Pi}]" win)
  (show! "Plot[Sin[x], {x, 0, 4 Pi}]" nil)

  (apply available-math-size ((juxt :frame :math) @default-app))

  (-> @default-app :math .repaint)
  (-> @default-app :math .revalidate)
  (-> @default-app :frame .getInsets)
  (-> @default-app :math .getParent .getInsets)
  ;; NOTE: Code changing components must be run on the Swing thread:
  (SwingUtilities/invokeAndWait (fn [] (-> @default-app :frame .revalidate)))
  (SwingUtilities/invokeAndWait (fn [] (-> @default-app :frame .repaint)))
  (SwingUtilities/invokeAndWait (fn []
                                  (let [{:keys [frame math]} @default-app]
                                   (ensure-math-sized-to-frame! math frame))))
  )
