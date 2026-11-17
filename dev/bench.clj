(ns bench
  "A small benchmark of Wolframite's own overhead (converting, parsing, kernel round-trips),
  to compare performance before and after a change. Requires a local Wolfram kernel.

  Run from the project root with:

      clojure -M:jlink-jar -i dev/bench.clj -e '(bench/run!)'

  To compare with another commit, run it there too (e.g. via `git stash` or `git worktree`).
  Each case prints the median wall time of several runs, after a warm-up. Expect noise of
  ±20 % between runs, more for the cases dominated by kernel round-trips."
  (:refer-clojure :exclude [run!])
  (:require [wolframite.core :as wl]))

(defn time-ms
  "Median wall time (ms) of `n` runs of `f`, after `warm` warm-up runs."
  [f & {:keys [n warm] :or {n 15 warm 5}}]
  (dotimes [_ warm] (f))
  (let [ts (sort (repeatedly n #(let [t0 (System/nanoTime)]
                                  (f)
                                  (/ (- (System/nanoTime) t0) 1e6))))]
    (nth ts (quot n 2))))

(def cases
  [["symbolic: Expand[(a+b+c+d+e)^8] (parse many symbols)"
    #(wl/eval '(Expand (Power (+ a b c d e) 8)))]
   ["table of mixed lists, 2000 rows (parse)"
    #(wl/eval '(Table [i "x" y z] [i 1 2000]))]
   ["100x small eval (Total [1 2 3]) (per-call overhead)"
    #(dotimes [_ 100] (wl/eval '(Total [1 2 3])))]
   ["100x small eval of a string \"Total[{1,2,3}]\""
    #(dotimes [_ 100] (wl/eval "Total[{1,2,3}]"))]
   ["send 2000-element vector of booleans (convert)"
    (let [v (vec (take 2000 (cycle [true false])))]
      #(wl/eval (list 'Length v)))]
   ["1000x ->wl of [true false x] (convert only, no kernel)"
    #(dotimes [_ 1000] (wl/->wl '[true false x] {:output-fn identity}))]])

(defn run!
  "Start the kernel, time all [[cases]], print the results, and stop the kernel."
  []
  (wl/start!)
  (try
    (doseq [[label f] cases]
      (printf "%-58s %10.2f ms%n" label (time-ms f))
      (flush))
    (finally (wl/stop!)))
  (shutdown-agents))
