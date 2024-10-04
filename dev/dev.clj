(ns dev
  (:refer-clojure
   ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.core :as wl]
   [wolframite.wolfram :as w]
   [scicloj.kindly.v4.kind :as k]
   [babashka.fs :as fs]))

(comment (wl/start!))

(def path--video "resources/video/attention-test--higher-res.webm")

(defn make-frames!
  "Like `export-frames!`, but where the frames are kept on the Wolfram side and not passed to the Clojure REPL."
  [path--video]
  (-> path--video
      w/Video
      (w/VideoTrim [6 7])
      ;; (w/VideoFrameList 50)
        ;; (->> (w/= 'thing))

      wl/eval))

(-> "./resources/video/Baby.mp4"
    w/Video
    (w/VideoTrim [6 7])
    (->> (w/= 'thing))
    wl/eval)

(-> ;; (w/Clear 'thing)
 'thing
 wl/eval)

(Video "/home/ctw/Documents/Wolfram/Video/VideoTrim-2024-10-02T14-30-24.mp4" (-> Appearance Automatic) (-> AudioOutputDevice Automatic) (-> AudioTrackSelection [1]) (-> PlaybackSettings {}) (-> SoundVolume Automatic) (-> SubtitleTrackSelection []) (-> VideoTrackSelection [1]))

(k/video {:src "/home/ctw/Documents/Wolfram/Video/VideoTrim-2024-10-02T12-37-00.mp4"})

(comment ;; Get Started!

  ;;
  ;; Evaluation
  ;;

  ;; Use eval with a quoted Wolfram-as-clojure-data expression (`Fn[..]` becoming `(Fn ..)`):
  (wl/eval '(Dot [1 2 3] [4 5 6])) ; Wolfram: `Dot[{1, 2, 3}, {4, 5, 6}]]`
  (wl/eval (Dot [1 2 3] [4 5 6])) ; We have loaded all symbols as Clojure fns and thus can run this directly

  (wl/eval '(N Pi 20))
  (wl/eval (N 'Pi 20)) ; Beware: Pi must still be quoted, it is not a fn

  #_:end-comment)
