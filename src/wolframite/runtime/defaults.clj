(ns wolframite.runtime.defaults
  "Flags and aliases for the Wolfram runtime."
  (:require [clojure.set]
            [clojure.walk :as walk]))

"TODO:
- Consider function that finds all non-numeric symbols (and not '- ') that start with a '-' and replace them with Minus[<symbol>]
- Consider renaming this namespace. Not clear what it contains."

;; * Flags
(def flag-sets {#{:vectors :seqs #_:seq-fn} :vectors ;; FIXME: this is not really a flag, not sure how useful at all
                #{:parallel :serial}        :serial
                #{:parse :no-parse}         :parse
                #{:evaluate :no-evaluate}   :evaluate
                #{:convert :no-convert}     :convert
                #{:hash-maps :no-hash-maps} :hash-maps ;; FIXME: remove when Association's impltd
                #{:functions :no-functions} :functions ;; ??
                #{:aliases :no-aliases}     :aliases
                #{:N :no-N}                 :no-N
                #{:verbose :no-verbose}     :no-verbose
                #{:as-function
                  :as-expression}           :as-expression
                #{:full-form
                  :clojure-form}            :clojure-form
                #{:restore-defaults
                  :no-restore-defaults}     :no-restore-defaults})

(def all-flags (apply clojure.set/union (keys flag-sets)))

(def default-flags (set (vals flag-sets)))

;; * Aliases

(def base-aliases
  {'<< 'Get
   '? 'WolframFunctionUsage ;; TODO: (need to add the Wolfram-side part)
   '?? 'Information
   'do   'CompoundExpression
   '=    'Set
   '..=  'SetDelayed ;; TODO: Document this exception. Presumably not := because of clojure's keywords. It's actually nicer in a way though, because an (almost) ellipsis implies a delay!
   '=.   'Unset
   '->   'Rule
   '..>  'RuleDelayed
   '_ 'Blank
   '__ 'BlankSequence
   '==   'Equal
   '===  'SameQ
   '<    'Less
   '>    'Greater
   '<=   'LessEqual
   '>=   'GreaterEqual
   '+=   'AddTo
   '-=   'SubtractFrom
   '+    'Plus
   '-    'Subtract  ;; TODO: deal with Subtract[x] i.e. single argument; Minus[x] works
   '*    'Times
   '** 'Power
   '⮾ 'NonCommutativeMultiply
   '√ 'Sqrt
   '.    'Dot
   '/    'Divide
   '<>   'StringJoin
   '&&   'And
   '||   'Or
   '!    'Not
   'fn   'Function
   '<-> 'ReplaceAll})

(def clojure-scope-aliases
  {#_#_'Function 'ClojurianScopes/Function
   'Let      'ClojurianScopes/Let
   'With     'ClojurianScopes/With
   'Block    'ClojurianScopes/Block
   'Module   'ClojurianScopes/Module})

(def user-aliases
  {'**2 'WolframitePower2
   '**3 'WolframitePower3
   '**4 'WolframitePower4
   '**5 'WolframitePower5
   '**6 'WolframitePower6
   '**7 'WolframitePower7
   '**8 'WolframitePower8
   '**9 'WolframitePower9

   '**-1 'WolframitePowerMinus1
   '**-2 'WolframitePowerMinus2
   '**-3 'WolframitePowerMinus3
   '**-4 'WolframitePowerMinus4
   '**-5 'WolframitePowerMinus5
   '**-6 'WolframitePowerMinus6
   '**-7 'WolframitePowerMinus7
   '**-8 'WolframitePowerMinus8
   '**-9 'WolframitePowerMinus9})

(def emmy-aliases
  {})

(def all-aliases (merge base-aliases clojure-scope-aliases user-aliases))

;; * Full config options

;; TODO: Do we need separate categories of aliases at this point?
(def default-options
  (merge flag-sets
         {#_#_:alias-list                 :clojure-aliases
          :poll-interval              20 ;; ms
          :clojure-scope-aliases      clojure-scope-aliases
          :clojure-aliases            base-aliases
          :all-aliases all-aliases}))

;; * DEV: WIP!

(comment
  (def default-flags'
    {:serial       true
     :convert      true
     :evaluate     true
     :parse        true
     :functions    true
     :aliases      true
     :as-expression true ;; FIXME: shouldn't be a flag but a parsing option?
     :clojure-form true ;; FIXME: better name
     :N false})

;; support some better merging with a lib
  ;; top level options spec
  ;; default options
  {:flags {:convert true
           :parse false}
   :aliases {}
   :config {:poll-interval 20}
   ;; runtime opts
   :kernel/link nil}

  (def expression '(+ -x -x -y -5 -2 (- x 10 5) (** x 2)))

  (defn strip-ns [form]
    (walk/postwalk (fn [form]
                     (if (qualified-symbol? form)
                       (symbol (name form))
                       form))
                   form))

  (defn replacement-map
    "Creates a replacement map for negative symbols, such that they are reasonably interpreted by Wolfram.

  TODO:
  - Extend the idea to deal with other custom replacements (e.g. greek/hebrew symbols.) .
  "
    [expression]
    (let [syms (->> expression
                    strip-ns
                    (into '())
                    (tree-seq list? seq)
                    (remove (some-fn list? nil?))
                    (filter #(when (symbol? %)
                               (->> %
                                    str
                                    (re-matches #"-.+"))))
                    distinct)

          syms-base (map (fn [sym] (-> sym str char-array rest (#(apply str %)) symbol)) syms)]

      (zipmap syms (map (fn [sym] (format "Minus[%s]" sym)) syms-base))))

  (replacement-map expression)

  (replacement-map `(+ -x -x -y -5 -2 (- x 10 5) (** x 2))))
