(defproject me.lomin/alive "0.4.0"
  :description "A selector-based (à la CSS) templating library for Clojure and ClojureScript, inspired by enlive."
  :url "https://github.com/lomin/alive"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [com.rpl/specter "1.1.1"]
                 [hiccup "1.0.5"]
                 [me.lomin/spectree "0.2.0"]
                 [hickory "0.7.1"]]

  :test-selectors {:default   (constantly true)
                   :unit      :unit
                   :focused   :focused
                   :all       (constantly true)}

  :cljsbuild {:builds
              [{:id           "test"
                :source-paths ["src" "test"]
                :compiler     {:output-to    "target/test.js"
                               :main         me.lomin.alive.doo-runner}}]}
  :profiles {:dev {:plugins [[lein-doo "0.1.10"]]
                   :dependencies [[enlive "1.1.6"]]}
             :test {:resource-paths ["test-resources"]}}
  :doo {:build "test"})

