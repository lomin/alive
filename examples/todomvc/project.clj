(defproject me.lomin.alive.todomvc "1.1.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [com.rpl/specter "1.1.1"]
                 [reagent "0.6.2"]
                 [re-frame "0.10.5"]
                 [hickory "0.7.1"]
                 [binaryage/devtools "0.8.1"]
                 [secretary "1.2.3"]
                 [hawk "0.2.11"]
                 [me.lomin.spectree "0.1.0"]]

  :plugins [[lein-figwheel "0.5.16"]]

  :source-paths [".alive" "src" "../../src"]

  :doo {:build "test"
        :alias {:default [:phantom]}}

  :profiles {:dev  {:dependencies [[pjstadig/humane-test-output "0.8.3"]
                                   [clj-diff "1.0.0-SNAPSHOT"]]
                    :plugins      [[lein-doo "0.1.10"]]
                    :cljsbuild
                                  {:builds {:test   {:source-paths ["src" "test"]
                                                     :compiler     {:output-to     "target/testable.js"
                                                                    :output-dir    "target"
                                                                    :main          me.lomin.alive.test-runner
                                                                    :optimizations :none}}
                                            :client {:compiler {:asset-path           "js"
                                                                :optimizations        :none
                                                                :source-map           true
                                                                :source-map-timestamp true
                                                                :main                 "todomvc.core"
                                                                :closure-defines      {"me.lomin.alive.core.DEV" true}}
                                                     :figwheel {:on-jsload "todomvc.core/main"}}}}}

             :prod {:cljsbuild
                    {:builds {:client {:compiler {:optimizations :advanced
                                                  :elide-asserts true
                                                  :pretty-print  false}}}}}}

  :figwheel {:server-port 3450
             :repl        true}

  :clean-targets ^{:protect false} ["resources/public/js" "target"]

  :cljsbuild {:builds {:client {:source-paths ["src" ".alive"]
                                :compiler     {:output-dir "resources/public/js"
                                               :output-to  "resources/public/js/client.js"}}}}

  :main me.lomin.alive.watcher)
