(defproject Clojuratica "2.0.0-SNAPSHOT"
  :description "An interface between Clojure and Wolfram Mathematica."
  :url "https://github.com/orb/Clojuratica"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;; see README for info
                 [com.wolfram.jlink/JLink "4.4"]]

  :profiles {:dev {:source-paths ["dev"]}
             :osx {:dependencies
                   [[com.wolfram.jlink/JLink-native "4.4" :classifier native-osx]]}})


