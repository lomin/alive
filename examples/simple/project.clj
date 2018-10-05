(defproject simple "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [com.rpl/specter "1.1.1"]
                 [hiccup "1.0.5"]
                 [me.lomin/spectree "0.2.0"]
                 [hickory "0.7.1"]
                 [reagent  "0.7.0"]
                 [re-frame "0.10.5"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel  "0.5.14"]]

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
