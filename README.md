# Clojuratica

An interface between Clojure and the Wolfram Language and Mathematica.

## What is Clojuratica?

Clojuratica brings together two of today's most exciting tools for high-performance, parallel computation.

[Clojure](http://clojure.org) is a dynamic functional programming language with a compelling approach to concurrency and state, a strong cast of persistent immutable data structures, and a growing reputation for doing all the right things.
[Wolfram Mathematica](https://www.wolfram.com/mathematica/) is arguably the world's most powerful integrated tool for numerical computation, symbolic mathematics, optimization, and visualization and is build on top of its own splendid functional programming language, [Wolfram Language](https://www.wolfram.com/language/).

By linking the two:

* Clojuratica lets you **write and evaluate Mathematica code in Clojure** with full **syntactic integration**. Now Clojure programs can take advantage of Mathematica's enormous range of numerical and symbolic mathematics algorithms and fast matrix algebra routines.
* Clojuratica provides the **seamless and transparent translation of native data structures** between Clojure and Mathematica. This includes high-precision numbers, matricies, N-dimensional arrays, and evaluated and unevaluated Mathematica expressions and formulae.
* Clojuratica lets you **call, pass, and store Mathematica functions just as if they were first-class functions in Clojure.** This is high-level functional programming at its finest. You can write a function in whichever language is more suited to the task and never think again about which platform is evaluating calls to that function.
* Clojuratica facilitates **the "Clojurization" of Mathematica's existing parallel-computing capabilities.** Mathematica is not designed for threads or concurrency. It has excellent support for parallel computation, but parallel evaluations are initiated from a single-threaded master kernel which blocks until all parallel evaluations return. By contrast, Clojuratica includes a concurrency framework that lets multiple Clojure threads execute Mathematica expressions without blocking others. Now it is easy to run a simulation in Clojure with 1,000 independent threads asynchronously evaluating processor-intensive expressions in Mathematica. The computations will be farmed out adaptively and transparently to however many Mathematica kernels are available on any number of processor cores, either locally or across a cluster, grid, or network.

Clojuratica is open-source and targeted at applications in scientific computing, computational economics, finance, and other fields that rely on the combination of parallelized simulation and high-performance number-crunching. Clojuratica gives the programmer access to Clojure's most cutting-edge features--easy concurrency and multithreading, immutable persistent data structures, and software transactional memory---alongside Mathematica's easy-to-use algorithms for numerics, symbolic mathematics, optimization, statistics, visualization, and image-processing.

The canonical pronunciation of Clojuratica starts with Clojure and rhymes with erotica.

## Usage

### Prerequisites:

First, if you haven't already, install the [Clojure CLI toolchain](https://clojure.org/guides/getting_started) (homebrew is a great way to do this if you're on Mac or Linux, but you can just as easily use the install scripts if you prefer).
Next, obviously, you'll need to ensure that you have Wolfram Language or Mathematica installed.

### Getting started

After starting a REPL, `(dev)` drops you into dev namespace. This is side effecting, altering the classpath to require JLink (see docstring on `init` ns):

```
clj -M:dev
(dev)
:loaded
```

If the above complains that the installation path cannot be found, please export either `MATHEMATICA_INSTALL_PATH` or `WOLFRAM_INSTALL_PATH` environment variables to tell Clojuratica where it needs to look.

Check if you're all set:

```clojure
(WL (Dot [1 2 3] [4 5 6]))
;=> 32
```

More examples

```clojure
(WL (D (Power x 2) x))
;=> (* 2 x)
(WL (ChemicalData "Ethanol" "MolarMass"))
;=> (Quantity 46.069M (* "Grams" (Power "Moles" -1)))

;; Accessing WlframAlpha
(WL (WolframAlpha "How many licks does it take to get to the center of a Tootsie Pop?"))
;=> [(-> [["Input" 1] "Plaintext"] "How many licks does it take to get to the Tootsie Roll center of a Tootsie Pop?") (-> [["Result" 1] "Plaintext"] "3481\n(according to student researchers at the University of Cambridge)")]

(WL (N Pi 20))
;=> 3.141592653589793238462643383279502884197169399375105820285M
```

## Dependencies

Clojuratica requires JLink, which is currently only available with a Mathematica or Wolfram Language install.
It will also need to know where the `MathKernel` is, in order to initialize against it (see `clojuratica.init` ns for details).

If you've installed Mathematica in a default location on Mac, Linux or Windows, Clojuratica _should_ be able to find these files automatically.
However, if necessary, you can specify either the `MATHEMATICA_INSTALL_PATH` or `WOLFRAM_INSTALL_PATH` environment variable to tell Clojuratica where it needs to look.
For example, if you have Wolfram Language installed, do the following before starting Clojuratica:

```bash
export WOLFRAM_INSTALL_PATH=/usr/local/Wolfram/WolframEngine/12.3
```

The example location corresponds to a typical Wolfram Language installation, but please check this is a valid location in your environment.

## Authors

Clojuratica was created by Garth Sheldon-Coulson, a graduate student at the Massachusetts Institute of Technology and Harvard Law School. See the [Community](http://clojuratica.weebly.com/community.html) page to find out how to contribute to Clojuratica, suggest features, report bugs, or ask general questions.  It is currently maintained by [Norman Richards](http://github.com/orb).

Ongoing maintenance and development over the years have been thanks to
* [Steve Chan](https://github.com/chanshunli)
* [Dan Farmer](https://github.com/dfarmer)
* [Norman Richards](https://github.com/orb)

The project is now being maintained as part of the [SciCloj](https://github.com/scicloj) project.

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Legal

The product names used in this web site are for identification purposes only.
All trademarks and registered trademarks, including "Wolfram Mathematica," are the property of their respective owners.
Clojuratica is not a product of Wolfram Research.
The software on this site is provided "as-is," without any express or implied warranty.
