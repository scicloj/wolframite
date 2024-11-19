(ns wolframite.api.v1
  (:refer-clojure :exclude [eval])
  (:require [wolframite.core :as core]
            [wolframite.lib.helpers :as helper]))

(def start!
  "Initialize Wolframite and start! the underlying Wolfram Kernel - required once before you make any eval calls.

  - `opts` - a map that is passed to `eval` and other functions used in the convert-evaluate-parse
             cycle, which may contain, among others:
     - `:aliases` - a map from a custom symbol to a symbol Wolfram understands, such as the built-in
        `{'* 'Times, ...}` - added to the default aliases from `wolframite.runtime.defaults/all-aliases`
        and used when converting symbols at _function position_ to Wolfram expressions.
        You may add your own ones, to be able to use them in your Wolfram expressions and get those
        translated into Wolfram ones. See Wolframite docs.
     -  `:flags [kwd ...]` - various on/off toggles for how Wolframite processes inputs/results,
        passed e.g. to the `custom-parse` multimethod; see also wolframite.flags

  See also [[stop!]]"
  core/start!)
(def restart!
  "Same as calling [[stop!]] and then [[start!]]."
  core/restart!)
(def stop!
  "Sends a request to the kernel to shut down.

  See https://reference.wolfram.com/language/JLink/ref/java/com/wolfram/jlink/KernelLink.html#terminateKernel()"
  core/stop!)

(def ->wl
  "Convert Clojure forms to instances of Wolfram's Expr class.
  Generally useful, especially for working with graphics - or for troubleshooting
  what will be sent to Wolfram for evaluation."
  core/->wl)

(def ->clj
  "Turn the given Wolfram expression string into its Clojure data structure form.

  Ex.: `(->clj \"Power[2,3]\") ; => (Power 2 3)`"
  core/->clj)

(def eval
  "Evaluate the given Wolfram expression (a string, or a Clojure data) and return the result as Clojure data.

   Args:
   - `opts` - same as those for [[start!]], especially `:aliases` and `:flags` (see
      wolframite.flags)

    Example:
    ```clojure
    (wl/eval \"Plus[1,2]\")
    ; => 3
    (wl/eval '(Plus 1 2))`
    ; => 3
    ```

    Tip: Use [[->wl]] to look at the final expression that would be sent to Wolfram for evaluation."
  core/eval)

(def !
  "Alias of core/eval. Evaluate the given Wolfram expression (a string, or a Clojure data) and return the result as Clojure data.

   Args:
   - `opts` - same as those for [[start!]], especially `:aliases` and `:flags` (see
      wolframite.flags)

    Example:
    ```clojure
    (wl/eval \"Plus[1,2]\")
    ; => 3
    (wl/eval '(Plus 1 2))`
    ; => 3
    ```

    Tip: Use [[->wl]] to look at the final expression that would be sent to Wolfram for evaluation."
  core/eval)

(def <<!
  "A Wolfram-like alias to load-package!. An extended version of Wolfram's 'Get'. Gets a Wolfram package and makes the constants/functions etc. accessible via a Clojure namespace (the given `alias`, by default the same as `context`)."
  core/load-package!)

(def help!
  "Get web-based help for a given symbol or form.

  If a form is given, this will operate on all symbols found in the form and, by default, opens a web browser with relevant documentation. You can pass `links true` if you just want URLs."
  helper/help!)

(comment
  (start!)
  (! '(+ 1 1))
  (help! '(+ 1 1)) ;; => Doesn't seem to be opening a browser for me?
  )
