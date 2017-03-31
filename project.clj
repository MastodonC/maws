(defproject maws "0.1.0-SNAPSHOT"
  :description "Generate AWS configuration files from EDN files"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :exclusions [[org.clojure/clojure]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-uuid "1.0.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [amazonica "0.3.94"]
                 [cljstache "2.0.0"]
                 [aero "1.1.2"]
                 [cheshire "5.7.0"]
                 [clj-http "3.4.1"]]
  :main ^:skip-aot maws.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-binplus "0.6.2"]]}}
  :bin {:name "maws"
        :bin-path "~/bin"}
  )
