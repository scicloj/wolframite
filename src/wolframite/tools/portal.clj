(ns wolframite.tools.portal
  "Integration with https://djblue.github.io/portal/"
  (:require [portal.api :as p]
            [wolframite.tools.hiccup :as h]))

(defn show [form & {:keys [folded?]}]
  (p/submit (with-meta (h/show form {:folded? folded?})
              {:portal.viewer/default :portal.viewer/hiccup})))

(def ^:deprecated view
  "DEPRECATED - for backwards compatibility with Wolframite <= v1.0.1"
  show)
