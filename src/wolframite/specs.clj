(ns wolframite.specs
  (:require [clojure.spec.alpha :as s]
            clojure.set
            [wolframite.runtime.defaults :as defaults]))

(s/def :wl/flag defaults/all-flags)
(s/def :wl/flags (s/coll-of :wl/flag :kind set?))

(s/def :wl/aliases (s/map-of symbol? symbol?))

(s/def :wl/opts-kw #{:opts})
(s/def :wl/opts-map (s/keys :opt [:wl/flags :wl/aliases]))

(s/def :wl/args (s/alt
                 :no-options   (s/cat :body any?)
                 :with-options (s/cat :opts-kw? :wl/opts-kw :opts-map (s/nilable :wl/opts-map) :body any?)))

(comment ;; spec examples

  (s/valid? :wl/flags [:as-function :clojure-form])

  ;; ** :wl/args
  ;; *** yes
  (s/valid? :wl/args [:opts {:flags #{:parse/as-function :debug/verbose :convert/hash-maps}} '(Dot [1 3 4] [5 4 6])])
  (s/valid? :wl/args ['(Dot [1 3 4] [5 4 6])])
  (s/valid? :wl/args [:opts {} '(Dot [1 3 4] [5 4 6])])
  (s/valid? :wl/args [:opts nil '(Dot [1 3 4] [5 4 6])])

  ;; *** no
  (not (s/valid? :wl/args []))
  (not (s/valid? :wl/args nil))

  (s/conform :wl/args [:opts {:flags #{:parse/as-function :debug/verbose :convert/hash-maps}} '(Dot [1 3 4] [5 4 6])])


  (let [args #_['(Dot [1 3 4] [5 4 6])] [:opts {:flags #{:parse/as-function :debug/verbose :convert/hash-maps}} '(Dot [1 3 4] [5 4 6])]
        [options-flag {:keys [opts-map body]}] (s/conform :wl/args args)]
    (if (= :with-options options-flag)
      [opts-map body]
      [body]))


  )
