(ns com.palletops.multi-test.test-ns2
  (:require
   [clojure.pprint]
   [clojure.test :refer [deftest is testing with-test-out]]
   [com.palletops.multi-test :refer :all]))

(def ^:dynamic *x* nil)

(defn test-ns-hook
  "Don't test this namespace with clojure.test"
  []
  (test-ns (ns-name *ns*) {:bindings [{#'*x* 1}] :threads 2}))

(deftest binding-test
  (testing "binding"
    (is (= 1 *x*) "succeed")
    (testing "fail introspection"
      (is (not (test-var-has-failures?))))))
