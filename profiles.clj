{:dev {:dependencies [[org.clojure/clojure "1.6.0-beta2"]]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]
                 [lein-set-version "0.4.1"]]}
 :no-checkouts {:checkout-shares ^:replace []} ; disable checkouts
 :clojure-1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/api/0.1"
               :src-dir-uri "https://github.com/palletops/multi-test/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/source/0.1"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :release
 {:set-version {:updates [{:path "README.md" :no-snapshot true}]}}}
