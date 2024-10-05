(ns wolframite.tools.hiccup
  (:require
    [scicloj.kindly.v4.kind :as kind]
    [wolframite.core :as wl]
    [wolframite.impl.jlink-instance :as jlink-instance]
    [wolframite.impl.protocols :as proto])
  (:import
    (java.util Base64)))

(defn bytes->b64encodedString
  [^bytes bs]
  (String. (.encode (Base64/getEncoder) bs)))

(defn img [b64img]
  (kind/hiccup
    [:img {:src (format "data:image/jpeg;base64,%s" b64img)
           :style {:margin-top "1rem"}}]))

(defn view* [form folded?]
  (let [wl-str (wl/->wl form {:output-fn str})
        input-img    (.evaluateToImage (proto/kernel-link (jlink-instance/get)) wl-str 0 0 0 true)
        b64img (bytes->b64encodedString input-img)]
    (kind/hiccup
      [:div
       [:details {:open (not folded?)}
        [:summary [:h5 {:style {:display "inline"
                                :cursor "pointer"
                                :padding "0.5rem 1rem 0 1rem"}}
                   wl-str]]
        [:div.wl-results
         [:hr]
         (img b64img)]]])))

(defn view
  "View a given Wolframite `form` as Hiccup, Kindly compatible.
  `:folded true` will fold the view.

  NOTE: We use base-64 encoding and for larger images, you may be better off using `wl/Export` and then
  including the image as a file."
  ([form]
   (view form nil))
  ([form {:keys [folded?]}]
   (-> form
       (view* folded?)
       kind/hiccup)))
