(defproject Clojuratica "2.0.0-SNAPSHOT"
  :description "An interface between Clojure and Wolfram Mathematica."
  :url "https://github.com/orb/Clojuratica"
  :license {:name "Mozilla Public License"
            :url "http://www.mozilla.org/MPL/2.0/index.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;; see README for info
                 [com.wolfram.jlink/JLink "4.4"]]
  :profiles {:dev {:source-paths ["dev"]}})


