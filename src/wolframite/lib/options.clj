(ns wolframite.lib.options
  (:require [wolframite.runtime.defaults :as defaults]))

;(defn filter-params [current-options]
;  (into {} (filter #(keyword? (key %)) current-options)))

(defn filter-flags  [current-options]
  (into {} (filter #(set? (first %)) current-options)))

(defn filter-flag-sets
  [flag current-options]
  (filter #(some #{flag} %) (keys (filter-flags current-options))))

(defn flags-into
  "Conjoins each flag in flags into hash-map *flags*, setting each such flag as the value
  of any keys that contain flag as an element."
  [current-options flags]
  (into current-options
    (for [flag        flags
          active-set (filter-flag-sets flag current-options)]
      [active-set flag])))

;(defn params-into
;  "Conjoins each key-value pair in options into hash-map *options*, conjoining each such pair
;  only if options-keys contains that key."
;  [current-options params]
;  (into current-options
;    (filter #(some #{(first %)} (keys (filter-params current-options))) params)))

;(defn options-into [current-options params flags]
;  (flags-into (params-into current-options params) flags))

(defn flag? [current-options flag]
  (not (nil? (some #{flag} (vals (filter-flags current-options))))))

(def ^:private effective-flags
  "The set of flags in effect given the `user-flags`, i.e. the defaults overridden by the user's.
  Memoized, as this is checked many times per evaluation (incl. for every parsed (sub)expression),
  while the user flags are typically one of very few sets."
  (memoize
   (fn [user-flags]
     (set (vals (filter-flags (flags-into defaults/default-options user-flags)))))))

(defn flag?' [user-flags flag]
  (contains? (effective-flags user-flags) flag))

(defn set-flag [flags f]
  (flags-into flags [f]))

(defn aliases [base-list]
  (or (defaults/default-options base-list)
      (defaults/default-options :clojure-aliases)))
