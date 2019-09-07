(defproject Clojuratica "2.0.0-SNAPSHOT"
  :description "An interface between Clojure and Wolfram Mathematica."
  :url "https://github.com/orb/Clojuratica"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 ;; see README for info
                 [com.wolfram.jlink/JLink "4.9.1"]]

  ;; 参考两年前项目: https://github.com/chanshunli/clj-jri
  :jvm-opts [~(str "-Djava.library.path=/Applications/Mathematica.app/Contents/SystemFiles/Links/JLink:"
                   (System/getProperty "java.library.path"))]

  :resource-paths ["/Applications/Mathematica.app/Contents/SystemFiles/Links/JLink/JLink.jar"]
  
  :profiles {:dev {:source-paths ["dev"]}
             :osx {:dependencies
                   [[com.wolfram.jlink/JLink-native "4.4" :classifier native-osx]]}}
  :plugins [[lein-localrepo "0.5.4"]])
