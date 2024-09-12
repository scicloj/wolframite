(ns wolframite.base.types)

(defrecord Entity [Entity])
(defrecord EntityProperty [EntityProperty])

(def all-types #{Entity EntityProperty})

(defn wolframite-type? [x]
  (and (record? x)
       (some #(instance? % x) all-types)))
