(defproject data-extract-clj "1.0.0"
  :description "Takes SQL queries from file and writes result as xlsx files"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [cheshire "5.10.0"]
                 [dk.ative/docjure "1.12.0"]
                 [org.postgresql/postgresql "42.2.11"]
                 [com.oracle.database.jdbc/ojdbc10 "19.3.0.0"]]
  :main ^:skip-aot data-extract-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
