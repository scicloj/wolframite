(ns wolframite.runtime.jlink
  "
  'J/Link' integrates the Wolfram Language and Java (https://reference.wolfram.com/language/JLink/tutorial/Introduction.html): providing a two-way bridge between the two.

  Accordingly, this namespace manages the J/Link connection in order to extend this bridge to the Clojure language

  WARNING: This namespace is (currently) side effecting, and is required for many of the files in this project to compile.

  NOTE:

  - Paths which are not absolute should not start with a '/'. In general, we should be aiming to comply with the babashka.fs standards.
  - Pomegranate is used to dynamically add the Wolfram Language / Mathematica JLink jar to the JVM classpath.
  - Because many of the namespaces in this project either import or reference the jlink classes, it's necessary to have loaded this namespace before those namespaces will compile. Thus, you'll see this ns required, but unused, across the codebase. This is to get around that fact that we don't have the jar available to us through a standard maven repository, and can't use environment variables in our `deps.edn` specifications.

  Function argument types (that differ from clojure defaults):
  os - keyword
  "
  (:require
   [babashka.fs :as fs]
   [cemerick.pomegranate :as pom]
   [wolframite.runtime.system :as system])
  (:import
   (java.lang System)))

(def ^:private default-jlink-path-under-root "SystemFiles/Links/JLink/JLink.jar")

(defn- path--jlink
  "The full path to jlink.

  NOTE: Returns a string."
  ([info]
   (let [{:keys [user-paths defaults]} info
         jpaths (keep :JLINK_JAR_PATH user-paths)]
     (or (first jpaths)
         (-> defaults
             :root
             (fs/path default-jlink-path-under-root)
             str)))))

(defn find-jlink-jar
  "Searches the machine for an instance of JLink.jar

  NOTE: Not used by default, but available for the discerning user. "
  []
  (->  (fs/glob "/" "**/JLink.jar")
       first
       str))

(defn add-jlink-to-classpath!
  "Checks for valid locations of the jar file. If one is not found based on the system defaults, then performs a machine search. If that doesn't work either, then 'throws'."

  ([]
   (let [info (system/info)
         path (path--jlink info)
         add-path (fn [p]
                    (println (str "=== Adding path to classpath: " p " ==="))
                    (pom/add-classpath p)
                    true)]
     (if (fs/exists? path)
       (add-path path)
       (or
        (throw (ex-info (str "Unable to find JLink jar at the expected path " path
                             " Consider setting one of the supported environment variables;"
                             " currently: " (into [] (:user-paths info)) ".")
                        {:os (get-in info [:defaults :os])
                         :path path
                         :env (:user-paths info)})))))))

;; ==================================================
;; ENTRY POINT
;; ==================================================
(add-jlink-to-classpath!)
;; ==================================================

