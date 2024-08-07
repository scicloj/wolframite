(ns wolframite.impl.jlink-instance
  (:require [wolframite.impl.protocols :as proto])
  (:import [wolframite.impl.protocols JLink])
  (:refer-clojure :exclude [get reset!]))

;; The actual impl. of the JLink interface, set at runtime
(defonce jlink-instance (atom nil))

(defn ^JLink get [] (deref jlink-instance))

(defn reset! [] (clojure.core/reset! jlink-instance nil))
