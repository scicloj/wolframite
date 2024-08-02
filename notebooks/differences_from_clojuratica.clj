(ns differences-from-clojuratica
  "Collecting differences here until we have enough to justify a page in the docs."
  (:require
   [scicloj.kindly.v4.kind :as k]))

(k/md "Note, separating expression chains from automatic eval was done for efficiency. Previously, all Wolfram expressions were individually evaluated with separate kernel calls and this was considered a performance problem.")
