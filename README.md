# Wolframite

[![Clojars download](https://img.shields.io/clojars/v/org.scicloj/wolframite.svg)](https://clojars.org/org.scicloj/wolframite) [Wolframite sources](https://github.com/scicloj/wolframite/)

An interface between Clojure and the Wolfram Language (Supports [Mathematica](https://www.wolfram.com/mathematica/) and [Wolfram Engine](https://www.wolfram.com/engine/)).

## Status

Stable

## What is Wolframite? ##

Wolframite (formerly Clojuratica) brings together two of today's most exciting tools for high-performance, parallel computation.

[Clojure](http://clojure.org) is a dynamic functional programming language with a compelling approach to concurrency and state, a strong cast of persistent immutable data structures, and a growing reputation for doing all the right things.
[Wolfram Mathematica](https://www.wolfram.com/mathematica/) is arguably the world's most powerful integrated tool for numerical computation, symbolic mathematics, optimization, and visualization and is build on top of its own splendid functional programming language, [Wolfram Language](https://www.wolfram.com/language/).

By linking the two:

* Wolframite lets you **write and evaluate Wolfram/Mathematica code in Clojure** with full **syntactic integration**. Now Clojure programs can take advantage of Wolfram's enormous range of numerical and symbolic mathematics algorithms and fast matrix algebra routines.
* Wolframite provides the **seamless and transparent translation of native data structures** between Clojure and Wolfram. This includes high-precision numbers, matricies, N-dimensional arrays, and evaluated and unevaluated Mathematica expressions and formulae.
* Wolframite lets you **write Wolfram as if it was Clojure** by providing Clojure functions and vars for all Wolfram symbols, including docstrings and autocompletion in your favorite IDE
* [Tentative] Wolframite facilitates **the "Clojurization" of Wolfram's existing parallel-computing capabilities.** Wolfram is not designed for threads or concurrency. It has excellent support for parallel computation, but parallel evaluations are initiated from a single-threaded master kernel which blocks until all parallel evaluations return. By contrast, Wolframite includes a concurrency framework that lets multiple Clojure threads execute Wolfram expressions without blocking others. Now it is easy to run a simulation in Clojure with 1,000 independent threads asynchronously evaluating processor-intensive expressions in Wolfram. The computations will be farmed out adaptively and transparently to however many Wolfram kernels are available on any number of processor cores, either locally or across a cluster, grid, or network.
  * Notice that you cannot run more Wolfram kernels than your license allows (see the function `(wolframite.core/kernel-info!)`) 

Wolframite is open-source and targeted at applications in scientific computing, computational economics, finance, and other fields that rely on the combination of parallelized simulation and high-performance number-crunching. Wolframite gives the programmer access to Clojure's most cutting-edge features--easy concurrency and multithreading, immutable persistent data structures, and software transactional memory---alongside Wolfram's easy-to-use algorithms for numerics, symbolic mathematics, optimization, statistics, visualization, and image-processing.

Wolfram / Mathematica itself is a commercial product and requires a license. Though Wolfram Engine is [free to use in development](https://www.wolfram.com/engine/free-license/).

## Documentation

No matter what is your level of experience with either Clojure or Wolfram, we have you covered!

While this page provides a brief overview of Wolframite assuming some level of knowledge of each of the systems, we have also tutorials targeted at [scientists new to Clojure](https://scicloj.github.io/wolframite/for_scientists.index.html) 
and at [Clojure developers new to Wolfram](https://scicloj.github.io/wolframite/for_developers.index.html). However, you should try to read through this page, the [Quickstart](https://scicloj.github.io/wolframite/quickstart.html), and browse through [Understanding Wolframite](https://scicloj.github.io/wolframite/understanding_wolframite.html) before diving into the tutorials. If you have never seen Clojure before, you may want to start with a quick look at our [Clojure Primer](https://scicloj.github.io/wolframite/clojure_primer.index.html) to understand better the rest of the documentation. Remember: you don't need to learn or remember all at once.

## Usage

### Prerequisites:

#### Clojure

First, if you haven't already, install the [Clojure CLI toolchain](https://clojure.org/guides/getting_started) (homebrew is a great way to do this if you're on Mac or Linux, but you can just as easily use the installation scripts if you prefer).

#### Wolfram Mathematica or Wolfram Engine

Next, obviously, you'll need to ensure that you have Wolfram Engine or the Wolfram desktop application (formerly called Mathematica) installed and your license registered. Make sure you can run these tools on their own _before_ trying Wolframite.

First of all, you need to initialize a connection to a Wolfram/Mathematica kernel, like this:

```clojure
(wolframite.api.v1/start!)
```
This should also find and load the JLink JAR included with your installation. Watch stdout for an INFO log message (via clojure.tools.logging) like:

> === Adding path to classpath: /Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents/SystemFiles/Links/JLink/JLink.jar ===

However, sometimes Wolframite may fail to find the correct path automatically and needs your help. You can set the `WOLFRAM_INSTALL_PATH` environment variable or Java system property (the latter takes priority) to point to the correct location. Examples:

```shell
export WOLFRAM_INSTALL_PATH=/opt/mathematica/13.1
export WOLFRAM_INSTALL_PATH="/Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents"
```

### Getting started

Start a [Clojure REPL](https://clojure.org/guides/repl/introduction) with Wolframite on the classpath:

```shell
clj -Sdeps '{:deps {org.scicloj/wolframite {:mvn/version "1.0.0-SNAPSHOT"}}}'
```

and try it out:

```clojure
(require '[wolframite.api.v1 :as wl] 
         '[wolframite.wolfram :as w :refer :all  ; Wolfram symbols as Clojure vars / fns
           :exclude [* + - -> / < <= = == > >= fn
                     Byte Character Integer Number Short String Thread]])
;; Initialize
(wl/start!) ; => {:status :ok, :wolfram-version 14.1, :started? true}
;; Use it:
(wl/! (Dot [2 2 4] [4 5 6]))
;=> 42
```

More examples:

```clojure
(wl/! (D (Power 'x 2) 'x)) ; derivative
;=> (* 2 x)
(wl/! (ChemicalData "Ethanol" "MolarMass"))
;=> (Quantity 46.069M (* "Grams" (Power "Moles" -1)))

;; Accessing WolframAlpha (BEWARE: must be online)
(wl/! (WolframAlpha "How many licks does it take to get to the center of a Tootsie Pop?"))
;=> [(-> [["Input" 1] "Plaintext"] "How many licks does it take to get to the Tootsie Roll
;   center of a Tootsie Pop?") (-> [["Result" 1] "Plaintext"] "3481\n(according to student
;   researchers at the University of Cambridge)")]

(wl/! (N Pi 20)) ; numerical value with 20 digit precision
;=> 3.141592653589793238462643383279502884197169399375105820285M

(wl/! (Map (w/fn [x] (Sqrt x)) [4 16]))
;=> [2 4]
```

TIP: Cursive - teach it to resolve `w/fn` as `clojure.core/fn`.

NOTE: The `wolframite.wolfram` (`w`) namespace has vars for all Wolfram symbols at the time of the last release of Wolframite. Check `w/*wolfram-kernel-name*` for kernel type/version and run `(wolframite.impl.wolfram-syms.write-ns/write-ns!)`
to generate your own wolfram ns with whatever additional symbols your Wolfram/Mathematice has, and/or with custom "aliases". 

### Clerk Integration

We primarily use [Clay](https://scicloj.github.io/clay/) as our notebook tool, but there is also experimental support for [Clerk](https://github.com/nextjournal/clerk). 

Example usage: (watching for changes in a folder)

```
user> (require '[wolframite.tools.clerk-helper :as ch])
user> (ch/clerk-watch! ["dev/notebook"])
```

* Open `dev/notebook/quickstart.clj`, make a change and save.
* Open `localhost:7777` in the browser

### How does it work?

You compose Wolfram expressions using the convenience functions and vars from `wolframite.wolfram`. These are then turned first into a symbolic representation of themselves and later into a tree of JLink `Expr` objects and sent to a Wolfram kernel subprocess (started by `wl/start!`) for evaluation. The result is translated back from jlink.Expr into a Clojure form. This translation allows for some additional convenience logic, such as supporting `w/*` instead of `Times`.

## Development

### Running tests

To run tests from the command line, you need to add JLink to the classpath (only REPL supports dynamically loading jars) -
create a `./symlink-jlink.jar` symlink and then run the tests:

```shell
clojure -X:run-tests
```

### Deployment

If required, update version in `deps.edn` under `:aliases - :neil - :project - :version` (possibly using `neil`).

then build the jar with `clojure -T:build jar` then deploy with
`env CLOJARS_USERNAME=<tbd> CLOJARS_PASSWORD=<clojars-token> clojure -T:build deploy`.

Note: You need to log in to Clojars and generate a deployment token. You also need to be added to
the SciCloj group there by an admin.

Consider studying Wolfram's guide [Writing Java Programs That Use the Wolfram Language](https://reference.wolfram.com/language/JLink/tutorial/WritingJavaProgramsThatUseTheWolframLanguage.html) and 
[WSTP and External Program Communication](https://reference.wolfram.com/language/tutorial/WSTPAndExternalProgramCommunicationOverview.html)
when you want to dig into the JVM ↔ Wolfram communication.

#### Writing documentation

Documentation is written as literal programming sources in the `notebooks` directory and turned into HTML
under `docs` using [Clay](https://scicloj.github.io/clay/)
and [Quarto](https://quarto.org/).

To render a single namespace/page, require Clay and run `(clay/make! {:source-path "<path to the file>""})`. Tip: You can also do this without quarto - just add `:run-quarto false` to the options.

To build the whole site, run `clojure -T:build build-site` (remembering to ensure that you have the `./symlink-jlink.jar` symlink).

## Authors

The original Clojuratica, Wolframite's predecessor, was created by Garth Sheldon-Coulson, a graduate student at the Massachusetts Institute of Technology and Harvard Law School.

Ongoing maintenance and development over the years have been thanks to

* [Steve Chan](https://github.com/chanshunli),
* [Dan Farmer](https://github.com/dfarmer),
* [Norman Richards](https://github.com/orb)
and others.

Most recently, Clojuratica has been transformed into *Wolframite* by

* Pawel Ceranka,
* Thomas Clark and
* [Jakub Holý](https://holyjak.cz);

and is now being maintained as part of the [SciCloj](https://github.com/scicloj) project. Wolframite is free, open-source software, but if you would like to support our work then please cite us.

``` bibtex
@software{Holý_2024},
author = {Holý, Jakub and Clark, Thomas and Ceranka, Pawel and Richards, Norman and Farmer, Dan and Chan, Steve and Sheldon-Coulon, Garth},
license = {Mozilla Public License},
title = {{Wolframite: }},
url = {https://github.com/scicloj/wolframite},
version = {1.0.0-SNAPSHOT},
year = {2024}

```
## Sponsorship
We are grateful for the financial support of [Clojurists Together](https://www.clojuriststogether.org/), who supported this work for a quarter in 2024.

## Contact
If you would like to contact the maintainers or otherwise seek help from the community then please drop a message into our [zulip channel](https://clojurians.zulipchat.com/#narrow/stream/313853-wolfram-clojure-bridge), the `#wolframite` channel in [Clojurians Slack](https://clojurians.net), or contact the team at [SciCloj](https://github.com/scicloj).

## License

Distributed under the Mozilla Public License either version 2.0 or (at
your option) any later version.

## Legal

The product names used in this website are for identification purposes only.
All trademarks and registered trademarks, including "Wolfram Mathematica," are the property of their respective owners.
Wolframite is not a product of Wolfram Research.
The software on this site is provided "as-is," without any express or implied warranty.
