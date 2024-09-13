^:kindly/hide-code
(ns for-developers.index
  "An introduction to the library, aimed at the jobbing Clojure developer!"
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [wolframite.core :as wl]
    [wolframite.wolfram :as w]
    [scicloj.kindly.v4.kind :as k])
  (:import (java.io FileInputStream)
           (java.util.zip GZIPInputStream ZipInputStream)))

(k/md "# Wolframite for developers

We introduce you, the motivated Clojure developer, to using the Wolfram programming language as a Clojure library. Following some brief inspiration (why on earth should you do this?), and some getting started notes, we outline a 'real' workflow using the example of **TODO**")

; First, start & connect to a Wolfram Kernel (assuming all the common requires):
(wl/start)

(k/md "
Now, let's play with some data! But first we will need to read them in from a CSV file. The docs
tell us [how to import the first ten lines](https://reference.wolfram.com/language/ref/format/CSV.html) of a CSV:

```wolfram
Import[\"ExampleData/financialtimeseries.csv\", {\"Data\", 1 ;; 10}]
```
However, how do we write the `1 ;; 10` in Wolframite?! Let's ask!")

(wl/->clj "1 ;; 10")

(k/md "Thus, we could write it as `'(Span 1 10)` (notice the important quote!),
but we rather use our convenience functions:")

(w/Span 1 10)

(k/md "
Ideally, we'd take the easy path and follow the docs and use the very smart and flexible `Import` on our `202304_divvy_tripdata.csv.gz`.
Sadly, the current version of Wolfram is not very efficient in this and with our 400k rows it is unbearably slow. All the smartness and
auto-detection costs \uD83E\uDD37. If we read only a few rows then it is fine (±2s for 10s - 100s of rows):")

(k/table
  {:row-vectors
   (time (wl/eval (w/Import (.getAbsolutePath (io/file "notebooks/data/202304_divvy_tripdata.csv.gz"))
                            ["Data"
                             (w/Span 1 3)])))})

(k/md "
Note: [Loading the ± 400k rows file with the awesome SciCloj tooling](https://github.com/scicloj/clojure-data-scrapbook/blob/bdc46d643ac5fcdba2fb21002e269897274d9be3/projects/geography/chicago-bikes/notebooks/index.clj#L84-L88) would take ± 3.5s. How amazing is that?!

It would be nice to load the data with SciCloj / dtype.next and send it to Wolfram as data, but there is currently [no efficient
way to share large binary data](https://github.com/scicloj/wolframite/issues/113).

Thus we will need a more DIY and lower-level approach to getting the data in, leveraging `OpenRead` and `ReadList`.
Sadly, it cannot handle a gzpipped files (as far as I know) so we need to unzip it first:")

(when-not (.exists (io/file "/tmp/huge.csv"))
 (let [zip (io/file "notebooks/data/202304_divvy_tripdata.csv.gz")]
   (with-open [zis (GZIPInputStream. (io/input-stream zip))]
     (io/copy zis (io/file "/tmp/huge.csv")))))

(k/md "
Now we are ready to read the data in. We will store them into a Wolfram-side var called `data` so that we can work with them further.
")

(time
  (wl/eval (w/do (w/= 'f (w/OpenRead "/tmp/huge.csv"))
                 (w/AbsoluteTiming (w/= 'data (w/ReadList 'f
                                                          w/Word
                                                          (w/-> 'WordSeparators [","])
                                                          (w/-> 'NullWords true)
                                                          (w/-> 'RecordLists true)))
                                   #_#_(w/Table w/Record [13]) ; a row of 13 records b/c there are 13 columns
                                           (w/-> 'RecordSeparators ["," "\n"]))
                 (w/Length 'data))))



(wl/->clj "ReadList[f, Word, WordSeparators -> {\",\"}, NullWords -> True, RecordLists -> True]")

(time (wl/eval "f = OpenRead[\"/tmp/huge.csv\"];
   AbsoluteTiming[data = ReadList[f, Word, WordSeparators -> {\",\"}, NullWords -> True, RecordLists -> True]; \"done\"];
   Close[f];
   Length[data]"))

(wl/eval "Length[data]")
(wl/eval "data[[300123]]")
(def headers (->> (wl/eval "data[[1]]") ; FIXME fails if $Failed
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

(-> (wl/->wl (w/Map (w/fn [row] (list 'Internal/StringToMReal row))
                    ["123"]))
    wl/eval)


(time
 (wl/eval (->> (w/Part 'rows (w/Range 1 1000))
               (w/Map (w/fn [row] (w/Part row 9))))))

(wl/eval (w/Part 'data (w/Range 1 3)) {:flags #{:as-function}}) ; TODO (jakub) Find a better way to skip returning a value? This still transmits it from Wolfram, I assume
