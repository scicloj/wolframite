(ns clojuratica.tools.portal
  "Integration with https://djblue.github.io/portal/"
  (:require [portal.api :as p]
            [clojuratica.tools.hiccup :as h]))

(defn view [form & {:keys [folded?]}]
  (p/submit (with-meta (h/view* form folded?)
              {:portal.viewer/default :portal.viewer/hiccup})))
