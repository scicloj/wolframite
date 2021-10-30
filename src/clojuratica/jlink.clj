(ns clojuratica.jlink
  (:require [cemerick.pomegranate :as pom]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(def ^:private unable-to-find-message "Unable to find Mathematica installation. Please specify JLink jar path using the JLINK_JAR environment variable.")

(defn- version-vector [version-dir]
  (-> (.getName version-dir)
      (string/split #"\.")
      (->> (mapv #(Integer/parseInt %)))))

(defn- linux-version-path [base-path]
  (if-let [version-dir
           (->> (io/file base-path)
                (.listFiles)
                (sort-by version-vector)
                (last))]
    version-dir
    (throw (Exception. unable-to-find-message)))) 

(def ^:private default-mac-base-path "/Applications/Mathematica.app/Contents/")
(def ^:private default-linux-base-path "/usr/local/Wolfram/Mathematica/")

(defn- get-jlink-path
  ([base-path]
   (str base-path "/SystemFiles/Links/JLink/JLink.jar"))
  ([]
   (or (System/getenv "JLINK_JAR")
       (let [os (System/getProperty "os.name")]
         (case os
           "Linux" (get-jlink-path (linux-version-path default-linux-base-path))
           "Mac OS X" (get-jlink-path default-mac-base-path)
           :else (throw (Exception. unable-to-find-message)))))))


;; Add the jar to the classpath
(pom/add-classpath (get-jlink-path))
  

