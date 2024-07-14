(ns dev
  (:require [scicloj.clay.v2.api :as clay]))

;; Render book:
(clay/make! {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           {:part "For Scientists"
                            :chapters ["for_scientists/index.clj"]}
                           {:part "For Developers"
                            :chapters ["for_developers/index.clj"]}]
             :base-target-path "docs"
             :book {:title "Wolframite Documentation"}
             ;; Turn this to true if you wish to
             ;; overwrite the whole target dir:
             :clean-up-target-dir false})
