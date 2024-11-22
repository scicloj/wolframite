(ns user
  (:require [wolframite.api.v1 :as wl]
            [wolframite.wolfram :as w]))

(defn dev
  "Load and switch to the 'dev' namespace."
  []
  (require 'dev)
  (in-ns 'dev)
  :loaded)

(comment
  (wl/init)
  (wl/! (w/Subtract 4 3))

  (require '[scicloj.clay.v2.api :as clay])
  (clay/make! {:source-path "notebooks/index.clj"}))
