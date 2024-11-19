(ns wolframite.runtime.defaults
  "Flags and aliases for the Wolfram runtime."
  (:require [wolframite.flags :as flags]
            [clojure.set :as set]))

"TODO:
- Consider function that finds all non-numeric symbols (and not '- ') that start with a '-' and replace them with Minus[<symbol>]
- Consider renaming this namespace. Not clear what it contains."

;; * Flags
(def flag-sets
  {#{:vectors :seqs #_:seq-fn} :vectors ;; **DEPRECATED**: this is not really a flag, not sure how useful at all
   #{flags/parallel flags/serial} flags/serial
   #{flags/parse flags/no-parse} flags/parse
   #{flags/evaluate flags/no-evaluate} flags/evaluate
   #{flags/convert flags/no-convert} flags/convert
   ;#{:hash-maps :no-hash-maps} :hash-maps
   ;#{:functions :no-functions} :functions ;; ?? parse (Function ...) into our parse-fn instance?!
   ;#{:aliases :no-aliases}     :aliases   ;; Not used anywhere?!
   #{flags/arrays flags/no-arrays} flags/no-arrays ; arrays from Wolfram are returned as Java arrays, not Clj vectors (more efficient)
   ;#{:verbose :no-verbose}     :no-verbose
   ;#{:as-function
   ;  :as-expression}           :as-expression
   ;#{:restore-defaults
   ;  :no-restore-defaults}     :no-restore-defaults
   #{:full-form :clojure-form} :clojure-form})

(def all-flags (apply set/union (keys flag-sets)))

;(def default-flags (set (vals flag-sets)))

;; * Aliases

(def base-aliases
  ;; TIP: Ideally, respect https://clojure.org/reference/reader#_symbols, or at least the wider
  ;;  https://github.com/edn-format/edn?tab=readme-ov-file#symbols minus Clojure's reserved `: .`
  ;; OF the syms we use, especially / is problematic; few others may be too.
  {'do 'CompoundExpression
   '= 'Set
   '_= 'SetDelayed
   '=! 'Unset
   '-> 'Rule
   '_> 'RuleDelayed
   '== 'Equal
   '=== 'SameQ
   '< 'Less
   '> 'Greater
   '<= 'LessEqual
   '>= 'GreaterEqual
   '+= 'AddTo
   '-= 'SubtractFrom
   '+    'Plus
   '-    ^{:doc "Maps to Wolfram Minus/Subtract"
           ::experimental-fn true
           :wolframite.alias/targets #{'Minus 'Subtract}}

   (fn [args] (case (count args)
                1 'Minus
                2 'Subtract
                (throw (IllegalArgumentException. "Can't handle more than 2 arguments"))))
   '*    'Times
   '<*>    'Dot
   '/    'Divide
   '<>   'StringJoin
   '&&   'And
   '||   'Or
   '!    'Not
   'fn   'Function
   '?? 'Information
   '** 'Power
   '++ 'Conjugate
   'x> 'Replace
   'x>> 'ReplaceAll
   '<_> 'Expand
   '<<_>> 'ExpandAll
   '++<_> 'ComplexExpand
   '>_< 'Simplify
   '>>_<< 'FullSimplify
   '⮾ 'NonCommutativeMultiply
   '√ 'Sqrt
   '∫ 'Integrate})

(defn experimental-fn-alias? "EXPERIMENTAL - DO NOT USE!" [alias]
  (-> alias meta ::experimental-fn))

(def all-aliases base-aliases)

;; * Full config options

(def default-options
  (merge flag-sets
         {#_#_:alias-list                 :clojure-aliases
          :poll-interval              20 ;; ms
          :clojure-aliases            base-aliases
          :all-aliases all-aliases}))
