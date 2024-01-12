(ns clojuratica.jlink
  "ATTENTION! This namespace is side effecting, and is required for many of the files in this project to compile.
  It uses Pomegranate to dynamically add the Wolfram Language / Mathematica JLink jar to the JVM classpath.
  Because many of the namespaces in this project either import or reference the jlink classes, it's necessary to have loaded this namespace before those namespaces will compile.
  ;Thus, you'll see this ns required but unused across the codebase.
  This is to get around that fact that we don't have the jar available to us through a standard maven repository, and can't use environment variables in our `deps.edn` specifications."
  (:require [cemerick.pomegranate :as pom]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.nio.file Paths)))

(defn- read-install-path-setting []
  (or (System/getProperty "MATHEMATICA_INSTALL_PATH")
      (System/getProperty "WOLFRAM_INSTALL_PATH")
      (System/getenv "MATHEMATICA_INSTALL_PATH") ; TODO (jh) support alternatively Java system properties?
      (System/getenv "WOLFRAM_INSTALL_PATH")))

(defn wolframite-path-settings-info []
  (str/join ", "
            (for [v ["JLINK_JAR_PATH" "MATHEMATICA_INSTALL_PATH" "WOLFRAM_INSTALL_PATH"]]
              (str v ": " (or (some->> (System/getProperty v) (format "\"%s\" (source: system property)"))
                              (some->> (System/getenv v) (format "\"%s\" (source: environmental variable)"))
                              "nil")))))

(defn- unable-to-find-exc []
  (let [env (wolframite-path-settings-info)]
    (ex-info
     (str "Unable to find Mathematica or Wolfram Engine installation."
          " Please specify using an appropriate environment variable."
          " Current values:" env)
     {:env env})))

(def ^:private paths
  {:linux {:wolfram-engine {:path "/usr/local/Wolfram/WolframEngine"
                            :mathlink-suffix "/Executables/WolframKernel"} ; FIXME verify
           :mathematica {:path "/usr/local/Wolfram/Mathematica"
                         :mathlink-suffix "/Executables/MathKernel"}}
   :macos {:wolfram-engine {:path "/Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents"
                            :mathlink-suffix "/MacOS/WolframKernel"}
           :mathematica {:path "/Applications/Mathematica.app/Contents"
                         :mathlink-suffix "/MacOS/MathKernel"}}
   :windows {:wolfram-engine {:path "/c:/Program Files/Wolfram Research/Wolfram Engine/"
                              :mathlink-suffix "/MathKernel.exe"} ; TODO verify
             :mathematica {:path "/c:/Program Files/Wolfram Research/Mathematica/"
                           :mathlink-suffix "/WolframKernel.exe"}}})

(defn- file-exists? [path]
  (and path (.exists (io/file path))))

(defn detect-available-installation [platform]
  (->> (-> (get paths platform)
           ((juxt :mathematica :wolfram-engine)))  ; prefer Mathematica to Wolfram, it is presumably more capable
       (filter (comp file-exists? :path))
       first))

  ;; 1) We need the jlink JAR to talk to Wolfram:
(def ^:private jlink-suffix "/SystemFiles/Links/JLink/JLink.jar") ; path within the install dir

(def supported-platform? #{:linux :macos :windows})

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
    (str (.normalize (Paths/get path-root (into-array String path-tail))))))

(defn guess-mathkernel-suffix-for [base-path]
  (first
   (for [mathkernel-suffix (->> paths vals (mapcat vals) (map :mathlink-suffix))
         :when (file-exists? (file-path base-path mathkernel-suffix))]
     mathkernel-suffix)))

(defn- version-vector [version-dir]
  (->  version-dir
       io/file
       (.getName)
       (str/split #"\.")
       (->> (mapv parse-long))))

(defn- version-path
  "For installation that use the format <base path>/<version>, detect full path to the latest one

  NOTE: Returns a string.
  "
  [base-path]
  (if-let [version-dir
           (some->> (io/file base-path)
                    (.listFiles)
                    (sort-by version-vector)
                    (last)
                    (.getAbsolutePath))]
    version-dir
    (throw (unable-to-find-exc))))

(defn platform-paths [platform]
  (when-not (supported-platform? platform)
    (throw (ex-info (str "Unsupported platform: " platform ". Supported: " supported-platform?)
                    {:platform platform})))
  (or (when-let [path (read-install-path-setting)]
        {:path path
         :mathlink-suffix (or (guess-mathkernel-suffix-for path)
                              (throw (Exception. (str "Couldn't find MathKernel / WolframKernel under the given path '"
                                                      path "' at any of the known locations."))))})
      (cond-> (detect-available-installation platform)
        (#{:linux :windows} platform)
        (update :path version-path))))

(defn get-jlink-path
  ([platform]
   (or (System/getProperty "JLINK_JAR_PATH")
       (System/getenv "JLINK_JAR_PATH")
       (-> platform
           platform-paths
           :path
           (str jlink-suffix)))))

(defn get-mathlink-path
  "Get path to the MathKernel executable, to pass to JLink for starting the process"
  ([platform]
   (let [{:keys [path mathlink-suffix]} (platform-paths platform)]
     (file-path path mathlink-suffix))))

(defn add-jlink-to-classpath!
  ([]
   (add-jlink-to-classpath! (platform-id (or (keyword (System/getenv "WL_PLATFORM"))
                                             (System/getProperty "os.name")))))
  ([platform]
   (let [path (get-jlink-path platform)]
     (when-not (.exists (io/file path))
       (throw (ex-info (str "Unable to find JLink jar at the expected path " path
                            " Consider setting one of the supported environment variables;"
                            " currently: " (wolframite-path-settings-info))
                       {:platform platform
                        :path path
                        :env (wolframite-path-settings-info)})))
     (println "Adding path to classpath:" path)
     (pom/add-classpath path))))

(add-jlink-to-classpath!)

(comment

  (get-jlink-path :macos)
  (get-jlink-path :linux)
  (get-jlink-path :windows)

  (get-mathlink-path :macos)
  (get-mathlink-path :linux)
  (get-mathlink-path :windows)

  (platform-paths :linux))
