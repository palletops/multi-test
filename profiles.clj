{:dev {:dependencies [[org.clojure/clojure "1.6.0-beta2"]]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]
                 [lein-pallet-release "0.1.6"]]
       :pallet-release
       {:url "https://pbors:${GH_TOKEN}@github.com/palletops/multi-test.git",
        :branch "master"}}
 :clojure-1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/api/0.1"
               :src-dir-uri "https://github.com/palletops/multi-test/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/source/0.1"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}}
