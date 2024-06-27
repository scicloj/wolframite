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
   clojure.repl.deps ; cemerick.pomegranate
   [wolframite.runtime.system :as system]))

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
                    (if *repl*
                      (do (println (str "=== Adding path to classpath: " p " ==="))
                          (clojure.repl.deps/add-lib 'w/w {:local/root p})) ; BEWARE: only works in REPL => not when running from CLI, e.g. via a test runner
                     (when-not (try (Class/forName "com.wolfram.jlink.Expr") (catch ClassNotFoundException _))
                       (throw (IllegalStateException. "JLink jar not on the classpath and can't be loaded because we are not in a dynamic context, such as REPL"))))
                    ;(cemerick.pomegranate/add-classpath p)
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
