(ns wolframite.runtime.system-test
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is testing]]
            [wolframite.runtime.jlink :as jlink]
            [wolframite.runtime.system :as system]))

(deftest jlink-jar-path-test
  (let [dir (str (fs/create-temp-dir))]
    (try
      (testing "root"
        (is (= dir (system/root {:user-paths {"JLINK_JAR_PATH" "/x/JLink.jar"
                                              "WOLFRAM_INSTALL_PATH" dir}}))
            "A JLINK_JAR_PATH pointing to a jar is not used as the root")
        (is (= dir (system/root {:user-paths {"JLINK_JAR_PATH" "/x/JLink.jar"}
                                 :defaults {:root dir}}))
            "A JLINK_JAR_PATH pointing to a jar is not used as the root, even w/o WOLFRAM_INSTALL_PATH")
        (is (= dir (system/root {:user-paths {"JLINK_JAR_PATH" dir}}))
            "Legacy: a JLINK_JAR_PATH pointing to a directory is used as the root"))
      (testing "jlink path"
        (with-redefs [system/info (constantly {:user-paths {"JLINK_JAR_PATH" "/x/JLink.jar"}})]
          (is (= "/x/JLink.jar" (#'jlink/path--jlink))
              "JLINK_JAR_PATH pointing to a jar is used as-is"))
        (with-redefs [system/info (constantly {:user-paths {"WOLFRAM_INSTALL_PATH" dir}})]
          (is (= (str (fs/path dir "SystemFiles/Links/JLink/JLink.jar")) (#'jlink/path--jlink))
              "Otherwise, the jar is found under the root")))
      (finally
        (fs/delete-tree dir)))))
