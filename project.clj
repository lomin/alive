(defproject me.lomin/alive "0.1.0"
  :description "A selector-based (à la CSS) templating library for Clojure and ClojureScript, inspired by enlive."
  :url "https://github.com/lomin/alive"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.rpl/specter "0.12.0"]
                 [hickory "0.6.0"]]
  :cljsbuild {:builds
              [{:id           "test"
                :source-paths ["src" "test"]
                :compiler     {:output-to    "target/test.js"
                               :main         me.lomin.alive.doo-runner}}]}
  :profiles {:dev {:plugins [[lein-doo "0.1.7"]
                             [com.jakemccrary/lein-test-refresh "0.16.0"]
                             [jonase/eastwood "0.2.3"]
                             [lein-kibit "0.1.2"]
                             [lein-cljfmt "0.5.6"]]}}
  :doo {:build "test"})

