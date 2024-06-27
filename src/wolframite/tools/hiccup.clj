(ns wolframite.tools.hiccup
  (:require [wolframite.core :as wl]
            [scicloj.kindly.v4.kind :as kind]))

(defn bytes->b64encodedString
  [bs]
  (java.lang.String. (.encode (java.util.Base64/getEncoder) bs)))

(defn img [b64img]
  (kind/hiccup
   [:img {:src (format "data:image/jpeg;base64,%s" b64img)
          :style {:margin-top "1rem"}}]))

(defn view* [form folded?]
  (let [wl-str (wl/->wl form {:output-fn str})
        input-img    (.evaluateToImage @wl/kernel-link-atom wl-str 0 0 0 true)
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
  `:folded true` will fold the view."
  ([form]
   (view form nil))
  ([form {:keys [folded?]}]
   (-> form
       (view* folded?)
       kind/hiccup)))
