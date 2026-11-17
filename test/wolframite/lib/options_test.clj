(ns wolframite.lib.options-test
  "Pure tests (no kernel needed) of how flags are resolved."
  (:require [clojure.test :refer [deftest is testing]]
            [wolframite.flags :as flags]
            [wolframite.lib.options :as options]
            [wolframite.runtime.defaults :as defaults]))

(deftest flag?'-test
  (testing "defaults apply when the user sets no flags"
    (is (options/flag?' nil flags/serial))
    (is (options/flag?' #{} flags/parse))
    (is (not (options/flag?' nil flags/parallel)))
    (is (not (options/flag?' nil flags/arrays))))
  (testing "a user flag overrides the default of its set, and only that one"
    (is (options/flag?' #{flags/parallel} flags/parallel))
    (is (not (options/flag?' #{flags/parallel} flags/serial)))
    (is (not (options/flag?' #{flags/no-parse} flags/parse)))
    (is (options/flag?' #{flags/no-parse} flags/evaluate)))
  (testing "flags may be given as any collection"
    (is (options/flag?' [flags/arrays] flags/arrays))
    (is (options/flag?' (list flags/arrays flags/no-evaluate) flags/no-evaluate)))
  (testing "unknown flags are ignored"
    (is (not (options/flag?' #{:no-such-flag} :no-such-flag)))
    (is (options/flag?' #{:no-such-flag} flags/serial)))
  (testing "the (memoized) flag?' agrees with resolving the flags from scratch"
    (doseq [user-flags [nil #{} #{flags/parallel} [flags/arrays flags/no-parse]
                        #{flags/allow-large-data flags/no-convert} #{:no-such-flag}]
            flag defaults/all-flags]
      (is (= (options/flag? (options/flags-into defaults/default-options user-flags) flag)
             (options/flag?' user-flags flag))
          (str "user flags " (pr-str user-flags) ", flag " flag)))))
