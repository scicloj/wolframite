(ns wolframite.runtime.defaults
  "Flags and aliases for the Wolfram runtime."
  (:require clojure.set
            [clojure.walk :as walk]))

"TODO:
- Consider function that finds all non-numeric symbols (and not '- ') that start with a '-' and replace them with Minus[<symbol>]
- Consider renaming this namespace. Not clear what it contains."

;; * Flags
(def flag-sets {#{:vectors :seqs #_:seq-fn} :vectors ;; FIXME: this is not really a flag, not sure how useful at all
                #{:parallel :serial}        :serial ; serial = req-response; parallel = when having a bunch of parallel kernels (Wolfram-managed)
                #{:parse :no-parse}         :parse
                #{:evaluate :no-evaluate}   :evaluate
                #{:convert :no-convert}     :convert
                ;#{:hash-maps :no-hash-maps} :hash-maps
                #{:functions :no-functions} :functions ;; ?? parse (Function ...) into our parse-fn instance?!
                #{:aliases :no-aliases}     :aliases
                #{:N :no-N}                 :no-N ; :N -> use Expr.asArray on matrix' rows
                ;#{:verbose :no-verbose}     :no-verbose
                ;#{:as-function
                ;  :as-expression}           :as-expression
                ;#{:restore-defaults
                ;  :no-restore-defaults}     :no-restore-defaults
                #{:full-form
                  :clojure-form}            :clojure-form})

(def all-flags (apply clojure.set/union (keys flag-sets)))

(def default-flags (set (vals flag-sets)))

;; * Aliases

(def base-aliases
  {'do   'CompoundExpression
   '=    'Set
   '_=  'SetDelayed
   '=!   'Unset
   '->   'Rule
   '_>  'RuleDelayed
   '==   'Equal
   '===  'SameQ
   '<    'Less
   '>    'Greater
   '<=   'LessEqual
   '>=   'GreaterEqual
   '+=   'AddTo
   '-=   'SubtractFrom
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
   ;; The "new" aliases, added for v1:
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

;; * DEV: WIP!

(comment
  (def default-flags'
    {:serial       true
     :convert      true
     :evaluate     true
     :parse        true
     :functions    true
     :aliases      true
     ;:as-expression true
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
   :jlink-instance nil}

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
