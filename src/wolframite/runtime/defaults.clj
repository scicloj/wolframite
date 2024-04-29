(ns wolframite.runtime.defaults
  (:require [clojure.set]))

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
  {'do   'CompoundExpression
   '=    'Set
   '..=  'SetDelayed ;; TODO: Document this exception. Presumably not := because of clojure's keywords
   '=.   'Unset
   '->   'Rule
   '..>  'RuleDelayed
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
   '**   'Power
   '.    'Dot
   '/    'Divide
   '<>   'StringJoin
   '&&   'And
   '||   'Or
   '!    'Not
   'fn   'Function})

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
   '**9 'WolframitePower9})

(def all-aliases (merge base-aliases clojure-scope-aliases user-aliases))

;; * Full config options

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
   :kernel/link nil})
