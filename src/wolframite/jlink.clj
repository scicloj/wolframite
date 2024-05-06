(ns wolframite.jlink
  "
  'J/Link' integrates the Wolfram Language and Java (https://reference.wolfram.com/language/JLink/tutorial/Introduction.html): providing a two-way bridge between the two.

  Accordingly, this namespace manages the J/Link connection in order to extend this bridge to the Clojure language. As such, this namespace is crucial to the whole wolframite project.

  WARNING: This namespace is side effecting, and is required for many of the files in this project to compile.

  NOTE:

  By listing defaults in order of preference, we can decide whether we prefer Mathematica or Wolfram Engine. Currently, we assume that Mathematica is more featureful.

  Paths which are not absolute should not start with a '/'. In general, we should be aiming to comply with the babashka.fs standards.

  Pomegranate is used to dynamically add the Wolfram Language / Mathematica JLink jar to the JVM classpath.

  Because many of the namespaces in this project either import or reference the jlink classes, it's necessary to have loaded this namespace before those namespaces will compile. Thus, you'll see this ns required, but unused, across the codebase. This is to get around that fact that we don't have the jar available to us through a standard maven repository, and can't use environment variables in our `deps.edn` specifications.

  Function argument types (that differ from clojure defaults):
  os - keyword

  TODO:
  - Rename platform to OS throughout? (this would be clearer and there are references to screen platforms as well in the code)
  - Should all of the general install definitions/functions be put into a separate namespace?
  - clarify which functions are actually used outside of this file and restructure accordingly.
  "

  (:require
   [babashka.fs :as fs]
   [cemerick.pomegranate :as pom]
   [clojure.string :as string])
  (:import
   (java.lang System)))

;; ===
;; 1) We need the jlink JAR to talk to Wolfram:
;; ===
(def supported-platforms #{:linux :macos :windows})
(def ^:private jlink "SystemFiles/Links/JLink/JLink.jar") ; Expected relative path within the install directory.

;; ===
;; Changed paths to a flat collection of maps. Hopefully this is simpler to reason about as well as to add new default paths.
;;
;; These should be listed in order of preference
;; ===
(def ^:private defaults
  [{:root  "/usr/local/Wolfram/Mathematica"
    :kernel "Executables/MathKernel"
    :product :mathematica
    :platform :linux}
   {:root  "/opt/Mathematica"
    :kernel "Executables/MathKernel"
    :product :mathematica
    :platform :linux}
   {:root  "/usr/local/Wolfram/WolframEngine"
    :kernel "Executables/WolframKernel"
    :product :wolfram-engine
    :platform :linux}

   {:root  "/Applications/Mathematica.app/Contents"
    :kernel "MacOS/MathKernel"
    :product :mathematica
    :platform :macos}
   {:root   "/Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents"
    :kernel "MacOS/WolframKernel"
    :product :wolfram-engine
    :platform :macos}

   {:root "/c:/Program Files/Wolfram Research/Mathematica/"
    :kernel "WolframKernel.exe"
    :product :mathematica
    :platform :windows}
   {:root "/c:/Program Files/Wolfram Research/Wolfram Engine/"
    :kernel "MathKernel.exe"
    :product :wolfram-engine
    :platform :windows}])

(defn- supplied-paths
  "The paths given via Java property or environment variables.

  NOTE: These should be root directories that contain Mathematica or Wolfram engine files, not subdirectories.

  TODO:
  - Should this be here or somewhere else? Not directly relevant to jlink.
  - (jh) alternatively, support Java system properties?"
  []
  (let [names  ["JLINK_JAR_PATH"
                "MATHEMATICA_INSTALL_PATH"
                "WOLFRAM_INSTALL_PATH"]
        method (fn [f] (-> f (nth  2) first str (string/split #"/") last))
        fs ['(fn [name] (System/getProperty name)) '(fn [name] (System/getenv name))]]
    (->> (map (fn [f]
                (map (fn [name] [[(keyword name) ((eval f) name)]
                                 [:source (method f)]])
                     names))
              fs)
         (apply concat)
         (map #(into {} %)))))

(defn- version-vector
  "Splits a given path into a vector of numbers.

  ASSUME: path node (folder) is period-separated number, e.g. 13.0.2 -> [13 0 2]
  "
  [version-dir]
  (-> version-dir
      fs/file-name
      (string/split #"\.")
      (->> (mapv parse-long))))

(defn- version-path
  "Detects type of path, i.e. whether or not there is a version number subdirectory, and returns the relevant one.

  TODO: Functions more general than jlink should not be in this namespace. Maybe make a 'system' namespace?
  NOTE:
  - Returns a string.
  "
  [base-path]

  (let [path (when (fs/exists? base-path) (str base-path))
        version-dir
        (some->> path
                 fs/list-dir
                 (sort-by version-vector)
                 last
                 str)]
    (if version-dir version-dir
        (if path
          (throw
           (ex-info (str "Could not find a Wolfram base directory (directory with numerical values separated by '.') at the given base path.")
                    {:paths base-path}))))))

(defn- ->platform-id
  "Coerces to a common os identifier or throws an 'unrecognised' error."
  [os]
  (cond
    (#{:linux "Linux" "linux"} os)         :linux
    (#{:osx :macos "Mac OS X" "mac"} os) :macos
    (or (#{:win :windows} os) (string/starts-with? os "Windows")) :windows
    :else (throw
           (ex-info (str "Did not recognise " os " as a valid platform.")
                    {:platform os}))))

(defn detect-platform
  "Tries to detect the current operating system.

  Currently just checks a java property, but could search default paths in the future."
  []
  (->platform-id (System/getProperty "os.name")))

(defn- select-installation
  "Chooses the application options according to the platform, guessing the platform if not provided."
  ([]
   (select-installation (detect-platform)))

  ([os]
   (->> defaults
        (filter #(= os (:platform %)))
        (filter (comp fs/exists? :root))
        first)))

(defn- path--jlink
  "The full path to jlink.

  NOTE: Returns a string."
  ([os]
   (let [jpaths (keep :JLINK_JAR_PATH (supplied-paths))]
     (if (not-empty jpaths)
       (first jpaths)
       (-> os
           select-installation
           :root
           version-path
           (fs/path jlink)
           str)))))

(defn path--kernel
  "Using the given base path, checks if any of the wolfram binaries can be found.

  TODO: Maybe just search to see if the executable is anywhere under the given root?
  TODO: replace filter with something that returns on first successful test.
  "
  ([] (path--kernel (:root (select-installation))))
  ([base-path]
   (let [path (version-path base-path)
         options (->> defaults
                      (map :kernel)
                      (map #(fs/path path %)))]

     (or (-> (filter fs/exists? options)
             first)
         (throw (ex-info (str "Could not find a Wolfram executable at the given base path. We looked in these places: " (seq options) ".") {:paths options}))))))

(defn add-jlink-to-classpath!
  "Tries and 'throws' otherwise."
  ([]
   (add-jlink-to-classpath! (detect-platform)))
  ([os]
   (let [path (path--jlink os)]
     (when-not (fs/exists? path)
       (throw (ex-info (str "Unable to find JLink jar at the expected path " path
                            " Consider setting one of the supported environment variables;"
                            " currently: " (into [] (supplied-paths)) ".")
                       {:platform os
                        :path path
                        :env (supplied-paths)})))
     (println (str "=== Adding path to classpath:" path " ==="))
     (pom/add-classpath path))))

;; ==================================================
;; ENTRY POINT
;; ==================================================
(add-jlink-to-classpath!)
;; ==================================================
