(ns dev
  (:refer-clojure
    ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.core :as wl]))

(println "Initializing Wolframite...")
(wl/start)

(comment ;; Get Started!

  ;;
  ;; Evaluation
  ;;

  ;; Use eval with a quoted Wolfram-as-clojure-data expression (`Fn[..]` becoming `(Fn ..)`):
  (wl/eval '(Dot [1 2 3] [4 5 6])) ; Wolfram: `Dot[{1, 2, 3}, {4, 5, 6}]]`

  (wl/eval '(N Pi 20))

  ;;
  ;; Built-in documentation:
  ;;
  (clojure.repl/doc 'Dot)

  #_:end-comment)

