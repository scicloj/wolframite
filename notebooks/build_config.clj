(ns build-config
 "Clay build configuration, used by ../build.clj script")

(def config {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           "demo.clj"
                           {:part "For Scientists"
                            :chapters ["for_scientists/index.clj"]}
                           {:part "For Developers"
                            :chapters ["for_developers/index.clj"]}]
             :base-target-path "docs"
             :book {:title "Wolframite Documentation"}
             ;; Turn this to true if you wish to
             ;; overwrite the whole target dir:
             :clean-up-target-dir false})

;; Render book:
(comment
 ((requiring-resolve 'scicloj.clay.v2.api/make!) config))
