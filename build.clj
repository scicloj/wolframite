(ns build
  "Project build config as code for clojure's tools.build"
  (:require [build-config]
            [babashka.fs :as fs]
            [clojure.tools.build.api :as b]
            [clojure.edn :as edn]
            [scicloj.clay.v2.api :as clay]))

(def project (-> (edn/read-string (slurp "deps.edn"))
                 :aliases :neil :project))
(def lib (or (:name project) 'my/lib1))

;; use neil project set version 1.2.0 to update the version in deps.edn

(def version (or (:version project)
                 "1.2.0"))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]
                :pom-data
                [[:description "An interface between Clojure and Wolfram Language (the language of Mathematica)"]
                 [:url "https://github.com/scicloj/wolframite"]
                 [:licenses
                  [:license
                   [:name "Eclipse Public License 2.0"]
                   [:url "https://opensource.org/license/epl-2-0/"]]]
                 [:scm
                  [:url "https://github.com/scicloj/wolframite"]
                  [:connection "scm:git:https://github.com/scicloj/wolframite.git"]
                  [:developerConnection "scm:git:ssh:git@github.com:scicloj/wolframite.git"]
                  [:tag (str "v" version)]]]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install [_]
  (jar {})
  (b/install {:basis basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy [opts]
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  opts)

(defn build-site [opts]
  (println "Going to build docs ...")
  (clay/make! (assoc build-config/config
                :clean-up-target-dir true
                :show false))
  (System/exit 0) ; something keeps the JVM alive and I don't know what so kill it
  opts)
