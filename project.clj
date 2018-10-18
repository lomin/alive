(defproject me.lomin/alive "0.4.0"
  :description "A selector-based (à la CSS) templating library for Clojure and ClojureScript, inspired by enlive."
  :url "https://github.com/lomin/alive"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [com.rpl/specter "1.1.1"]
                 [hiccup "1.0.5"]
                 [hickory "0.7.1"]]

  :test-selectors {:default (constantly true)
                   :unit    :unit
                   :focused :focused
                   :all     (constantly true)}

  :profiles {:dev  {:resource-paths ["test-resources"]
                    :dependencies   [[thheller/shadow-cljs "2.6.13"]]}
             :test {:dependencies [[enlive "1.1.6"]]}})

