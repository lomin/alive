(defproject simple "1.1.0"
  :dependencies [[org.clojure/clojure        "1.8.0"]
                 [org.clojure/clojurescript  "1.9.542"]
                 [reagent  "0.6.2"]
                 [re-frame "0.9.3"]
                 [com.rpl/specter "1.0.1"]
                 [hickory "0.7.1"]]

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-figwheel  "0.5.10"]]

  :hooks [leiningen.cljsbuild]

  :source-paths ["src" "../../src"]

  :profiles {:dev {:cljsbuild
                   {:builds {:client {:figwheel     {:on-jsload "simple.core/run"}
                                      :compiler     {:main "simple.core"
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
