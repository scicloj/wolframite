(ns wolframite.impl.internal-constants
  "Internal constants. This can be safely required in any internal ns w/o a risk of circular dependencies.")

(def wolframiteLimitSize
  "Wolframite-made Wolfram fn for wrapping expressions to prevent undesired large data transfers."
  'wolframiteLimitSize)

(def WolframiteLargeData
  "Symbol representing that the result of an expression is too large to be returned."
  'WolframiteLargeData)

(def wolframiteFastArray
  "Wolframite-made Wolfram fn for wrapping expressions so that large packed numeric arrays in the result
  are tagged with [[WolframitePackedArray]], which allows us to read them quickly as Java arrays."
  'wolframiteFastArray)

(def WolframitePackedArray
  "Head of `WolframitePackedArray[type, depth, data]`, which marks a result that can be read directly
  from the link as a Java array. `type` is one of [[packed-array-types]]."
  'WolframitePackedArray)

(def packed-array-types
  "Types of [[WolframitePackedArray]] data we know how to read fast."
  #{"Real64" "Integer32"})

(def fast-array-min-size
  "Minimal number of elements of a packed array for it to be tagged by [[wolframiteFastArray]].
  Smaller arrays are fast enough the normal way."
  1000)
