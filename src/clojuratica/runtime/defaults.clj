(ns clojuratica.runtime.defaults
  (:require clojure.set))

;; * Flags
(def flag-sets {#{:vectors :seqs #_:seq-fn} :vectors
                #{:parallel :serial}        :serial
                #{:parse :no-parse}         :parse
                #{:evaluate :no-evaluate}   :evaluate
                #{:convert :no-convert}     :convert
                #{:hash-maps :no-hash-maps} :hash-maps
                #{:functions :no-functions} :functions
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
   '..=  'SetDelayed
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
   '-    'Subtract
   '*    'Times
   '.    'Dot
   '/    'Divide
   '<>   'StringJoin
   '&&   'And
   '||   'Or
   '!    'Not})

(def clojure-scope-aliases
  {'Function 'ClojurianScopes/Function
   'Let      'ClojurianScopes/Let
   'With     'ClojurianScopes/With
   'Block    'ClojurianScopes/Block
   'Module   'ClojurianScopes/Module})

(def all-aliases (merge base-aliases clojure-scope-aliases))

;; * Full config options

(def default-options
  (merge flag-sets
         {:alias-list                 :clojure-aliases
          :poll-interval              20 ;; ms
          :clojure-scope-aliases      clojure-scope-aliases
          :clojure-aliases            base-aliases}))

;; NOTE: DEPRECATED *default-options*
(def ^:dynamic *default-options*
   {#{:vectors :seqs :seq-fn}   :vectors
    #{:parallel :serial}        :serial
    #{:parse :no-parse}         :parse
    #{:evaluate :no-evaluate}   :evaluate
    #{:convert :no-convert}     :convert
    #{:hash-maps :no-hash-maps} :hash-maps
    #{:functions :no-functions} :functions
    #{:aliases :no-aliases}     :aliases
    #{:N :no-N}                 :no-N
    #{:verbose :no-verbose}     :no-verbose
    #{:as-function
      :as-expression}           :as-expression
    #{:full-form
      :clojure-form}            :clojure-form
    #{:restore-defaults
      :no-restore-defaults}     :no-restore-defaults
    :alias-list                 :clojure-aliases
    :poll-interval              20 ;ms
    :clojure-scope-aliases      clojure-scope-aliases
    :clojure-aliases            base-aliases})
