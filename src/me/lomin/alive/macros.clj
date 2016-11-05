(ns me.lomin.alive.macros
  (:require [hickory.core :as h]
            [com.rpl.specter :as s]
            [com.rpl.specter.macros :as sm]))

(defn trim-html [s]
  (-> s
      (clojure.string/replace #"\s\s+" " ")
      (clojure.string/replace #">\W*<" "><")))

(defn get-template [resource]
  (with-open [rdr (clojure.java.io/reader resource)]
    (trim-html (apply str (map clojure.string/trim-newline (line-seq rdr))))))

(defn- get-template-str
  ([path]
   (get-template (clojure.java.io/resource path))))

(defn get-hiccup-snippet [path]
  (h/as-hiccup (first (h/parse-fragment (get-template-str path)))))

(defn get-hiccup [resource]
  (vec (h/as-hiccup (h/parse (get-template resource)))))

(defn get-hiccup-from-path [path]
  (vec (h/as-hiccup (h/parse (get-template-str path)))))

(defn- as-path-str [path] (if (string? path) path (eval path)))

(defmacro defsnippet [sym path]
  `(def ~sym ~(get-hiccup-snippet (as-path-str path))))

(defmacro deftemplate [sym path]
  `(def ~sym ~(get-hiccup-from-path (as-path-str path))))

(defmacro import-attrs [attrs-name]
  `(do
     (sm/declarepath ~attrs-name)
     (sm/providepath ~attrs-name [(s/srange 1 2) s/FIRST])))

(import-attrs attrs)

(defmacro import-tag [selector]
  (let [selector-keyword (keyword selector)]
    `(do
       (sm/declarepath ~selector)
       (sm/providepath ~selector
                       (s/if-path sequential?
                                  (s/if-path [s/FIRST #(= ~selector-keyword %)]
                                             (s/continue-then-stay s/ALL ~selector)
                                             [s/ALL ~selector]))))))
(defmacro import-class [selector]
  (let [class-name (apply str (rest (name selector)))]
    `(do
       (sm/declarepath ~selector)
       (sm/providepath ~selector
                       (s/if-path sequential?
                                  (s/if-path [#(some #{~class-name}
                                                     (clojure.string/split (str (:class (second %))) #" "))]
                                             (s/continue-then-stay s/ALL ~selector)
                                             [s/ALL ~selector]))))))
(defmacro import-id [selector]
  (let [id-name (apply str (drop 3 (name selector)))]
    `(do
       (sm/declarepath ~selector)
       (sm/providepath ~selector
                       (s/if-path sequential?
                                  (s/if-path [#(= ~id-name (str (:id (second %))))]
                                             (s/continue-then-stay s/ALL ~selector)
                                             [s/ALL ~selector]))))))