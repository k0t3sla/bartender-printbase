(defproject bartender-printbase "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "localhost:=)))"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 
                 [metosin/reitit "0.3.9"]
                 [http-kit "2.7.0"]
                 [ring/ring-defaults "0.3.2"]
                 [yogthos/config "1.2.0"]
                 [com.github.igrishaev/pg2-core "0.1.2"]
                 [com.github.igrishaev/pg2-honey "0.1.2"]
                 
                 ]
  :main ^:skip-aot bartender-printbase.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
