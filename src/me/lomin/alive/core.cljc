(ns me.lomin.alive.core
  (:require [com.rpl.specter :as specter]
            [me.lomin.spectree.keyword :as spectree-keyword]
            [me.lomin.spectree.tree-search :as tree-search]
            [clojure.string :as string]
            [hickory.core :as h])
  #?(:cljs (:require-macros me.lomin.alive.core)))

#?(:clj
   (do
     (defn trim-html [s]
       (-> s
           (clojure.string/replace #"\s\s+" " ")
           (clojure.string/replace #">\W*<" "><")))

     (defn tag-as-element [hiccup]
       [:me.lomin.alive/element (vec hiccup)])

     (defn tag-as-document [hiccup]
       [:me.lomin.alive/document (vec hiccup)])

     (defn load-hiccup [parser tag resource]
       (-> (with-open [rdr (clojure.java.io/reader resource)]
             (trim-html (apply str
                               (map clojure.string/trim-newline
                                    (line-seq rdr)))))
           (parser)
           (h/as-hiccup)
           (tag)))))

(def TAG 0)
(def ATTRS 1)

(defn map-key [k]
  (specter/must k))

(defn each [selector]
  (cond
    (keyword? selector) (spectree-keyword/each selector)
    (vector? selector) (mapv each selector)
    :else selector))

(defn make-set-from-str [class-str]
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

(defn remove-attr [k node]
  (update node ATTRS dissoc k))

(defn update-attr [& args]
  (apply update (last args) ATTRS (drop-last args)))

(defn update-classes [f node]
  (update-in node
             [ATTRS :class]
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
   (contains? (make-set-from-str (get-in node [ATTRS :class])) a-class)))

(defn set-listener
  ([event f] #(set-listener event f %))
  ([event f node]
   (update-attr assoc event f node)))

(defn- make-component* [& args]
  (vec args))

(defn make-component [& args]
  (apply partial make-component* args))

(defn make-component2 [& args]
  (fn [_] (vec args)))

(defn- transform* [dom [raw-path transformation]]
  (specter/transform (each raw-path)
                     transformation
                     dom))

(defn transform [dom & args]
  (if (not (even? (count args)))
    [:p {} "FAIL: uneven path-transformation-pairs to transform"]
    (reduce transform* dom (partition 2 args))))

(defmethod spectree-keyword/selector nil [ns k ns+k]
  (tree-search/selector [specter/FIRST #(= k %)]))

(defmethod spectree-keyword/selector :. [ns k ns+k]
  (tree-search/selector [#(if-let [class-str (and (seqable? %) (:class (second %)))]
                            (some #{(name k)}
                                  (string/split class-str #" ")))]))

(defmethod spectree-keyword/selector :# [ns k ns+k]
  (tree-search/selector [#(= (name k) (:id (second %)))]))