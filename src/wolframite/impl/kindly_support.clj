(ns wolframite.impl.kindly-support
  "Add Kindly annotations to eval results to improve their display in compatible tools, such as Clay"
  (:require [scicloj.kindly.v4.kind :as k]
            [scicloj.kindly.v4.api])
  (:import (clojure.lang IObj)))

(defn- head [expr]
  (when (list? expr)
    (first expr)))

(defn- video->url
  "Extract the url from `(Video \"url..\" ...)`"
  [expr]
  {:pre [(= (head expr) 'Video)]}
  (second expr))

;; (defn maybe-add-kindly-meta [expr]
;;   {:pre [expr]}
;;   (if (instance? IObj expr)
;;     (->> (cond
;;            (= (head expr) 'Video)
;;            {:kind/video true
;;             :kindly/options {:kindly/f video->url}})
;;          (with-meta expr))
;;     expr))
;;
(defn- kind<-Video
  "Takes a Wolfram-like `(video url ...)` expression and returns an appropriate video kind.

  NOTE: Take care with the capital letter. This is to distinguish a Wolfram `Video` from other types."
  [expr]
  (k/video {:src (second expr)}))

(defn maybe-add-kindly-meta [expr]
  {:pre [expr]}
  (if (instance? IObj expr)
    (cond
      (= (head expr) 'Video)
      (k/fn expr
        {:kindly/f kind<-Video}))

    expr))

(comment
  (def vid
    '(Video "./resources/video/Baby.mp4" (-> Appearance Automatic) (-> AudioOutputDevice Automatic) (-> AudioTrackSelection [1]) (-> PlaybackSettings {}) (-> SoundVolume Automatic) (-> SubtitleTrackSelection []) (-> VideoTrackSelection [1])))

  (def vid--VideoTrim-output
    '(Video "/home/ctw/Documents/Wolfram/Video/VideoTrim-2024-10-02T14-14-20.mp4" (-> Appearance Automatic) (-> AudioOutputDevice Automatic) (-> AudioTrackSelection [1]) (-> PlaybackSettings {}) (-> SoundVolume Automatic) (-> SubtitleTrackSelection []) (-> VideoTrackSelection [1])))

  ;; Both (below) work here...
  (maybe-add-kindly-meta vid)
  (maybe-add-kindly-meta vid--VideoTrim-output))
