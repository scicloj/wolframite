^:kindly/hide-code
(ns for-developers.index
  "An introduction to the library, aimed at the jobbing Clojure developer!"
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [wolframite.core :as wl]
    [wolframite.wolfram :as w]
    [scicloj.kindly.v4.kind :as k]))

(k/md "# Wolframite for developers

## TL;DR

We introduce you, the motivated Clojure developer, to using the Wolfram programming language as a Clojure library. Following some brief inspiration (why on earth should you do this?), and some getting started notes, we outline a 'real' workflow using the example of ...")

(wl/start) ; start & connect to a Wolfram Kernel


(k/md "Let's read a CSV file. The docs
tell us [how to import the first ten lines](https://reference.wolfram.com/language/ref/format/CSV.html) of a CSV:

```wolfram
Import[\"ExampleData/financialtimeseries.csv\", {\"Data\", 1 ;; 10}]
```

However, how do we write the `1 ;; 10` in Wolframite?! Let's ask!")

(wl/->clj "1 ;; 10")

(k/md "Thus, we could write it as `'(Span 1 10)` (notice the important quote!),
but we rather user our convenience functions:")

(w/Span 1 10)

(def R (time (wl/eval (w/Import (.getAbsolutePath (io/file "notebooks/data/202304_divvy_tripdata.csv.gz"))
                                [["GZIP" "CSV"]
                                 (w/Span 1 10)])))) ; 2s for 10 rows on my PC

(k/md "Note: [Loading the ± 400k rows file with the awesome SciCloj tooling](https://github.com/scicloj/clojure-data-scrapbook/blob/bdc46d643ac5fcdba2fb21002e269897274d9be3/projects/geography/chicago-bikes/notebooks/index.clj#L84-L88) would take ± 3.5s. How amazing is that?!")

;(wl/eval (w/Import "/Users/holyjak/tmp/delme/demo.csv.gz"
;                   ["GZIP", "CSV"])) ; works
;; TODO 1.5s for 3 rows, 1.8s for 100
;; TODO ols 3+4 are "2023-04-02 08:37:28" => parse?
#_ FIXME ; Import takes too long, we need to do a simpler Open + ReadList of records; TODO How to parse parts of interest?
;(wl/->wl (w/Import (.getAbsolutePath (io/file "notebooks/data/202304_divvy_tripdata.csv.gz"))
;                   ["Data" (w/Span 1 10000)], ; import as 2D data, but only rows 1...3 [inc. headers]
;                   ;; Notice that the format (gzipped CSV) is detected automatically
;                   (w/-> "HeaderLines" 1)))

(time (wl/eval "f = OpenRead[\"/tmp/huge.csv\"];
   AbsoluteTiming[data = ReadList[f, Table[Record, {13}], RecordSeparators -> {\",\", \"\\n\"}]; \"done\"];
   Close[f];
   Length[data]"))

(time (wl/eval "f = OpenRead[\"/tmp/huge.csv\"];
   AbsoluteTiming[data = ReadList[f, Word, WordSeparators -> {\",\"}, NullWords -> True, RecordLists -> True]; \"done\"];
   Close[f];
   Length[data]"))

(wl/eval "Length[data]")
(wl/eval "data[[300123]]")
(def headers (->> (wl/eval "data[[1]]")
                  (map #(str/replace % "\"" ""))))
(def header->idx (zipmap headers (next (range))))


(-> (wl/eval "data[[1]]") first (str/replace "\"" ""))

(wl/eval (w/Length (w/= 'rows (w/Drop 'data 1)))) ; drop the header row
(time
  (wl/eval (-> (w/= 'starts
                    ;; Parsing speed tips: see https://mathematica.stackexchange.com/a/94762
                    (w/Map (w/fn [row] ['(Internal/StringToMReal (Part row 10)) ; ugly to use internal stuff, but much faster
                                        #_(w/ToExpression (w/Part row 9 #_(header->idx "start_lat")))])
                           'rows))
               (w/Part (w/Range 1 3))))) ; 0.8s to just extract 2 columns; 5s w/ parsing to numbers via ToExpression

(-> (wl/->wl '(Map (Function [x] [(Internal/StringToMReal x) (Internal/StringToMReal x)])
                   ["123"]))
    wl/eval)

(-> (wl/->wl (w/Map (w/fn [row] (Internal/StringToMReal row)) ;; FIXME does not work ,but the above thus
                    ["123"]))
    wl/eval)


(time
 (wl/eval (->> (w/Part 'rows (w/Range 1 1000))
               (w/Map (w/fn [row] (w/Part row 9))))))

(wl/eval (w/Part 'data (w/Range 1 3)) {:flags #{:as-function}}) ; TODO (jakub) Find a better way to skip returning a value? This still transmits it from Wolfram, I assume
