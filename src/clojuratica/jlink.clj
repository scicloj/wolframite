(ns clojuratica.jlink
  (:require [cemerick.pomegranate :as pom]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private unable-to-find-message "Unable to find Mathematica installation. Please specify using either the MATHEMATICA_INSTALL_PATH or WOLFRAM_INSTALL_PATH environment variable.")

(def ^:private default-mac-base-path "/Applications/Mathematica.app/Contents")
(def ^:private default-linux-base-path "/usr/local/Wolfram/Mathematica")
(def ^:private default-windows-base-path "/c:/Program Files/Wolfram Research/Mathematica/")

(def ^:private jlink-suffix "/SystemFiles/Links/JLink/JLink.jar")

(def ^:private mathlink-macos-suffix "/MacOS/MathKernel")
(def ^:private mathlink-linux-suffix "/Executables/MathKernel")
(def ^:private mathlink-windows-suffix "/MathKernel.exe")

(def supported-platform? #{:linux :macos :windows})

(defn platform-id
  "Coerce to a common platform identifier"
  [platform]
  (cond
    (#{:linux "Linux"} platform)         :linux
    (#{:osx :macos "Mac OS X"} platform) :macos
    (or (#{:win :windows} platform) (str/starts-with? platform "Windows")) :windows))

(defn file-path [& path-parts]
  (let [[path-root & path-tail] path-parts]
    (str (.normalize (java.nio.file.Paths/get path-root (into-array String path-tail))))))

(defn- version-vector [version-dir]
  (->  version-dir
       io/file
       (.getName)
       (str/split #"\.")
       (->> (mapv #(Integer/parseInt %)))))

(defn- version-path [base-path]
  (if-let [version-dir
           (->> (io/file base-path)
                (.listFiles)
                (sort-by version-vector)
                (last))]
    version-dir
    (throw (Exception. unable-to-find-message))))

(defn base-path [platform]
  (or (System/getenv "MATHEMATICA_INSTALL_PATH")
      (System/getenv "WOLFRAM_INSTALL_PATH")
      (str (case platform
             :linux   (version-path default-linux-base-path)
             :windows (version-path default-windows-base-path)
             :macos   default-mac-base-path
             (throw (Exception. unable-to-find-message))))))

(defn get-jlink-path
  ([platform]
   (or (System/getenv "JLINK_JAR_PATH")
       (-> platform
           base-path
           (str jlink-suffix)))))

(defn get-mathlink-path
  ([platform]
   (let [base-path (base-path platform)]
     (file-path base-path
                (case platform
                  :macos   mathlink-macos-suffix
                  :linux   mathlink-linux-suffix
                  :windows mathlink-windows-suffix)))))

(defn add-jlink-to-classpath
  ([]
   (add-jlink-to-classpath (platform-id (System/getProperty "os.name"))))
  ([platform]
   (pom/add-classpath (get-jlink-path platform))))

(comment

  (base-path :macos)
  (base-path :linux)

  (get-jlink-path :macos)
  (get-jlink-path :linux)

  (get-mathlink-path :macos)
  (get-mathlink-path :linux)

  )
