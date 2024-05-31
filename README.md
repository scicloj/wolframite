# Wolframite

An interface between Clojure and the Wolfram Language (Supports [Mathematica](https://www.wolfram.com/mathematica/) and [Wolfram Engine](https://www.wolfram.com/engine/) ).

## Status

**Wolframite is currently (Q4/2023) under active development again. You can [keep track of what is happening in this discussion](https://github.com/scicloj/wolframite/discussions/17).**

## What is Wolframite? ##

Wolframite (formerly Clojuratica) brings together two of today's most exciting tools for high-performance, parallel computation.

[Clojure](http://clojure.org) is a dynamic functional programming language with a compelling approach to concurrency and state, a strong cast of persistent immutable data structures, and a growing reputation for doing all the right things.
[Wolfram Mathematica](https://www.wolfram.com/mathematica/) is arguably the world's most powerful integrated tool for numerical computation, symbolic mathematics, optimization, and visualization and is build on top of its own splendid functional programming language, [Wolfram Language](https://www.wolfram.com/language/).

By linking the two:

* Wolframite lets you **write and evaluate Wolfram/Mathematica code in Clojure** with full **syntactic integration**. Now Clojure programs can take advantage of Wolfram's enormous range of numerical and symbolic mathematics algorithms and fast matrix algebra routines.
* Wolframite provides the **seamless and transparent translation of native data structures** between Clojure and Wolfram. This includes high-precision numbers, matricies, N-dimensional arrays, and evaluated and unevaluated Mathematica expressions and formulae.
* Wolframite lets you **call, pass, and store Wolfram functions just as if they were first-class functions in Clojure.** This is high-level functional programming at its finest. You can write a function in whichever language is more suited to the task and never think again about which platform is evaluating calls to that function.
* Wolframite facilitates **the "Clojurization" of Wolfram's existing parallel-computing capabilities.** Wolfram is not designed for threads or concurrency. It has excellent support for parallel computation, but parallel evaluations are initiated from a single-threaded master kernel which blocks until all parallel evaluations return. By contrast, Wolframite includes a concurrency framework that lets multiple Clojure threads execute Wolfram expressions without blocking others. Now it is easy to run a simulation in Clojure with 1,000 independent threads asynchronously evaluating processor-intensive expressions in Wolfram. The computations will be farmed out adaptively and transparently to however many Wolfram kernels are available on any number of processor cores, either locally or across a cluster, grid, or network.

Wolframite is open-source and targeted at applications in scientific computing, computational economics, finance, and other fields that rely on the combination of parallelized simulation and high-performance number-crunching. Wolframite gives the programmer access to Clojure's most cutting-edge features--easy concurrency and multithreading, immutable persistent data structures, and software transactional memory---alongside Wolfram's easy-to-use algorithms for numerics, symbolic mathematics, optimization, statistics, visualization, and image-processing.

## Usage

### Prerequisites:

#### Clojure

First, if you haven't already, install the [Clojure CLI toolchain](https://clojure.org/guides/getting_started) (homebrew is a great way to do this if you're on Mac or Linux, but you can just as easily use the installation scripts if you prefer).

#### Mathematica or Wolfram Engine

Next, obviously, you'll need to ensure that you have Wolfram Engine or Mathematica installed.

Normally, it should be detected and loaded automatically, when you require the `wolframite.core` namespace. Watch stdout for a message like:

> Adding path to classpath: /Applications/Wolfram Engine.app/Contents/Resources/Wolfram Player.app/Contents/SystemFiles/Links/JLink/JLink.jar

However, sometimes Wolframite may fail to find the correct path automatically and needs your help. You can set the `WOLFRAM_INSTALL_PATH` environment variables or Java system properties (the latter takes priority) to point to the correct location. Example:

```shell
export WOLFRAM_INSTALL_PATH=/opt/mathematica/13.1
```

### Getting started

```
clj -A:dev
(dev) ; this takes few 10s of seconds
```

`(dev)` drops you into the dev namespace and requires `wolframite.core` ns, which is side effecting (detecting and initializing Wolfram/Mathematica and internalizing all Wolfram functions)!

Check if you're all set:

```clojure
(wl/eval '(Dot [1 2 3] [4 5 6]))
;=> 32
```

More examples

```clojure
(wl/eval '(D (Power 'x 2) 'x))
;=> (* 2 x)
(wl/eval '(ChemicalData "Ethanol" "MolarMass"))
;=> (Quantity 46.069M (* "Grams" (Power "Moles" -1)))

;; Accessing WlframAlpha
(wl/eval '(WolframAlpha "How many licks does it take to get to the center of a Tootsie Pop?"))
;=> [(-> [["Input" 1] "Plaintext"] "How many licks does it take to get to the Tootsie Roll center of a Tootsie Pop?") (-> [["Result" 1] "Plaintext"] "3481\n(according to student researchers at the University of Cambridge)")]
; FIXME Try this out; when offline, we get just []

(wl/eval '(N Pi 20))
(N 'Pi 20)
;=> 3.141592653589793238462643383279502884197169399375105820285M
```

#### Learning Wolframite

Read through and play with [explainer.clj](dev%2Fexplainer.clj) and [demo.clj](dev%2Fdemo.clj), which demonstrate most of Wolframite's features and what you can do with Wolfram.

### Clerk Integration

Example usage: (watching for changes in a folder)

```
user> (require '[clojuratica.tools.clerk-helper :as ch])
user> (ch/clerk-watch! ["dev/notebook"])
```

* Open dev/notebook/demo.clj, make a change and save.
* Open `localhost:7777` in the browser

## Dependencies

Wolframite requires Wolfram's Java integration library JLink, which is currently only available with a Wolfram Engine or Mathematica installation. It will also need to know where the `WolframKernel` / `MathKernel` executable is, in order to be able to start the external evaluation kernel process. Normally, `wolframite.jlink` should be able to find these automatically, if you installed either into a standard location on Mac, Linux or Windows. However, if necessary, you can specify either with env variables / sys properties - see Prerequisites above.

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

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Legal

The product names used in this website are for identification purposes only.
All trademarks and registered trademarks, including "Wolfram Mathematica," are the property of their respective owners.
Wolframite is not a product of Wolfram Research.
The software on this site is provided "as-is," without any express or implied warranty.
