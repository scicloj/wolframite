(* ::Package:: *)

  BeginPackage[ "WolframPackage`"]

  tryIt::usage = "Used for testing cloratica."
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



