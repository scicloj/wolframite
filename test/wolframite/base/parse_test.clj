(ns wolframite.base.parse-test
  (:require
    [clojure.test :refer :all]
    [wolframite.base.convert :as convert]
    [wolframite.base.parse :as parse]
    [wolframite.core :as wl]))

(deftest parse-test
  (#'wl/init-jlink! (deref #'wl/kernel-link-atom) {}) ; bypass private var via deref #'
  (testing "maps"
    (is (= {"a" 1}
           (parse/parse (convert/convert {"a" 1} nil) nil)))
    (is (= {}
           (parse/parse (convert/convert {} nil) nil))
        "Empty maps work too")))
