Changelog
=========

WIP
---

1.3.0 6/2025
------------
* Support DelayedRules in Associations - the raw, unevaluted expression is now returned, with a metadata marking it as such
* Detect a "delayed error message" (MessageTemplate) and throw an exception with the message instead
* Detect large data in Wolfram results and return `:wolframite/large-data` instead, unless the flag `wolframite.flags/allow-large-data` is set. This prevents undesirably slow processing and avoidable memory issues.

1.2.0 5/2025
------------

* Re-generated `wolframite.wolfram` for Wolfram Language 14.2 (previously 14.0)
* Expanded Gotchas doc page

1.1.1
-----

* Fix `graphics/show!` when rendering new content in the same window, and to scale content correctly

1.1.0
-----

* Switched the default Java graphics from AWT to Swing (see below)
* Renamed all `view` fns for hiccup/clerk/portal to `show` to be consistent with the Java graphics `show!` (aside of the `!`)
  and with a similarly named Wolfram Language function.
* **Breaking change**: the former ns `wolframite.tools.graphics` is now called `wolframite.legacy.tools.graphics-awt`,
  while the former `experimental` is the new `graphics` ns. There is still a `show!` fn, but now it takes 1-3 arguments.
  Creating an app or canvas is not necessary anymore. You can still `show!` multiple graphics in different windows by
  passing in an explicit `nil` for the `window` argument.
* Added support for resizing the Swing graphics view and for storing it as an image (Thomas)
* Improved handling of non-standard paths (Thomas)

1.0.1
-----
Docstring improvements, add `wh/view-graphics-unadorned`.