(ns wolframite.lib.helpers
  (:require [clojure.java.browse :refer [browse-url]]
            [wolframite.impl.wolfram-syms.intern :as intern]))

(defn help-link [fn-sym]
  (format "https://reference.wolfram.com/language/ref/%s.html" fn-sym))

(defn- just-symbols [clj-form]
  (filter symbol? (tree-seq seq? seq clj-form)))

(defn help!
  "Get web based help for a given symbol or form.
  If a form is given this will operate on all symbols found in the form.
  By default, opens web browser with relevant reference documentation.
  You can pass `links true` if you just want URLs."
  [sym-or-form & {:keys [return-links links]}]
  (let [return-links? (or return-links links)
        sym-or-form' (or (intern/interned-var-val->symbol sym-or-form)
                         sym-or-form)
        links (cond
                (symbol? sym-or-form') [(help-link sym-or-form')]
                (list? sym-or-form') (map help-link (just-symbols sym-or-form'))
                :else (throw (ex-info "You need to pass `symbol?` or `list?` as argument"
                                      {:passed-type (type sym-or-form')})))]
    (if return-links?
      links
      (doseq [l links]
        (browse-url l)))))

(comment
  (help! 'GeoGraphics)
  (help! '(GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"])))
  (help! '(GeoImage (Entity "City" ["NewYork" "NewYork" "UnitedStates"]))
         :return-links true)
  (help! []))
