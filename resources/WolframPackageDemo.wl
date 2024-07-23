(* ::Package:: *)

  BeginPackage[ "WolframPackageDemo`"]

  tryIt::usage = "Used for testing Wolframite."
  additional::usage = "Another function in the test package."

  Begin[ "`Private`"]

  tryIt[ x_] :=
    Module[ {y},
      x^3
    ]
    additional[y_]:=3*y
  End[]
  EndPackage[]
  "it works"



