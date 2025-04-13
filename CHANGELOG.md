Changelog
=========

1.1.0
-----

* Switched the default Java graphics from AWT to Swing (see below)
* Renamed all `view` fns for hiccup/clerk/portal to `show` to be consistent with the Java graphics `show!` (aside of the `!`)
  and with a similarly named Wolfram Language function.
* **Breaking change**: the former ns `wolframite.tools.graphics` is now called `wolframite.tools.graphics-legacy-awt`,
  while the former `experimental` is the new `graphics` ns. There is still a `show!` fn, but now it takes 1-3 arguments.
  Creating an app or canvas is not necessary anymore. You can still `show!` multiple graphics in different windows by
  passing in an explicit `nil` for the `window` argument.

1.0.1
-----
Docstring improvements, add `wh/view-graphics-unadorned`.