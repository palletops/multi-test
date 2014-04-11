(ns com.palletops.multi-test
  "An alternative runner for clojure.test tests."
  (:require
   [clojure.core.async :refer [>!! <!! chan close! go-loop]]
   [clojure.set :refer [intersection]]
   [clojure.stacktrace :refer [print-cause-trace]]
   [clojure.test :as test]))


(defn print-fail
  "Print a result as a fail."
  [m]
  (test/with-test-out
    (println "\nFAIL in" (:name (meta (:var m))))
    (when (seq (:contexts m))
      (println (apply str (interpose " " (reverse (:contexts m))))))
    (when-let [message (:message m)]
      (println message))
    (println "expected:" (pr-str (:expected m)))
    (println "  actual:" (pr-str (:actual m)))))

(defn print-error
  "Print a result as an error."
  [m]
  (test/with-test-out
        (println "\nERROR in" (:name (meta (:var m))))
        (when (seq (:contexts m))
          (println
           (apply str (interpose " " (reverse (:contexts m))))))
        (when-let [message (:message m)]
          (println message))
        (println "expected:" (pr-str (:expected m)))
        (print "  actual: ")
        (let [actual (:actual m)]
          (if (instance? Throwable actual)
            (print-cause-trace actual test/*stack-trace-depth*)
            (prn actual)))))

(defn print-begin-ns
  "Print a message when starting to test a namespace."
  [m]
  (test/with-test-out
    (println "\nTesting" (ns-name (:ns m)))))

;;; # Test failure counter

;;; This is used to count the number of failures while running a test
;;; var.  This can be used to control resource cleanup based on
;;; whether anything failed or not.

;;; *test-var-fails* should be bound to an atom containing an integer
;;; count.

(def ^:dynamic *test-var-fails*)

(defn inc-var-fail-counter []
  (swap! *test-var-fails* inc))

(defn test-var-has-failures?
  "Predicate to test if the current test var has seen any errors or
  failures."
  []
  (pos? @*test-var-fails*))

;;; # Reporters
(def result (atom nil))

(defn result-with-vars
  [m]
  (assoc m
    :contexts (if (bound? #'test/*testing-contexts*)
                test/*testing-contexts*)
    :var (if (bound? #'test/*testing-vars*)
           (first test/*testing-vars*))))

(defn add-result!
  [m]
  (swap! result conj m))


;;; ## Results Reporter

;;; Reporter that will return its results.  Failures, etc, are printed
;;; to `test-out`.

(defmulti report :type)

(defmethod report :default [m])

(defmethod report :begin-test-ns [m]
  (print-begin-ns m))

(defmethod report :pass [m]
  (test/inc-report-counter :pass)
  (add-result! (result-with-vars m)))

(defmethod report :fail [m]
  (test/inc-report-counter :fail)
  (inc-var-fail-counter)
  (let [m (result-with-vars m)]
    (print-fail m)
    (add-result! m)))

(defmethod report :error [m]
  (test/inc-report-counter :error)
  (inc-var-fail-counter)
  (let [m (result-with-vars m)]
    (print-error m)
    (add-result! m)))

(defmethod report :summary [m])

(defmethod report :initialize [_]
  (reset! result []))

(defmethod report :results [_]
  @result)

;;; ## Silent Reporter

;;; Reporter that will return its results.  No printed output.
;;; Does not increment `clojure.test`'s test counters.

(defmulti silent-report :type)

(defmethod silent-report :default [m])

(defmethod silent-report :pass [m]
  (add-result! (result-with-vars m)))

(defmethod silent-report :fail [m]
  (inc-var-fail-counter)
  (add-result! (result-with-vars m)))

(defmethod silent-report :error [m]
  (inc-var-fail-counter)
  (add-result! (result-with-vars m)))

(defmethod silent-report :summary [m])

(defmethod silent-report :initialize [_]
  (reset! result []))

(defmethod silent-report :results [_]
  @result)

;;; # Test Var Sequences
(defn ns-test-vars
  "Return a sequence of all test vars in the namespace, ns."
  [ns]
  (filter (comp :test meta) (vals (ns-interns ns))))

(defn all-test-vars
  "Return a sequence of all test vars in all namespaces."
  []
  (mapcat ns-test-vars (all-ns)))

(defn var-matches-selectors?
  "Predicate for testing whether the var has metadata keys that
  matches selectors, a set of keywords."
  [selectors var]
  {:pre [(set? selectors)
         (instance? clojure.lang.Var var)]}
  (intersection (set (keys (meta var))) selectors))

;;; # Running Tests
(defn- test-var
  [var binding-map reporter]
  (with-bindings binding-map
    (binding [test/report reporter
              test/*testing-vars* nil
              *test-var-fails* (atom 0)]
      (test/test-var var))))

(defn- var-runner
  [ch {:keys [reporter] :or {reporter report} :as options}]
  (go-loop []
    (when-let [[var binding] (<! ch)]
      (do
        (test-var var binding reporter)
        (recur)))))

(defn- parallel-test-vars
  [vars {:keys [bindings threads]
         :or {bindings [{}]}
         :as options}]
  (let [c (chan)
        ts (doall
            (for [_ (range threads)]
              (var-runner c options)))]
    (doseq [[ns vars] (group-by (comp :ns meta) vars)]
      (let [once-fixture-fn (test/join-fixtures (::once-fixtures (meta ns)))
            each-fixture-fn (test/join-fixtures (::each-fixtures (meta ns)))]
        (once-fixture-fn
         (fn []
           (doseq [var vars
                   binding bindings]
             (each-fixture-fn (fn [] (>!! c [var binding]))))))))
    (close! c)
    (doseq [t ts]
      (<!! t))))

;; This function is from clojure.test/test-vars in clojure 1.6.
;; Copyright (c) Rich Hickey. All rights reserved.
(defn test-vars-with-fixtures
  "Groups vars by their namespace and runs test-vars on them with
   appropriate fixtures applied."
  [vars]
  (doseq [[ns vars] (group-by (comp :ns meta) vars)]
    (let [once-fixture-fn (test/join-fixtures (::once-fixtures (meta ns)))
          each-fixture-fn (test/join-fixtures (::each-fixtures (meta ns)))]
      (once-fixture-fn
       (fn []
         (doseq [v vars]
           (when (:test (meta v))
             (each-fixture-fn
              (fn []
                (binding [*test-var-fails* (atom 0)]
                  (test/test-var v)))))))))))

(defn- serial-test-vars
  [vars {:keys [bindings reporter]
         :or {bindings [{}] reporter report}
         :as options}]
  (binding [test/report reporter]
    (doseq [binding bindings]
      (with-bindings binding
        (test-vars-with-fixtures vars)))))

;;; # Test Runners

(defn test-vars
  "Execute the specified test vars.
  Options are:

  `:threads`
  : the number of threads to invoke tests on.  By default tests are invoked
    on a single thread.

  `:bindings`
  : a sequence of bindings, maps from vars to values (to be passed to
  `with-bindings`).  The tests are run once for each binding map in
  the sequence.

  `:reporter`
  : the function used to report results. It is called as specified in
  clojure.test, with the addition of being invoked with a
  `{:type :inititialize}` argument before any tests are run, and a
  `{:type :results}` argument after all the tests are run.  The
  default reporter builds a result sequence in an atom, which is
  `deref`'ed and returned when invoked with `:results`."
  ([vars {:keys [bindings reporter threads]
          :or {reporter report threads 1}
          :as options}]
     (assert (integer? threads) "The :threads value must be an integer")
     (assert (or (nil? bindings) (every? map? bindings))
             "The :bindings value must be a sequence of binding maps")
     (reporter {:type :initialize})
     (if (> threads 1)
       (parallel-test-vars vars options)
       (serial-test-vars vars options))
     (reporter {:type :results}))
  ([vars] (test-vars vars {})))

(defn test-ns
  "Runs tests from the namespace, ns.  The tests can be filtered based
  on a `:run?` predicate, called on each test var.  Also acccepts
  options as specified in `test-vars`.  By default, returns a sequence
  of test result maps."
  ([ns {:keys [run?] :as options}]
     (let [vars (cond->> (ns-test-vars ns)
                        run? (filter run?))]
       (test-vars vars options)))
  ([ns] (test-ns ns {}))
  ([] (test-ns *ns* {})))

(defn test-all
  "Runs tests from all namespaces.  The tests can be filtered based
  on a `:run?` predicate, called on each test var.  Also acccepts
  options as specified in `test-vars`.  By default, returns a sequence
  of test result maps."
  ([{:keys [run?] :as options}]
     (let [vars (cond->> (all-test-vars)
                         run? (filter run?))]
       (test-vars vars options)))
  ([] (test-all {})))

;;; # Processing Results
(defn summary [results]
  "Summarise the results into a map, as reported by clojure.test."
  {:test (count (distinct (filter :var results)))
   :pass (count (filter :pass results))
   :fail (count (filter :fail results))
   :error (count (filter :error results))})

(defn print-summary
  "Print the summary map in the same way as clojure.test."
  [m]
  (test/with-test-out
    (println "\nRan" (:test m) "tests containing"
             (+ (:pass m) (:fail m) (:error m)) "assertions.")
    (println (:fail m) "failures," (:error m) "errors.")))

(defn print-bad
  "Print fails and errors"
  [results]
  (test/with-test-out
    (doseq [{:keys [type] :as m} results]
      (case type
        :fail (print-fail m)
        :error (print-error m)
        nil))))
