(ns wolframite.impl.kindly-support
  "Add Kindly annotations to eval results to improve their display in compatible tools, such as Clay"
  ;; NOTE We do not require Kindly APIs and only use keyword metadata b/c we don't want Wolframite itself to depend
  ;;      on the kindly libs (contrary to our build profile, which does so to be able to build docs)
  (:import (clojure.lang IObj)))

;; # Clay video test
;; (Uncomment the eval line below & render this file with Clay to verify it works.
;; Make sure ./docs/Baby.mp4 exists first.)
;; (wolframite.core/eval '(Video "./Baby.mp4"))

(defn- head [expr]
  (when (list? expr)
    (first expr)))

(defn- video->url
  "Extract the url from `(Video \"url..\" ...)`"
  [expr]
  {:pre [(= (head expr) 'Video)]}
  (second expr))

(defn ->video-kind [expr]
  ;; BEWARE: The path should be *relative* w.r.t. a folder on Clay's path, such as ./docs
  ;; (see clay.edn and its :base-target-path and :subdirs-to-sync, and
  ;; https://scicloj.github.io/clay/#referring-to-files)
  ^:kind/video {:src (video->url expr)})

(defn maybe-add-kindly-meta [expr]
  {:pre [expr]}
  (if (instance? IObj expr)
    (->> (cond
           (= (head expr) 'Video)
           {:kindly/kind :kind/fn
            :kindly/options {:kindly/f ->video-kind}})
         (with-meta expr))
    expr))
