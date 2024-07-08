# Wolframite

[![Clojars Project](https://img.shields.io/clojars/v/org.scicloj/wolframite.svg)](https://clojars.org/org.scicloj/wolframite)

An interface between Clojure and the Wolfram Language (Supports [Mathematica](https://www.wolfram.com/mathematica/) and [Wolfram Engine](https://www.wolfram.com/engine/) ).

## Status

**Wolframite is currently (Q2/2024) under active development again. You can [keep track of what is happening in this discussion](https://github.com/scicloj/wolframite/discussions/17).**

## What is Wolframite? ##

Wolframite (formerly Clojuratica) brings together two of today's most exciting tools for high-performance, parallel computation.

[Clojure](http://clojure.org) is a dynamic functional programming language with a compelling approach to concurrency and state, a strong cast of persistent immutable data structures, and a growing reputation for doing all the right things.
[Wolfram Mathematica](https://www.wolfram.com/mathematica/) is arguably the world's most powerful integrated tool for numerical computation, symbolic mathematics, optimization, and visualization and is build on top of its own splendid functional programming language, [Wolfram Language](https://www.wolfram.com/language/).

By linking the two:

* Wolframite lets you **write and evaluate Wolfram/Mathematica code in Clojure** with full **syntactic integration**. Now Clojure programs can take advantage of Wolfram's enormous range of numerical and symbolic mathematics algorithms and fast matrix algebra routines.
* Wolframite provides the **seamless and transparent translation of native data structures** between Clojure and Wolfram. This includes high-precision numbers, matricies, N-dimensional arrays, and evaluated and unevaluated Mathematica expressions and formulae.
* Wolframite lets you **write Wolfram as if it was Clojure** by providing Clojure functions and vars for all Wolfram symbols, including docstrings and autocompletion in your favorite IDE
* [Tentative] Wolframite facilitates **the "Clojurization" of Wolfram's existing parallel-computing capabilities.** Wolfram is not designed for threads or concurrency. It has excellent support for parallel computation, but parallel evaluations are initiated from a single-threaded master kernel which blocks until all parallel evaluations return. By contrast, Wolframite includes a concurrency framework that lets multiple Clojure threads execute Wolfram expressions without blocking others. Now it is easy to run a simulation in Clojure with 1,000 independent threads asynchronously evaluating processor-intensive expressions in Wolfram. The computations will be farmed out adaptively and transparently to however many Wolfram kernels are available on any number of processor cores, either locally or across a cluster, grid, or network.
  * Notice that you cannot run more Wolfram kernels than your license allows (see `wolframite/kernel-info!`) 

Wolframite is open-source and targeted at applications in scientific computing, computational economics, finance, and other fields that rely on the combination of parallelized simulation and high-performance number-crunching. Wolframite gives the programmer access to Clojure's most cutting-edge features--easy concurrency and multithreading, immutable persistent data structures, and software transactional memory---alongside Wolfram's easy-to-use algorithms for numerics, symbolic mathematics, optimization, statistics, visualization, and image-processing.

## Usage

### Prerequisites:

#### Clojure

First, if you haven't already, install the [Clojure CLI toolchain](https://clojure.org/guides/getting_started) (homebrew is a great way to do this if you're on Mac or Linux, but you can just as easily use the installation scripts if you prefer).

#### Mathematica or Wolfram Engine

Next, obviously, you'll need to ensure that you have Wolfram Engine or Mathematica installed and your license (free for W. E.) registered - make sure you can run these tools on their own _before_ trying Wolframite.

First of all, you need to initialize a connection to a Wolfram/Mathematica kernel, like this:

```clojure
(wolframite.core/init!)
```
This should also find and load the JLink JAR included with your installation. Watch stdout for an INFO log message (via clojure.tools.logging) like:

> === Adding path to classpath: /Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents/SystemFiles/Links/JLink/JLink.jar ===

However, sometimes Wolframite may fail to find the correct path automatically and needs your help. You can set the `WOLFRAM_INSTALL_PATH` environment variable or Java system property (the latter takes priority) to point to the correct location. Example:

```shell
export WOLFRAM_INSTALL_PATH=/opt/mathematica/13.1
```

### Getting started

Start a REPL with Wolframite on the classpath, then initialize it:

```clojure
(require '[wolframite.core :as wl] 
         '[wolframite.wolfram :as w]) ; Wolfram symbols as Clojure vars / fns
;; Initialize
(wl/init!) ; => nil
;; Use it:
(wl/eval (w/Dot [1 2 3] [4 5 6]))
;=> 32
```

More examples

```clojure
(wl/eval (w/D (w/Power 'x 2) 'x))
;=> (* 2 x)
(wl/eval (w/ChemicalData "Ethanol" "MolarMass"))
;=> (Quantity 46.069M (* "Grams" (Power "Moles" -1)))

;; Accessing WlframAlpha
(wl/eval (w/WolframAlpha "How many licks does it take to get to the center of a Tootsie Pop?")) ; BEWARE: must be online
;=> [(-> [["Input" 1] "Plaintext"] "How many licks does it take to get to the Tootsie Roll center of a Tootsie Pop?") (-> [["Result" 1] "Plaintext"] "3481\n(according to student researchers at the University of Cambridge)")]

(wl/eval (w/N w/Pi 20))
;=> 3.141592653589793238462643383279502884197169399375105820285M

(wl/eval (w/Map (w/fn [x] (w/Sqrt x)) [4 16]))
;=> [2 4]
```

TIP: Cursive - teach it to resolve `w/fn` as `clojure.core/fn`.

NOTE: The `wolframite.wolfram` (`w`) ns has vars for all Wolfram symbols at the time of the last release. Check `w/*wolfram-kernel-name*` for kernel type/version and run `(wolframite.impl.wolfram-syms.write-ns/write-ns!)`
to generate your own wolfram ns with whatever additional symbols your Wolfram/Mathematice has, and/or with custom "aliases". 

#### Learning Wolframite

Read through and play with [explainer.clj](dev%2Fexplainer.clj) and [demo.clj](dev%2Fdemo.clj), which demonstrate most of Wolframite's features and what you can do with Wolfram.

#### Customizing Wolframite

A big advantage of Wolframite (as opposed to its earlier incarnations) is that we can now individually tailor the user experience at the level of initialization,
```clojure
(wl/init! {:aliases '{** Power}})
(wl/eval '(** 2 5)) ; => 32
```
,
and function call,
```clojure
(wl/init!)
(wl/eval '(** 2 5) {:aliases '{** Power}}) ; => 32
```
. Use it how you want to!

TIP: You can also get convenience vars for your aliases in `wolframite.wolfram` by running
something like
`(wolframite.impl.wolfram-syms.write-ns/write-ns! <path> {:aliases '{** Power}})`. After you load
the file, you'll be able to use `(wl/eval (w/** 2 5) {:aliases '{** Power}})`.

### Clerk Integration

Example usage: (watching for changes in a folder)

```
user> (require '[clojuratica.tools.clerk-helper :as ch])
user> (ch/clerk-watch! ["dev/notebook"])
```

* Open dev/notebook/demo.clj, make a change and save.
* Open `localhost:7777` in the browser

### How does it work?

You compose Wolfram expressions using the convenience functions and vars from `wolframite.wolfram`. These are then turned first into a symbolic representation of themselves and later into a tree of JLink `Expr` objects and sent to a Wolfram kernel subprocess (started by `wl/init!`) for evaluation. The result is translated back from jlink.Expr into a Clojure form. This translation allows for some additional convenience logic, such as supporting `w/*` instead of `Times`.

## Dependencies

Wolframite requires Wolfram's Java integration library JLink, which is currently only available with a Wolfram Engine or Mathematica installation. It will also need to know where the `WolframKernel` / `MathKernel` executable is, in order to be able to start the external evaluation kernel process. Normally, `wl/init!` should be able to find these automatically, if you installed either into a standard location on Mac, Linux or Windows. However, if necessary, you can specify either with env variables / sys properties - see Prerequisites above.

## Development

### Running tests

To run tests from the command line, you need to add JLink to the classpath (only REPL supports dynamically loading jars) -
create a `./symlink-jlink.jar` symlink and then run the tests:

```shell
clojure -X:test-run
```

### Deployment

Build the jar with `clojure -T:build jar` then deploy with
`env CLOJARS_USERNAME=<tbd> CLOJARS_PASSWORD=<clojars-token> clojure -T:build deploy`

Note: You need to log in to Clojars and generate a deployment token. You also need to be added to
the SciCloj group there by an admin.

#### Documentation

Documentation is written as literal programming sources in the `notebooks` directory and turned into HTML
under `docs` using [Clay](https://scicloj.github.io/clay/)
and [Quarto](https://quarto.org/).

To render a single namespace/page, require Clay and run `(clay/make! {:source-path "<path to the file>""})`. Tip: You can also do this without quarto - just add `:run-quarto false` to the options.

To build the whole site, run `clojure -T:build build-site`.

## Authors

The original Clojuratica was created by Garth Sheldon-Coulson, a graduate student at the Massachusetts Institute of Technology and Harvard Law School. See the [Community](http://clojuratica.weebly.com/community.html) page to find out how to contribute to Clojuratica, suggest features, report bugs, or ask general questions.

Ongoing maintenance and development over the years have been thanks to
* [Steve Chan](https://github.com/chanshunli)
* [Dan Farmer](https://github.com/dfarmer)
* [Norman Richards](https://github.com/orb)

Clojuratica has been turned into Wolframite and further maintained by:

* Pawel Ceranka
* Thomas Clark
* [Jakub Holý](https://holyjak.cz)

The project is now being maintained as part of the [SciCloj](https://github.com/scicloj) project.

## License

Distributed under the Eclipse Public License either version 2.0 or (at
your option) any later version.

## Legal

The product names used in this website are for identification purposes only.
All trademarks and registered trademarks, including "Wolfram Mathematica," are the property of their respective owners.
Wolframite is not a product of Wolfram Research.
The software on this site is provided "as-is," without any express or implied warranty.
