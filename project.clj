(defproject me.lomin/alive "0.2.0"
  :description "A selector-based (à la CSS) templating library for Clojure and ClojureScript, inspired by enlive."
  :url "https://github.com/lomin/alive"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [com.rpl/specter "0.13.2"]
                 [hickory "0.7.0"]]
  :cljsbuild {:builds
              [{:id           "test"
                :source-paths ["src" "test"]
                :compiler     {:output-to    "target/test.js"
                               :main         me.lomin.alive.doo-runner}}]}
  :profiles {:dev {:plugins [[lein-doo "0.1.7"]]}}
  :doo {:build "test"})

