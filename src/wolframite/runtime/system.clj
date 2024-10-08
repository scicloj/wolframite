(ns wolframite.runtime.system
  "For dealing with the runtime system, primarily the operating system specific default paths."
  (:require
    [babashka.fs :as fs]
    [clojure.string :as str])
  (:import
    (java.lang System)))

(def supported-OS #{:linux :mac :windows})

;; Default path information can be listed here, in order of preference.
(def ^:private defaults
  [;; LINUX
   {:root  "/usr/local/Wolfram/Wolfram"
    :kernel "Executables/WolframKernel"
    :product :wolfram ; version 14.1+
    :os :linux}
   {:root  "/usr/local/Wolfram/Mathematica"
    :kernel "Executables/MathKernel"
    :product :mathematica ; prior to 14.1
    :os :linux}
   {:root  "/opt/Mathematica"
    :kernel "Executables/MathKernel"
    :product :mathematica ; alternative location
    :os :linux}
   {:root  "/opt/WolframEngine"
    :kernel "Executables/MathKernel"
    :product :wolfram-engine
    :os :linux}
   {:root  "/usr/local/Wolfram/WolframEngine"
    :kernel "Executables/WolframKernel"
    :product :wolfram-engine
    :os :linux}
   ;; MAC
   {:root  "/Applications/Wolfram.app/Contents"
    :kernel "MacOS/WolframKernel"
    :product :wolfram ; version 14.1+
    :os :mac}
   {:root  "/Applications/Mathematica.app/Contents"
    :kernel "MacOS/MathKernel"
    :product :mathematica ; prior to 14.1
    :os :mac}
   {:root   "/Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents"
    :kernel "MacOS/WolframKernel"
    :product :wolfram-engine
    :os :mac}
   ;; WINDOWS
   {:root "/c:/Program Files/Wolfram Research/Wolfram/" ; TODO verify
    :kernel "WolframKernel.exe" ; TODO verify
    :product :wolfram ; version 14.1+
    :os :windows}
   {:root "/c:/Program Files/Wolfram Research/Mathematica/"
    :kernel "WolframKernel.exe"
    :product :mathematica ; prior to 14.1
    :os :windows}
   {:root "/c:/Program Files/Wolfram Research/Wolfram Engine/"
    :kernel "MathKernel.exe"
    :product :wolfram-engine
    :os :windows}])

(defn find-bin
  "Searches the machine for one of the listed binaries and returns the path of the first one found.

  NOTE: Not used by default as there is a risk that rather than supplying an environment variable the user would just wait several minutes at each startup.
  "
  []
  (->> (keep :kernel defaults)
       (map fs/file-name)
       distinct
       (some #(let [paths (-> (fs/glob "/"
                                       (format "**/%s" %)))]
               (when (seq paths) paths)))
       first
       str))

(defn- user-paths
  "The paths given via Java property or environment variables.

  NOTE: These should be root directories that distinguish Mathematica or Wolfram engine files, not subdirectories. E.g.  '/usr/local/Wolfram/Mathematica'.
  "
  []
  (let  [properties  ["JLINK_JAR_PATH" "WOLFRAM_INSTALL_PATH"]]
    (->> properties
         (keep (fn [property]
                 (when-let [v ((some-fn #(System/getProperty %)
                                        #(System/getenv %))
                               property)]
                   [property v])))
         (into {})
         not-empty)))

(defn- version-number-path?
  "Checks if the given path is actually a version-number subdirectory."
  [path]
  (-> (fs/file-name path)
      (->> (re-matches #"(\d+)(?:\.(\d+))*"))
      boolean))

(defn- version
  "Extracts the version from a given path into a vector of numbers.

  ASSUME: path node (folder) is period-separated number, e.g. 13.0.2 -> [13 0 2]
  "
  [version-path]
  (-> version-path
      fs/file-name
      (str/split #"\.")
      (->> (mapv parse-long))))

(defn- ->version-path
  "Detects type of path, i.e. whether or not there is a version number subdirectory, and returns the relevant one.

  NOTE:
  - Returns a string.
  "
  [base-path]
  (let [path (when (fs/exists? base-path) (str base-path))
        version-dir
        (some->>  path
                  fs/list-dir
                  (filter fs/directory?)
                  (filter version-number-path?)
                  (sort-by version)
                  last
                  str)]
    (if version-dir version-dir
        (when path path))))

(defn- ->os
  "Coerces to a common OS identifier keyword or throws an 'unrecognised' error."
  [os]
  (let [os-str (str os)]
    (cond
      (#{"Linux" "linux"} os-str) :linux
      (#{"osx" "macos" "Mac OS X" "mac"} os-str) :mac
      (or (#{"win" "windows"} os-str) (str/starts-with? os-str "Windows")) :windows
      :else (throw
              (ex-info (str "Did not recognise " os-str " as a supported OS.")
                       {:os os-str})))))

(defn detect-os
  "Tries to determine the current operating system.

  Currently just checks a java property, but could search default paths in the future.
  "
  []
  (->os (System/getProperty "os.name")))

(defn- choose-defaults
  "Selects the application options according to the OS, guessing the OS if not provided."
  ([]
   (choose-defaults (detect-os)))

  ([os]
   (-> (->> defaults
            (filter (comp #{os} :os))
            (filter (comp fs/exists? :root))
            first)
       (update :root ->version-path))))

(defn info
  "Publicly available way of guessing the defaults."
  []
  (let [user (not-empty (user-paths))
        default (not-empty (choose-defaults))]
    (when-not (or user default)
      (throw (ex-info "Could not find a Wolfram or Mathematica kernel at a default location and no custom one was provided. Please check the installation and provide a path according to the user guide."
                      {})))
    {:user-paths user
     :defaults default}))

(defn path--kernel
  "Using the given base path, checks if any of the wolfram binaries can be found. If not, perform a 'glob' search and if that doesn't work either then throw an error!
  "
  ([] (path--kernel (:root (choose-defaults))))
  ([base-path]
   (if-let [path (->version-path base-path)]
     (let [options (when path
                     (->> defaults
                          (map :kernel)
                          (map #(fs/path path %))))]
       (or (-> (filter fs/exists? options)
               first
               str)
           (throw (ex-info (str "Could not find a Wolfram executable at the given base path. We looked in these places: " (seq options) ".") {:paths options}))))
     (throw (ex-info (str "Could not find a Wolfram executable using the given base path. Are you sure that " base-path " exists?")
                     {:paths base-path})))))

(comment (defn common-path
           "
  The longest parental directory path that is common to both input paths.
  "
           [path1 path2]
           ;; TODO: This should be moved to some utility library.
           (let  [sep "/"
                  rx-sep (re-pattern sep)]
             (->> [path1 path2]
                  (map str)
                  (map #(str/split % rx-sep))
                  (apply map (fn [x y] (if (= x y) x false)))
                  (take-while (comp not false?))
                  (string/join rx-sep)))))
