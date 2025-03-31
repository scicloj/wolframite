(ns wolframite.tools.hiccup
  (:require [wolframite.core :as wl]
            [scicloj.kindly.v4.kind :as kind]
            [wolframite.impl.jlink-instance :as jlink-instance]
            [wolframite.impl.protocols :as proto])
  (:import (java.util Base64)))

(defn bytes->b64encodedString
  [^bytes bs]
  (String. (.encode (Base64/getEncoder) bs)))

(defn img [b64img]
  (kind/hiccup
   [:img {:src (format "data:image/jpeg;base64,%s" b64img)
          :style {:margin-top "1rem"}}]))

(defn view-graphics-unadorned
  "Render a Wolfram graphics form (e.g. a Plot) as a hiccup `[:img ...]`.

  Useful when you want to have more control over the rendering of the HTML.

  (The Wolfram-zied form text is included in the return value's metadata, as `:wolfram`.)"
  [graphics-form]
  (let [wl-str (wl/->wl graphics-form {:output-fn str})
        input-img    (.evaluateToImage (proto/kernel-link (jlink-instance/get)) wl-str 0 0 0 true)
        b64img (bytes->b64encodedString input-img)]
    (with-meta (img b64img) {:kind/hiccup true
                             :wolfram wl-str})))

(defn view* [graphics-form folded?]
  (let [wl-str (wl/->wl graphics-form {:output-fn str})]
    (kind/hiccup
      [:div
      [:details {:open (not folded?)}
       [:summary [:h5 {:style {:display "inline"
                               :cursor "pointer"
                               :padding "0.5rem 1rem 0 1rem"}}
                  wl-str]]
       [:div.wl-results
        [:hr]
        (view-graphics-unadorned graphics-form)]]])))

(defn view
  "View a given Wolframite `form` as Hiccup, Kindly compatible.
  `:folded true` will fold the view.

  The `form` should be graphics or plotting code, as expected by
  [JLink's `evaluateToImage`](https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/KernelLink.html#evaluateToImage(com.wolfram.jlink.Expr,int,int),
  i.e. it must return a Mathematica Graphics (or Graphics3D, SurfaceGraphics, etc.).

  TIP: To get image data returned in JPEG format instead of GIF, set the Mathematica symbol `JLink\\`$DefaultImageFormat = \"JPEG\"`.

  NOTE: We use base-64 encoding and for larger images, you may be better off using `wl/Export` and then
  including the image as a file.

  See also [[view-graphics-unadorned]]"
  ([graphics-form]
   (view graphics-form nil))
  ([graphics-form {:keys [folded?]}]
   (view* graphics-form folded?)))
