(ns clojuratica.tools.clerk-helper
  (:require [clojuratica.core :as wl]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.webserver :as webserver]
            [nextjournal.beholder :as beholder]
            [clj-http.client        :as client]
            [clojure.java.io :as io]))

(defn bytes->b64encodedString
  [bs]
  (java.lang.String. (.encode (java.util.Base64/getEncoder) bs)))

(defn img [b64img]
  [:img {:src (format "data:image/jpeg;base64,%s" b64img)
         :style {:margin-top "1rem"}}])

(defn view* [form]
  (let [wl-str (wl/->wl! form {:output-fn str})
        input-img    (.evaluateToImage @wl/kernel-link-atom wl-str 0 0 0 true)
        b64img (bytes->b64encodedString input-img)]
    [:div
     [:details {:open true}
      [:summary [:h5 {:style {:display "inline-block"
                              :cursor "pointer"
                              :padding "0.5rem 1rem 0 1rem"}}
                 wl-str]]
      [:div.wl-results
       [:hr]
       (img b64img)]]]))

(defn view [form]
  (clerk/html (view* form)))

(defn stream->bytes [is]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (io/copy is baos)
    (.toByteArray baos)))

(defn fetch-image [url]
  (:body (client/get url {:as :stream})))

(defn get-image [url]
  (->> url
       fetch-image
       stream->bytes
       bytes->b64encodedString
       img))

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
