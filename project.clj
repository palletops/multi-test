(defproject com.palletops/multi-test "0.1.0-SNAPSHOT"
  :description "An alternative runner for clojure.test tests"
  :url "http://palletops.com/multi-test"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/core.async "0.1.267.0-0d7780-alpha"
                  :exclude [org.clojure/clojure]]
                 [org.clojure/clojure "1.5.1" :scope "provided"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0-beta2"]]}
             :clojure-1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})
