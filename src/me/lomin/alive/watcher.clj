(ns me.lomin.alive.watcher
  (:require [hawk.core :as hawk]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:gen-class))

(def reload-namespace "me.lomin.alive.reload")

(defn html-file? [_ {:keys [file]}]
  (and (.isFile file)
       (string/ends-with? (.getName file) ".html")))

(defn to-file-name [reload-namespace]
  (str (string/join "/" (cons ".alive" (string/split reload-namespace #"\.")))
       ".cljc"))

(defn -main
  "starts up the production system."
  [& args]
  (println "Watching resources ...")
  (hawk/watch! [{:paths   ["resources"]
                 :filter  html-file?
                 :handler (fn [ctx {:keys [file]}]
                            (println "detecting change at " (.getAbsolutePath file))
                            (let [file-name (to-file-name reload-namespace)]
                              (io/make-parents file-name)
                              (spit file-name
                                    (str "(ns " reload-namespace ")\n\n(def id " (rand-int 999999999) ")")))
                            ctx)}]))