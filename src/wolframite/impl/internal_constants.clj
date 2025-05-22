(ns wolframite.impl.internal-constants
  "Internal constants. This can be safely required in any internal ns w/o a risk of circular dependencies.")

(def wolframiteLimitSize
  "Wolframite-made Wolfram fn for wrapping expressions to prevent undesired large data transfers."
  'wolframiteLimitSize)

(def WolframiteLargeData
  "Symbol representing that the result of an expression is too large to be returned."
  'WolframiteLargeData)