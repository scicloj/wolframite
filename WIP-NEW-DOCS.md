# Dev docs

## Common ns aliases

```clojure
(:require
  [clojuratica.core/->wl! :as wl])
```

## Open questions

* What is the `parse` ns about? Turn `com.wolfram.jlink.Expr` to Clojure data? Thus likely
  the opposite of `clojuratica.core/->wl!` ?
* Q: What does `wl/wl` do, vs. `wl/eval` ? A: Eval is alias to wl, an evaluator instance.
* Why do we want to support `:parse/custom-parse-symbols` (and not simply support either a sym or a set of symbols as the dispatch-val?) Also, why do we force the user to set the `:custom-parse` flag? Is always 
 invoking a multimethod considered too expensive?
* The flags - do we need all, how to understand them, ...?
* How well is WL's Associative supported in parse<>convert? (There is some flag related to this)
* How to install packages?
* What are `defaults/clojure-scope-aliases` about?
* How to load .wl file into a REPL?
 
### Performance

* `graphics/show!` seems pretty slow - multiple seconds to minutes to render a result
* Loading symbol names + docs very slow - https://community.wolfram.com/groups/-/m/t/3071114?p_p_auth=Lqs4farl
* 

## How does it work

We talk to a locally installed Wolfram Engine with the Wolfram library JLink for Wolfram <-> Java interop.

## Setup

Finding JLink: Wolframite finds it at the OS-default location (see `clojuratica.jlink/add-jlink-to-classpath!`) at start, e.g., on OSX, it would look for `/Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents/SystemFiles/Links/JLink/JLink.jar` - look for `Adding path to classpath: <path>` in the stdout. You can override the path by setting the env var `JLINK_JAR_PATH` and influence it by setting either of `MATHEMATICA_INSTALL_PATH`, `WOLFRAM_INSTALL_PATH`.

## Learning Wolframite

Go through `dev/explainer.clj`

### Basic usage

```clojure
(require '[clojuratica.core :as wl])
(wl/eval <symbolic expr or string>)
```

#### The 3 ways of invoking Wolfram

1. Wolfram lang as a string: `(wl/eval "Plus[1,2]")`
2. Wolfram lang as Clojure data: `(wl/eval '(Plus 1 2))`
3. Wolfram functions interned into a workspace as Clojure functions: `(do (wl/load-all-symbols 'w) (w/Plus 1 2))`

On interning: this essentially creates a "proxy" function of the same name as a Wolfram function, which will convert the passed-in Clojure expression to the JLink `Expr`, send it to a Wolfram Kernel for evaluation, and parse the result back into Clojure data. Beware that `wl/load-all-symbols` may take 10s of seconds - some minutes.

#### Wolfram expressions <-> Clojure EDN

```clojure
"Map[Function[{x}, x + 1], {1, 2, 3}]" :-> '(Map (fn [x] (+ x 1)) [1 2 3])

;Function[{x}, StringJoin["Hi ", x, " there!"]] 
:-> '(Function [x] (StringJoin "Hi " x " there!"))
```

## Internals

### How does it work internally

#### The "CEP pipeline": convert - evaluate - parse

Evaluating an expression with Wolfram consists of three stages:

1. Convert (`convert/convert`) the given expression from Clojure to Wolfram (i.e. to an instance of jlink.Expr)
2. Evaluate (`evaluate/evaluate`) the jlink expression via the remote Wolfram Kernel
3. Parse (`parse/parse`) the response back from a jlink expression to Clojure data

See `clojuratica.base.cep/cep`.

#### Parse

Modify how Wolfram response is parsed into Clojure data - override `clojuratica.base.parse/custom-parse` with the symbol of interest. 

### Various

* `wl/clj-intern` - intern a Wolfram function (e.g. `'Plus`) into a clj ns
* `wl/load-all-symbols` - intern all Wolf. symbols into the given ns

* `(parse/parse-fn sym opts)` - return a "proxy fn", which will invoke a Wolfram fn of the given name, transforming its arguments from Clojure data to Wolfram expressions and the opposite on the result.
** Powered by `clojuratica.base.cep/cep`, the convert-eval-parse fn