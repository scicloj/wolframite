# Dev docs

## Notes 2024-01-18

* Rename ns convert and parse to st. like clj-to-wolfram and wolfram-to-clj ?
* Add a diagram documenting the eval pipeline and the relations of the other modules
     
## Open Questions
* Should we use 'proper' logging rather than just printing strings?

## Common ns aliases

```clojure
(:require
  [wolframite.core :as wl])
```

## TODO

* [ ] Add tests for parse, convert, key fns
* [x] FIXME: `load-all-symbols` should make each symbol into a var that just contains a symbol, and requires an explicit call to `wl/eval`. Justification: 1) wrapping each symbol with a fn breaks expression that use them as values, such as `(Plus 1 Pi)` (we'd try to add 1 to a clojure lambda fn), 2) Nested expressions result in multiple calls to Wolfram: e.g. `(Plus 1 (Plus 2 3))` would evaluate _two_ expressions: `Plus[2, 3]]` and `Plus[1, 5]]`, while we only want to evaluate once. Perhaps keep a fn to enable explicit turning of selected vars into functions? 
* [x] Rename namespaces, docs to Wolframite
* [x] Replace uses of wl/wl with wl/eval (= standardize on a single, understandable one) - in demo etc
* [x] Explore, Leverage for docs dev/explainer.clj, notebook.demo 
* [x] Get rid of the custom-parse flag requirement
* [ ] Get better errors from the Kernel: Running `(First (WolframLanguageData))` when offline returns `(Entity "WolframLanguageSymbol" "$Aborted")` while in W.Eng. it also prints a bunch of useful error info; can we get hold of it? Aslo, should we turn the $Aborted into an exception?! See below:

```wolram
In[6]:= First[WolframLanguageData[]]                                                              

EntityValue::conopen: 
   Using EntityValue requires internet connectivity. Please check your network connection. You
     may need to configure your firewall program or set a proxy in the Internet Connectivity tab
     of the Preferences dialog.

EntityValue::nodat: Unable to download data. Some or all results may be missing.

EntityValue::outdcache: Using potentially outdated cached values.

Out[6]= Entity[WolframLanguageSymbol, $Aborted]
```

Similar, when offline:

```wolfram
In[9]:= WolframAlpha["How many licks does it take to get to the center of a Tootsie Pop?"]        

URLFetch::invurl: Internal`HouseKeep[https://api.wolframalpha.com/v1/query.jsp, 
     {input -> How%20many%20licks%20does%20it%20take%20to%20get%20to%20the%20center%20of%20a%20To
        otsie%20Pop%3F, async -> false, format -> minput,plaintext, <<12>>, uuid -> None}] is not
     a valid URL
```


```wolfram
In[1]:= Subtract[1]

Subtract::argr: Subtract called with 1 argument; 2 arguments are expected.

## Open questions

* Why cannot I start three Wolframite REPLs on the same PC? (The 3rd fails w/ "MathLink connection was lost")
* The flags - do we need all, how to understand them, ...?
* How well is WL's Associative supported in parse<>convert? (There is some flag related to this, some TODO/FIXME comments, ...)
* How to install packages? (see `Needs` wl fn)
* What are `defaults/clojure-scope-aliases` about?
* How to load .wl file into a REPL? (Thomas may know)
```

### Performance

* `graphics/show!` seems pretty slow - multiple seconds to minutes to render a result
* Loading symbol names + docs very slow - https://community.wolfram.com/groups/-/m/t/3071114?p_p_auth=Lqs4farl


## How does it work

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

* `wl/load-all-symbols` - intern all Wolf. symbols into the given ns
* `(parse/parse-fn sym opts)` - return a "proxy fn", which will invoke a Wolfram fn of the given name, transforming its arguments from Clojure data to Wolfram expressions and the opposite on the result.
  * Powered by `clojuratica.base.cep/cep`, the convert-eval-parse fn
* Naming of options: the convention for opts passed through the pipeline si that the keyword namespace would indicate the stage of the pipeline (convert, parse, eval).

## Aliases
### To document
- **<->Power (new convention)
- **2->(Power ... 2)
