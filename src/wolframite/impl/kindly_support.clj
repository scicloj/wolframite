(ns wolframite.impl.kindly-support
  "Add Kindly annotations to eval results to improve their display in compatible tools, such as Clay"
  (:import (clojure.lang IObj)))

(defn- head [expr]
  (when (list? expr)
    (first expr)))

(defn- video->url
  "Extract the url from `(Video \"url..\" ...)`"
  [expr]
  {:pre [(= (head expr) 'Video)]}
  (second expr))

(defn maybe-add-kindly-meta [expr]
  {:pre [expr]}
  (if (instance? IObj expr)
   (->> (cond
          (= (head expr) 'Video)
          {:kind/video true
           :kindly/options {:kindly/f video->url}})
        (with-meta expr))
   expr))
