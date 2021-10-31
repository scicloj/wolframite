
# Scicloj Fork

* Moved to Clojure CLI & `deps.edn` tooling
* Automated JLink and Kernel initialization process with default pathways based on OS
  * Added `MATHEMATICA_INSTALL_PATH` and `WOLFRAM_INSTALL_PATH` options to override defaults
  * Code is dynamically bound, and should not have to be updated to run with newer versions of Wolfram Language or Mathematica
* Cleaned up namespace references (replaced `:use` with `:require` across namespace
* Switched default `math` macro to `WL`, to reflect more general emphasis on supporting Wolfram Language


## Dan Farmer's fork message

The original authors seem to have moved on; so far I've only updated the project.clj to bring things up to a more recent version of Clojure (and JLink to match Mathematica 11.3), I added the lein-repo plugin for folks that don't want to mess with the Maven steps below and wrote a basic (init-win) function for Windows users.

As far as real improvements go I'd like to:
- [ ] Add support for plotting
- [ ] Replace the home-grown HashMaps with Mathematica's built-in Associations (they weren't in Mathematica when the original author wrote the package)

Assuming you're on OS X or Windows and have Mathematica installed in the default location you should be able to run:

