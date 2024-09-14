^:kindly/hide-code
(ns for-developers.index
  "An introduction to the library, aimed at the jobbing Clojure developer!"
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [wolframite.core :as wl]
    [wolframite.wolfram :as w]
    [scicloj.kindly.v4.kind :as k])
  (:import (java.util.zip GZIPInputStream)))

(k/md "# Wolframite for developers

We introduce you, the motivated Clojure developer, to using the Wolfram programming language as a Clojure library. Following some brief inspiration (why on earth should you do this?), and some getting started notes, we outline a 'real' workflow using the example of analysing data about bike trips.")

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
Sadly, the current version of Wolfram is not very efficient in this and with our 400k rows it is unbearably slow. I assume that with the
shortened, 100k row file it wouldn't be much different. All the smartness and
auto-detection costs \uD83E\uDD37. If we read only a few rows then it is fine (±2s for 10s - 100s of rows):")

(k/table
  {:row-vectors
   (-> (w/Import (.getAbsolutePath (io/file "docs-buildtime-data/202304_divvy_tripdata_first100k.csv.gz"))
                 ["Data" (w/Span 1 3)])
       wl/eval)})

(k/md "
Note: [Loading the ± 400k rows file with the awesome SciCloj tooling](https://github.com/scicloj/clojure-data-scrapbook/blob/bdc46d643ac5fcdba2fb21002e269897274d9be3/projects/geography/chicago-bikes/notebooks/index.clj#L84-L88) would take ± 3.5s. How amazing is that?!

It would be nice to load the data with SciCloj / dtype.next and send it to Wolfram as data, but there is currently [no efficient
way to share large binary data](https://github.com/scicloj/wolframite/issues/113).

Thus we will need a more DIY and lower-level approach to getting the data in, leveraging `OpenRead` and `ReadList`.
Sadly, it cannot handle a gzpipped files (as far as I know) so we need to unzip it first:")

(when-not (.exists (io/file "/tmp/huge.csv"))
 (let [zip (io/file "docs-buildtime-data/202304_divvy_tripdata_first100k.csv.gz")]
   (with-open [zis (GZIPInputStream. (io/input-stream zip))]
     (io/copy zis (io/file "/tmp/huge.csv"))))
 :extracted)

(k/md "
Now we are ready to read the data in. We will store them into a Wolfram-side var called `data` so that we can work with them further.
")

(wl/eval (w/do (w/= 'f (w/OpenRead "/tmp/huge.csv"))
               (w/= 'data (w/ReadList 'f
                                      w/Word
                                      (w/-> w/WordSeparators [","])
                                      (w/-> w/RecordSeparators ["\n" "\r\n" "\r"])
                                      (w/-> w/NullWords true)
                                      (w/-> w/RecordLists true)))
               ;; Let's return only the length instead of all the large data:
               (w/Length 'data)))


(k/md (str "Docs: " (first (wolframite.lib.helpers/help! w/ReadList :links true))))

; We leverage the flexibility of `ReadList`, instructing it to read "Words" separated by `,` (instead of applying the normal word separating characters),
; thus reading individual column values. It reads them as records, separated by the given separators (which are the same as the default, shown here for clarity).
; I.e. each line is considered to be a record. And with `RecordLists -> True` we instruct it to put each record into a separate list and each row will thus
; become a list of values (instead of a single long list of all the column values in the whole file). Finally, we set `NullWords -> True` not to skip empty column values, so that all rows will have the same number of elements.

; (We could have instead used `ReadList` with something like `(w/Table w/Record [13]) (w/-> 'RecordSeparators ["," "\n"])`,
; telling it we have sequences of 13 records, where
; each record is separated by `,` (column separator) or `\n` (row separator). But this requires us to count the number of columns up-front.)

;; Let's extract column names:
(def headers (->> (wl/eval (w/Part 'data 1))
                  (map #(str/replace % "\"" ""))))

;; Let's have a look at row 300,123 to verify we get the data correctly:
(k/table
  {:row-vectors (map vector
                     headers
                     (wl/eval (w/Part 'data 300123)))})

(def header->idx
  "Header -> column index (1-based)"
  (zipmap headers (next (range))))

(k/md "
Now, let's parse strings into numbers so that we can use them in computations.

The recommended way is to use `ToExpression`, but it is too smart and thus slow. We will need to cheat and
use the internal `StringToMReal` instead. I got [these performance tips](https://mathematica.stackexchange.com/a/94762) from SO.")

;; Let's store the data without the header row as `rows` (and again, ensure we do not return the whole data set):
(wl/eval (w/Length (w/= 'rows (w/Drop 'data 1))))

;; Now we extract and parse the columns of interest (processing all but displaying only the first 3 here):
(k/table
  {:column-names ["Start latitude" "Start longitude"]
   :row-vectors
   (wl/eval (-> (w/= 'starts
                     (w/Map (w/fn [row] [(list 'Internal/StringToMReal (w/Part row (header->idx "start_lat")))
                                         (list 'Internal/StringToMReal (w/Part row (header->idx "start_lng")))])
                            'rows))
                (w/Part (w/Range 1 3))))})

;; For me, it took ±0.8s to extract the 2 columns as text and 1.5s to parse them into numbers. With `ToExpression` it would take ±5s.

;; **TO BE CONTINUED** - featuring GeoDistance & more!