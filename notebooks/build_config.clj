(ns build-config
  "Clay build configuration, used by ../build.clj script")

(def config {:format [:quarto :html]
             :base-source-path "notebooks"
             :source-path ["index.clj"
                           "quickstart.clj"
                           "understanding_wolframite.clj"
                           "gotchas.clj"
                           {:part "Tutorials for scientists"
                            :chapters ["for_scientists/index.clj"
                                       "for_scientists/cavity_physics.clj"]}
                           {:part "Tutorials for developers"
                            :chapters ["for_developers/index.clj"
                                       "for_developers/wolfram_for_clojurians.clj"
                                       "for_developers/demo_analysis_cycling.clj"]}
                           {:part "Clojure primer"
                            :chapters ["clojure_primer/index.clj"]}
                           {:part "Advanced topics"
                            :chapters ["advanced/customizing_wolframite.clj"
                                       "advanced/packages.clj"]}
                           "faq.clj"]
             :base-target-path "docs"
             :book {:title "Wolframite Documentation"}
             ;; Turn this to true if you wish to
             ;; overwrite the whole target dir:
             :clean-up-target-dir false})

;; Render book:
(comment
  ((requiring-resolve 'scicloj.clay.v2.api/make!) config))
