(ns wolframite.impl.jlink-instance
  (:refer-clojure :exclude [get reset!])
  (:require
    [wolframite.impl.protocols :as proto])
  (:import
    (wolframite.impl.protocols JLink)))

;; The actual impl. of the JLink interface, set at runtime
(defonce jlink-instance (atom nil))

(defn ^JLink get [] (deref jlink-instance))

(defn reset! [] (clojure.core/reset! jlink-instance nil))
