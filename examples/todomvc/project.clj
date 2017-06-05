(defproject todomvc "1.1.0"
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/clojurescript  "1.9.542"]
                 [reagent  "0.6.2"]
                 [re-frame "0.9.3"]
                 [com.rpl/specter "1.0.1"]
                 [hickory "0.7.1"]
                 [binaryage/devtools "0.8.1"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-figwheel  "0.5.10"]]

  :hooks [leiningen.cljsbuild]

  :source-paths ["src" "../../src"]

  :profiles {:dev {:cljsbuild
                   {:builds {:client {:figwheel     {:on-jsload "todomvc.core/main"}
                                      :compiler     {:main "todomvc.core"
                                                     :asset-path "js"
                                                     :optimizations :none
                                                     :source-map true
                                                     :source-map-timestamp true}}}}}

             :prod {:cljsbuild
                    {:builds {:client {:compiler    {:optimizations :advanced
                                                     :elide-asserts true
                                                     :pretty-print false}}}}}}

  :figwheel {:repl false}

  :clean-targets ^{:protect false} ["resources/public/js"]

  :cljsbuild {:builds {:client {:source-paths ["src"]
                                :compiler     {:output-dir "resources/public/js"
                                               :output-to  "resources/public/js/client.js"}}}})
