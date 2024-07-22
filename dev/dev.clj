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
  (wl/eval (Dot [1 2 3] [4 5 6])) ; We have loaded all symbols as Clojure fns and thus can run this directly

  (wl/eval '(N Pi 20))
  (wl/eval (N 'Pi 20)) ; Beware: Pi must still be quoted, it is not a fn

  ;;
  ;; Built-in documentation:
  ;;
  (clojure.repl/doc Dot)

  #_:end-comment)

