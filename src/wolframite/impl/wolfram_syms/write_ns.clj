(ns wolframite.impl.wolfram-syms.write-ns
  "Support creating wolframite/wolfram.clj with defs for all loaded Wolfram symbols - to support
  autocompletion (even in editors using static code analysis) and linters (clj-kondo only does
  static code)"
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [wolframite.core :as core]
    [wolframite.impl.wolfram-syms.intern :as intern]
    [wolframite.impl.wolfram-syms.wolfram-syms :as wolfram-syms]
    [wolframite.runtime.defaults :as defaults]
    [wolframite.runtime.system :as system])
  (:import (java.io FileNotFoundException PushbackReader)))

(comment
  (-> (io/resource "wolframite/impl/wolfram_syms/write_ns/includes.clj")
      (io/reader)
      (PushbackReader.)
      edn/read
      (->> (some #(and (list? %) (= :require (first %)) (rest %)))))

  ,)

(defn- inclusions-reader! []
  (io/reader (io/resource "wolframite/impl/wolfram_syms/write_ns/includes.clj")))

(defn- inclusions-ns-info!
  "Return {:require <requires>, :refer-clojure {:only <syms>}}"
  []
 (let [incl-ns (->> (inclusions-reader!)
                    (PushbackReader.)
                    edn/read
                    (keep #(when-let [kw (and (list? %)
                                              (keyword? (first %))
                                              (first %))]
                             [kw (rest %)]))
                    (into {}))]
   (update incl-ns :refer-clojure #(apply hash-map %))))

(defn- inclusions-body-str! []
  (->> (inclusions-reader!)
       (line-seq)
       (drop-while #(not (str/starts-with? % ";;--INCLUDE-START--")))
       next
       (str/join "\n")))

(def wolfram-ns-heading
  (let [{incl-reqs :require, incl-clj-refs :refer-clojure} (inclusions-ns-info!)]
   [(list 'ns 'wolframite.wolfram
          "[GENERATED - see `...wolfram-syms.write-ns/write-ns!`]
           Vars for all Wolfram functions (and their Clojurite aliases, where those exist).
          These can be composed into expressions and passed to `wl/eval`.

          BEWARE: This is based off a particular version of Wolfram and you may need to refresh it."
          (apply list :require
                (into '#{wolframite.impl.wolfram-syms.intern} incl-reqs))
          (list :refer-clojure :only
                (vec (into '#{defmacro map ns-unmap}  ; def and quote do not need to be listed
                           (:only incl-clj-refs)))))
    `(do ~@(map (fn [s] `(ns-unmap *ns* (quote ~s)))
                '[Byte Character Integer Number Short String Thread]))]))

(defn- aliases->defs [aliases]
  (mapv (fn [[from to]] `(def ~from ~to)) aliases))

(def wolfram-ns-footer (aliases->defs defaults/base-aliases))

(defn- make-defs
  ([] (make-defs (wolfram-syms/fetch-all-wolfram-symbols core/eval)))
  ([all-syms]
   (for [{:keys [sym doc]} (sort-by :sym all-syms)]
     (list 'def sym (if (string? doc) doc "") `(intern/wolfram-fn '~sym)))))

(defn write-ns!
  "Load symbols from the running Wolfram kernel and write a namespace file with vars for all of them.
  Args:
  - `path` (default: './src/wolframite/wolfram.clj') - where to write the code
  - `opts` is a map that may contain:
    - `:aliases` - see `wl/init!` for details; a var will be made for each alias, just as we do for `*`,
      so that you can uses it just as you do with `(w/* 2 3)`. Beware: You still also need to pass your
      custom aliases to init! or eval

  Requires that you've run `wl/init!` first."
  ([] (write-ns! "src/wolframite/wolfram.clj"))
  ([path] (write-ns! path nil))
  ([path {:keys [aliases] :as _opts}]
   (let [{:keys [wolfram-version wolfram-kernel-name]} (system/kernel-info!)]
     (try
       (spit path
             (str/join "\n"
                       (concat
                         (map pr-str wolfram-ns-heading)
                         ;; Add version info, similar to clojure's *clojure-version*; marked dynamic so
                         ;; that clj doesn't complain about those *..*
                         [(format "(def ^:dynamic *wolfram-version* %s)" wolfram-version)]
                         [(format "(def ^:dynamic *wolfram-kernel-name* \"%s\")" wolfram-kernel-name)]
                         (map pr-str (make-defs))
                         (map pr-str wolfram-ns-footer)
                         [(inclusions-body-str!)]
                         (some->> aliases aliases->defs (map pr-str)))))
       (catch FileNotFoundException e
         (throw (ex-info (format "Could not write to %s - does the parent dir exist?"
                                 path)
                         {:path path, :cause (ex-message e)})))))))


;(defmacro make-wolf-defs []
;  `(do ~@(make-defs)))
; USAGE: Use
;(macroexpand '(make-wolf-defs))

(comment

  (load-file "src/wolframite/wolfram.clj")
  (do (time (write-ns!))
      (load-file "src/wolframite/wolfram.clj"))

  (write-ns!
    "src/wolframite/wolfram.clj"
    {:aliases '{I Integrate}}))