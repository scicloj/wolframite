(ns dev
  (:require [clojuratica.core :as wl]))

(wl/load-all-symbols (symbol (ns-name *ns*)))

(comment ;; Get Started!

  ;; * Init
  ;;

  (wl/eval '(Dot [1 2 3] [4 5 6]))
  (Dot [1 2 3] [4 5 6])
  (wl/eval '(N Pi 20))
  (N 'Pi 20) ; Pi must still be quoted, it is not a fn
  (do Pi)

  (clojure.repl/doc EntityProperties)
  (clojure.repl/doc EntityValue)
  (clojure.repl/doc WolframLanguageData)
  (def WLD (WolframLanguageData))


  (wl/eval '(EntityTypeName Pi))

  (EntityProperties 'WolframLanguageSymbol)

  (EntityType 'WolframLanguageSymbol)

  (->> (ns-publics *ns*)
       keys
       (filter #(re-matches #".*Entity.*" (name %)))
       sort
       )

  #_:end-comment)

