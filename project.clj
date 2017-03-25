(defproject aws-cfg-gen "0.1.0-SNAPSHOT"
  :description "Generate AWS configuration files from EDN files"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :exclusions [[org.clojure/clojure]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-uuid "1.0.0"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main ^:skip-aot aws-cfg-gen.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  )
