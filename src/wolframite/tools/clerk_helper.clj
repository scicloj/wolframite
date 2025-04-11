(ns wolframite.tools.clerk-helper
  (:require [wolframite.core :as wl]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.webserver :as webserver]
            [nextjournal.beholder :as beholder]
            ;; [clj-http.client        :as client]
            [clojure.java.io :as io]
            [wolframite.tools.hiccup :as h]))

(defn show [form & {:keys [folded?]}]
  (clerk/html (h/view* form folded?)))

(defn stream->bytes [is]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (io/copy is baos)
    (.toByteArray baos)))

;; (defn fetch-image [url]
;;   (:body (client/get url {:as :stream})))

;; (defn get-image [url]
;;   (->> url
;;        fetch-image
;;        stream->bytes
;;        bytes->b64encodedString
;;        img))

;; There are some issues with the classpath,
;; (likely) due to dynamic loading of jlink,
;; so this is a hacky way to watch paths
(defn clerk-watch!
  [watch-paths]
  (webserver/start! {:port 7777})
  (future
    (reset! clerk/!watcher {:paths watch-paths
                            :watcher (apply beholder/watch-blocking #(clerk/file-event %) watch-paths)}))
  (prn "Clerk Started!"))
