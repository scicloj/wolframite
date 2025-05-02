(ns wolframite.runtime.jlink
  "
  'J/Link' integrates the Wolfram Language and Java (https://reference.wolfram.com/language/JLink/tutorial/Introduction.html): providing a two-way bridge between the two.

  Accordingly, this namespace manages the J/Link connection in order to extend this bridge to the Clojure language

  NOTE:

  - Paths which are not absolute should not start with a '/'. In general, we should be aiming to comply with the babashka.fs standards.
  - clojure.repl.deps is used to dynamically add the Wolfram Language / Mathematica JLink jar to the JVM classpath.

  Function argument types (that differ from clojure defaults):
  os - keyword
  "
  (:require
   [babashka.fs :as fs]
   [clojure.tools.logging :as log]
   ;clojure.repl.deps ; required dynamically, for a better error on old clj
   [wolframite.runtime.system :as system]))

(def ^:private default-jlink-path-under-root "SystemFiles/Links/JLink/JLink.jar")

(defn- path--jlink
  "The full path to jlink.

  NOTE: Returns a string."

  ;; TODO: Should WOLFRAM_INSTALL_PATH take priority over JLINK_JAR_PATH?

  ([] (path--jlink (system/root (system/info))))
  ([base-path]
   (->  base-path
        (fs/path default-jlink-path-under-root)
        str)))

(comment (-> (path--jlink)
             fs/exists?))

(defn find-jlink-jar
  "Searches the machine for an instance of JLink.jar

  NOTE: Not used by default, but available for the discerning user. "
  []
  (->  (fs/glob "/" "**/JLink.jar")
       first
       str))

(defn add-jlink-to-classpath!
  "Checks for valid locations of the jar file and 'throws' is unsuccessful.

  If one is not found based on the system defaults, then you can perform a machine-wide search using `find-jlink-jar`. "

  ([]
   (let [info (system/info)
         path (path--jlink)
         add-path (fn [p]
                    (when (neg? (compare ((juxt :major :minor) *clojure-version*) [1 12]))
                      (throw (UnsupportedOperationException. "Adding Wolfram's JLink lib to the REPL dynamically requires Clojure 1.12 or later. Alternatively, ensure it is already on the classpath")))
                    (when-not @(requiring-resolve 'clojure.core/*repl*)
                      (throw (throw (UnsupportedOperationException. "JLink jar not on the classpath and can't be loaded because we are not in a dynamic context, such as REPL"))))
                    (log/info "=== Adding path to classpath: " p " ===")
                    ((requiring-resolve 'clojure.repl.deps/add-lib) 'w/w {:local/root p}) ; dyn require so we can give good error on older clj
                    nil)]
     (cond
       (try (Class/forName "com.wolfram.jlink.Expr") (catch ClassNotFoundException _)) false ; skip, already loaded
       (fs/exists? path) (do (add-path path) true)

       :else (throw (ex-info (str "Unable to find JLink jar at the expected path " path
                                  " Consider setting one of the supported environment variables;"
                                  " currently: " (into [] (:user-paths info)) ".")
                             {:os (get-in info [:defaults :os])
                              :path path
                              :env (:user-paths info)}))))))

(comment
  (add-jlink-to-classpath!)
  (println (find-jlink-jar)))
