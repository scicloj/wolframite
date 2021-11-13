;; (ns clojuratica.alt
;;   (:require [clojure.spec.alpha :as s]
;;             [clojure.spec.test.alpha :as st]
;;             [clojuratica.core :as wl :refer [WL]]
;;             [clojuratica.lib.options :as options]
;;             [clojuratica.base.convert :as convert]
;;             [clojuratica.runtime.defaults :as defaults]
;;             [clojuratica.specs :as specs]
;;             [clojuratica.base.parse :as parse]))

;; ;; (remove-ns 'clojuratica.alt)


;; ;; TODO: don't rely on dynamic scope
;; ;; TODO: reduce number of macros

;; ;; API

;; (s/fdef clojuratica.alt/wl*
;;   :args :wl/args)

;; (defn wl* [args]
;;   (let [[options-flag {:keys [opts-map body]}] (s/conform :wl/args args)]
;;     (if (= :with-options options-flag)
;;       [opts-map body]
;;       [body])))

;; ;; TODO: merge with default flags before passing down

;; (comment

;;   (options/flag?' [] :parse)
;;   (not (options/flag?' [] :no-parse))
;;   (options/flag?' [:no-parse] :no-parse)

;;   (st/instrument [`wl*])

;;   (wl* [:opts {:wl/flags [:as-function :verbose :hash-maps]}
;;         '(Dot [1 3 4] [5 4 6])])

;;   (#'convert/dispatch '(Dot [1 3 4] [5 4 6]) {})

;;   (convert/convert '{a b c d} {:flags [:no-hash-maps]})

;;   (options/flag?' flags :hash-maps)

;;   (options/flag? (options/flags-into defaults/default-options [:hash-maps])
;;                  :hash-maps)

;;   )

;; (comment

;;   '(WL :seq-fn :N (Eigensystem (SparseArray (-> ~indices (N ~vals)) [~m ~n])))

;;   ;; (WL :seq-fn :N '"Eigensystem[SparseArray[{{1, 1} -> 1, {2, 2} -> 2, {3, 3} -> 3, {1, 3} -> 4}]]")

;;   ;; FUTURE
;;   '(WL :opts {:flags [:parse/as-function :debug/verbose :convert/hash-maps]}
;;        (Dot [1 3 4] [5 4 6]))

;;   ;; CURRENT
;;   '(WL :opts {:wl/flags [:as-function :verbose :hash-maps]}
;;        (Dot [1 3 4] [5 4 6])))
