(ns me.lomin.alive.core
  (:require [com.rpl.specter :as s]
            [hickory.core :as h])
  #?(:cljs (:require-macros me.lomin.alive.core)))

#?(:clj
   (do
     (defn- trim-html [s]
       (-> s
           (clojure.string/replace #"\s\s+" " ")
           (clojure.string/replace #">\W*<" "><")))

     (defn- load-hiccup [resource]
       (-> (with-open [rdr (clojure.java.io/reader resource)]
             (trim-html (apply str
                               (map clojure.string/trim-newline
                                    (line-seq rdr)))))
           (h/parse)
           (h/as-hiccup)
           (vec)))

     (defmacro load-template-from-resource [resource]
       `~(load-hiccup resource))

     (defmacro load-template-from-path [path]
       `~(load-hiccup (-> (if (string? path)
                            path
                            (eval path))
                          (clojure.java.io/resource))))))

(def html-selector
  (s/recursive-path [path]
                    p
                    (s/if-path sequential?
                               (s/if-path path
                                          (s/continue-then-stay s/ALL p)
                                          [s/ALL p]))))

(defn make-selector [selector]
  (if (keyword? selector)
    (let [selector-name (name selector)
          first-char (first selector-name)]
      (cond
        (= \# first-char)
        (let [id (apply str (rest selector-name))]
          (html-selector [#(= id (:id (second %)))]))
        (= \. first-char)
        (let [class-name (apply str (rest selector-name))]
          (html-selector [#(if-let [class-str (:class (second %))]
                             (some #{class-name}
                                   (clojure.string/split class-str #" ")))]))
        :else (html-selector [s/FIRST #(= selector %)])))
    selector))

(defn make-path [path]
  (mapv make-selector path))

(defn make-snippet [template]
  (s/select-first (make-path [:body (s/nthpath 2)]) template))

(defn- make-set-from-str [class-str]
  (if class-str
    (set (clojure.string/split class-str #" "))
    #{}))

(defn replace-content
  ([content] #(replace-content content %))
  ([content node]
   (into [(first node) (second node)] content)))

(defn remove-attr [k node]
  (update node 1 dissoc k))

(defn update-attr [& args]
  (apply update (last args) 1 (drop-last args)))

(defn update-classes [f node]
  (update-in node
             [1 :class]
             (comp #(clojure.string/join " " (sort (vec %)))
                   f
                   make-set-from-str)))

(defn add-class
  ([a-class] #(add-class a-class %))
  ([a-class node]
   (update-classes #(conj % a-class) node)))

(defn remove-class
  ([a-class] #(remove-class a-class %))
  ([a-class node]
   (update-classes #(disj % a-class) node)))

(defn contains-class?
  ([a-class] #(contains-class? a-class %))
  ([a-class node]
   (contains? (make-set-from-str (get-in node [1 :class])) a-class)))

(defn set-listener
  ([event f] #(set-listener event f %))
  ([event f node]
   (update-attr assoc event f node)))