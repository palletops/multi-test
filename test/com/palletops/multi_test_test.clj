(ns com.palletops.multi-test-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.palletops.multi-test :refer :all]
   [com.palletops.multi-test.test-ns :as test-ns]))

;; See http://dev.clojure.org/jira/browse/CLJ-1379 for an issue
;; with no quoting of the :actual form for :pass type results.

(deftest test-ns-test
  (let [simple-result [{:contexts ["true tests"]
                        :type :pass
                        :expected '(= 1 1)
                        :actual (list = 1 1)
                        :message "succeed"
                        :var #'test-ns/simple-test}
                       {:contexts ["fail tests"]
                        :type :fail
                        :expected '(= 1 0)
                        :actual '(not (= 1 0))
                        :message "fail"
                        :file "test_ns.clj"
                        :line 15
                        :var #'test-ns/simple-test}
                       {:var #'test-ns/binding-test
                        :contexts ["binding"],
                        :message "succeed",
                        :actual '(not (= 1 nil)),
                        :expected '(= 1 *x*),
                        :type :fail,
                        :file "test_ns.clj",
                        :line 19}]
        binding-result [{:var #'test-ns/simple-test
                         :contexts ["true tests"],
                         :type :pass,
                         :expected '(= 1 1),
                         :actual (list = 1 1),
                         :message "succeed"}
                        {:var #'test-ns/simple-test
                         :contexts ["fail tests"],
                         :message "fail",
                         :actual '(not (= 1 0)),
                         :expected '(= 1 0),
                         :type :fail,
                         :file "test_ns.clj",
                         :line 15}
                        {:var #'test-ns/binding-test
                         :contexts ["binding"],
                         :type :pass,
                         :expected '(= 1 *x*),
                         :actual (list = 1 1),
                         :message "succeed"}]]
    ;; don't use testing forms here
    (is (= (set simple-result)
           (set (test-ns 'com.palletops.multi-test.test-ns
                         {:reporter silent-report}))))
    (is (= (set binding-result)
           (set (test-ns 'com.palletops.multi-test.test-ns
                         {:bindings [{#'test-ns/*x* 1}]
                          :reporter silent-report}))))
    (is (= (set simple-result)
           (set (test-ns 'com.palletops.multi-test.test-ns
                         {:threads 2
                          :reporter silent-report}))))
    (is (= (set binding-result)
           (set (test-ns 'com.palletops.multi-test.test-ns
                         {:bindings [{#'test-ns/*x* 1}]
                          :threads 2
                          :reporter silent-report}))))))
