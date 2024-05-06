(ns wolframite.runtime.system
  "For dealing with the runtime system, primarily the operating system specific defaults."
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string])
  (:import
   (java.lang System)))

(def supported-OS #{:linux :mac :windows})

;; Default path information can be listed here, in order of preference.
(def ^:private defaults
  [{:root  "/usr/local/Wolfram/Mathematica"
    :kernel "Executables/MathKernel"
    :product :mathematica
    :os :linux}
   {:root  "/opt/Mathematica"
    :kernel "Executables/MathKernel"
    :product :mathematica
    :os :linux}
   {:root  "/usr/local/Wolfram/WolframEngine"
    :kernel "Executables/WolframKernel"
    :product :wolfram-engine
    :os :linux}

   {:root  "/Applications/Mathematica.app/Contents"
    :kernel "MacOS/MathKernel"
    :product :mathematica
    :os :mac}
   {:root   "/Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents"
    :kernel "MacOS/WolframKernel"
    :product :wolfram-engine
    :os :mac}

   {:root "/c:/Program Files/Wolfram Research/Mathematica/"
    :kernel "WolframKernel.exe"
    :product :mathematica
    :os :windows}
   {:root "/c:/Program Files/Wolfram Research/Wolfram Engine/"
    :kernel "MathKernel.exe"
    :product :wolfram-engine
    :os :windows}])

(defn- user-paths
  "The paths given via Java property or environment variables.

  NOTE: These should be root directories that distinguish Mathematica or Wolfram engine files, not subdirectories. E.g.  '/usr/local/Wolfram/Mathematica'.

  TODO:
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

(defn- version-number?
  [path]
  (-> (fs/file-name path)
      first
      str
      parse-long
      some?))

(defn- version
  "Extracts the version from a given path into a vector of numbers.

  ASSUME: path node (folder) is period-separated number, e.g. 13.0.2 -> [13 0 2]
  "
  [version-path]
  (-> version-path
      fs/file-name
      (string/split #"\.")
      (->> (mapv parse-long))))

(defn- ->version-path
  "Detects type of path, i.e. whether or not there is a version number subdirectory, and returns the relevant one.

  NOTE:
  - Returns a string.
  "
  [base-path]
  (let [path (when (fs/exists? base-path) (str base-path))
        version-dir
        (some->>  "/opt/Mathematica"
                  fs/list-dir
                  (filter fs/directory?)
                  (filter version-number?)
                  (sort-by version)
                  last
                  str)]
    (if version-dir version-dir
        (if path path
            (throw
             (ex-info (str "Could not find a Wolfram base directory (directory with numerical values separated by '.') at the given base path.")
                      {:paths base-path}))))))

(defn- ->os
  "Coerces to a common OS identifier or throws an 'unrecognised' error."
  [os]
  (cond
    (#{:linux "Linux" "linux"} os)         :linux
    (#{:osx :macos "Mac OS X" "mac"} os) :mac
    (or (#{:win :windows} os) (string/starts-with? os "Windows")) :windows
    :else (throw
           (ex-info (str "Did not recognise " os " as a supported OS.")
                    {:os os}))))

(defn detect-os
  "Tries to detect the current operating system.

  Currently just checks a java property, but could search default paths in the future.
  "
  []
  (->os (System/getProperty "os.name")))

(defn- choose-defaults
  "Selects the application options according to the OS, guessing the OS if not provided.

  TODO: Add the version directory type coercion here?
  "
  ([]
   (choose-defaults (detect-os)))

  ([os]
   (-> (->> defaults
            (filter #(= os (:os %)))
            (filter (comp fs/exists? :root)))
       first
       ;; (update :root ->version-path)
       )))
(defn info
  "Publicly available way of guessing the defaults."
  []
  {:user-paths (user-paths)
   :defaults (choose-defaults)})

(defn path--kernel
  "Using the given base path, checks if any of the wolfram binaries can be found.

  TODO: Maybe just search to see if the executable is anywhere under the given root?
  TODO: replace filter with something that returns on first successful test.
  "
  ([] (path--kernel (:root (choose-defaults))))
  ([base-path]
   (let [path (->version-path base-path)
         options (->> defaults
                      (map :kernel)
                      (map #(fs/path path %)))]

     (or (-> (filter fs/exists? options)
             first)
         (throw (ex-info (str "Could not find a Wolfram executable at the given base path. We looked in these places: " (seq options) ".") {:paths options}))))))
