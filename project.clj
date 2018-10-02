(defproject me.lomin/alive "1.0.0"
  :description "A selector-based (à la CSS) templating library for Clojure and ClojureScript, inspired by enlive."
  :url "https://github.com/lomin/alive"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [com.rpl/specter "1.1.1"]
                 [hickory "0.7.1"]
                 [hawk "0.2.11"]
                 [me.lomin.spectree "0.1.0"]]

  :test-selectors {:default (constantly true)
                   :focused :focused}

  :doo {:build "test"}

  :profiles {:dev  {:dependencies [[pjstadig/humane-test-output "0.8.3"]]
                    :plugins      [[lein-doo "0.1.10"]]
                    :cljsbuild
                                  {:builds {:test   {:source-paths ["src" "test"]
                                                     :compiler     {:output-to     "target/testable.js"
                                                                    :output-dir    "target"
                                                                    :main          me.lomin.alive.test-runner
                                                                    :optimizations :none}}}}}
             }
  :clean-targets ^{:protect false} ["resources/public/js" "target"])

