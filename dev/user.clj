(ns user
  (:require [wolframite.api.v1 :as wl]
            [wolframite.wolfram :as w]
            [wolframite.runtime.system :as system]))

(defn dev
  "Load and switch to the 'dev' namespace."
  []
  (require 'dev)
  (in-ns 'dev)
  :loaded)

(comment
  (wl/start!)
  (wl/stop!)

  (system/info)

  (wl/! (w/Subtract 4 3))
  (wl/! (w/- 4 3))

  (require '[scicloj.clay.v2.api :as clay])
  (clay/make! {:source-path "notebooks/index.clj"}))
