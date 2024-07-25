(ns user)

(defn dev
  "Load and switch to the 'dev' namespace."
  []
  (require 'dev)
  (in-ns 'dev)
  :loaded)

(comment
  (require '[scicloj.clay.v2.api :as clay])
  (clay/make! {:source-path "notebooks/index.clj"}))
