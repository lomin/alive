(ns me.lomin.alive.core
  (:require [me.lomin.alive.html :as html]
            [com.rpl.specter :as specter]
            [hickory.core :as hickory])
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
           (hickory/parse)
           (hickory/as-hiccup)
           (vec)))

     (defmacro load-template-from-resource [resource]
       `~(load-hiccup resource))

     (defmacro load-template-from-path [path]
       `~(load-hiccup (-> (if (string? path)
                            path
                            (eval path))
                          (clojure.java.io/resource))))))

(def html-selector
  (specter/recursive-path [path]
                          p
                          (specter/if-path sequential?
                                           (specter/if-path path
                                                            (specter/continue-then-stay specter/ALL p)
                                                            [specter/ALL p]))))

(defn make-selector [selector]
  (cond
    (and (keyword? selector)
         (= "me.lomin.alive.html" (namespace selector)))
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
        :else (html-selector [specter/FIRST #(= (keyword selector-name) %)])))
    (number? selector) (specter/nthpath selector)
    :else selector))

(defn not-selected? [selector]
  (specter/not-selected? (make-selector selector)))

(defn make-path [path]
  (mapv make-selector path))

(defn- make-set-from-str [class-str]
  (if class-str
    (set (clojure.string/split class-str #" "))
    #{}))

(defn add-content
  ([content] #(add-content content %))
  ([content node]
   (conj node content)))

(defn replace-content
  ([content] #(replace-content content %))
  ([content node]
   (into [(first node) (second node)] content)))

(defn remove-content
  ([node]
   [(first node) (second node)]))

(defn remove-attr [k node]
  (update node html/ATTRS dissoc k))

(defn update-attr [& args]
  (apply update (last args) html/ATTRS (drop-last args)))

(defn update-classes [f node]
  (update-in node
             [html/ATTRS :class]
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
   (contains? (make-set-from-str (get-in node [html/ATTRS :class])) a-class)))

(defn set-listener
  ([event f] #(set-listener event f %))
  ([event f node]
   (update-attr assoc event f node)))

(defn make-component [& args]
  #(conj (vec args) %))

(defn- do-transform [dom [raw-path transformation]]
  (specter/transform (make-path raw-path)
                     transformation
                     dom))

(defn- pairs
  ([args] (pairs [] nil args))
  ([pairs spare args]
   (if-let [x0 (first args)]
     (if-let [x1 (second args)]
       (recur (conj pairs [x0 x1]) spare (nnext args))
       [pairs x0])
     [pairs spare])))

(defn transform* [transformations dom]
  (reduce do-transform dom transformations))

(defn transform [& args]
  (let [[transformations dom] (pairs args)]
    (if dom
      (reduce do-transform dom transformations)
      #(reduce do-transform % transformations))))

(defn filter-sub-states [state pairs]
  (specter/select [(specter/filterer specter/FIRST #(clojure.set/subset? % state))
                   specter/ALL
                   specter/LAST]
                  pairs))

(defn chain-fns [x f] (f x))

(defn choose-state [state & args]
  (let [[state-to-transformer-pairs dom] (pairs args)
        transformers (filter-sub-states state state-to-transformer-pairs)]
    (if dom
      (reduce chain-fns dom transformers)
      #(reduce chain-fns % transformers))))

(defn select [selector dom]
  (specter/select-first (make-path selector) dom))