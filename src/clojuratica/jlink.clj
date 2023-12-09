(ns clojuratica.jlink
  "ATTENTION! This namespace is side effecting, and is required for many of the files in this project to compile.
  It uses Pomegranate to dynamically add the Wolfram Language / Mathematica JLink jar to the JVM classpath.
  Because many of the namespaces in this project either import or reference the jlink classes, it's necessary to have loaded this namespace before those namespaces will compile.
  ;Thus, you'll this library required but inused across the codebase.
  This is to get around that fact that we don't have the jar available to us through a standard maven repository, and can't use environment variables in our `deps.edn` specifications."
  (:require [cemerick.pomegranate :as pom]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private ^String unable-to-find-message
  ;; TODO (jh) print current values of the install paths
  "Unable to find Mathematica or Wolfram Engine installation. Please specify using either the MATHEMATICA_INSTALL_PATH or WOLFRAM_INSTALL_PATH environment variable.")

(def ^:private default-mac-base-path "/Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents")
(def ^:private mathematica-mac-base-path "/Applications/Mathematica.app/Contents")
(def ^:private wolfram-linux-base-path "/opt/WolframEngine") ; it has SystemFiles/Links/JLink/JLink.jar; *might* have WolframEngine/<version>
(def ^:private default-linux-base-path "/usr/local/Wolfram/Mathematica")
(def ^:private default-windows-base-path "/c:/Program Files/Wolfram Research/Mathematica/")

(def ^:private jlink-suffix "/SystemFiles/Links/JLink/JLink.jar")

(def ^:private mathlink-macos-suffix "/MacOS/WolframKernel")
(def ^:private mathematica-mathlink-macos-suffix "/MacOS/MathKernel")
(def ^:private mathlink-linux-suffix "/Executables/MathKernel")
(def ^:private mathlink-windows-suffix "/MathKernel.exe")

(def supported-platform? #{:linux :macos :windows :macos-mathematica})

(defn platform-id
  "Coerce to a common platform identifier"
  [platform]
  (cond
    (#{:linux "Linux"} platform)         :linux
    (#{:osx :macos "Mac OS X"} platform) :macos
    (or (#{:win :windows} platform) (str/starts-with? platform "Windows")) :windows
    :else platform))

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
  (or (System/getenv "MATHEMATICA_INSTALL_PATH") ; TODO (jh) support alternatively Java system properties?
      (System/getenv "WOLFRAM_INSTALL_PATH")
      (str (case platform
             :linux   (version-path default-linux-base-path)
             :windows (version-path default-windows-base-path)
             :macos   default-mac-base-path
             :macos-mathematica mathematica-mac-base-path
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
                  :windows mathlink-windows-suffix
                  :macos-mathematica mathematica-mathlink-macos-suffix)))))

(defn add-jlink-to-classpath!
  ([]
   (add-jlink-to-classpath! (platform-id (or (keyword (System/getenv "WL_PLATFORM"))
                                             (System/getProperty "os.name")))))
  ([platform]
   (let [path (get-jlink-path platform)]
     (println "Adding path to classpath:" path) ; FIXME (jh) Verify the file exists => good exception
     (pom/add-classpath path))))

(add-jlink-to-classpath!)

(comment

  (base-path :macos)
  (base-path :linux)

  (get-jlink-path :macos)
  (get-jlink-path :linux)

  (get-mathlink-path :macos)
  (get-mathlink-path :linux))
