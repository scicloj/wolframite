(ns dev
  (:refer-clojure
    ;; Exclude symbols also used by Wolfram:
   :exclude [Byte Character Integer Number Short String Thread])
  (:require
   [wolframite.api.v1 :as wl]))

(println "Initializing Wolframite...")
(wl/start!)

(comment ;; Get Started!

  ;;
  ;; Evaluation
  ;;

  ;; Use eval with a quoted Wolfram-as-clojure-data expression (`Fn[..]` becoming `(Fn ..)`):
  (wl/! '(Dot [1 2 3] [4 5 6])) ; Wolfram: `Dot[{1, 2, 3}, {4, 5, 6}]]`
  (wl/! (Dot [1 2 3] [4 5 6])) ; We have loaded all symbols as Clojure fns and thus can run this directly

  (wl/! '(N Pi 20))
  (wl/! (N 'Pi 20)) ; Beware: Pi must still be quoted, it is not a fn

  ;;
  ;; Built-in documentation:
  ;;
  (clojure.repl/doc Dot)

  #_:end-comment)

