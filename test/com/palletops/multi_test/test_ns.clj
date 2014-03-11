(ns com.palletops.multi-test.test-ns
  (:require
   [clojure.test :refer :all]))

(def test-ns-hook
  "Don't test this namespace with clojure.test"
  (constantly false))

(def ^:dynamic *x* nil)

(deftest simple-test
  (testing "true tests"
    (is (= 1 1) "succeed"))
  (testing "fail tests"
    (is (= 1 0) "fail")))

(deftest binding-test
  (testing "binding"
    (is (= 1 *x*) "succeed")))
