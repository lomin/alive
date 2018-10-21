(ns me.lomin.alive
  (:require [me.lomin.alive.core :as alive-core]
            [me.lomin.alive.selectors :as alive-selectors]
            [com.rpl.specter :as specter]
            #?(:clj [me.lomin.alive.macros :as alive-macros])
            #?(:clj [hickory.core :as hickory])
            #?(:clj [hiccup.core :as hiccup]))
  #?(:cljs (:require-macros me.lomin.alive)))

(def TAG alive-selectors/TAG)
(def ATTRS alive-selectors/ATTRS)
(def CONTENT alive-selectors/CONTENT)
(def MAYBE-ALL me.lomin.alive.selectors/MAYBE-ALL)

(defn add-class
  ([a-class] #(add-class a-class %))
  ([a-class node]
   (alive-core/update-classes #(conj % a-class) node)))

(defn remove-class
  ([a-class] #(remove-class a-class %))
  ([a-class node]
   (alive-core/update-classes #(disj % a-class) node)))

(defn class-contains
  ([a-class] #(class-contains a-class %))
  ([a-class node]
   (contains? (alive-core/make-set-from-str (get-in node [ATTRS :class])) a-class)))

(defn set-listener
  ([event f] #(set-listener event f %))
  ([event f node]
   (alive-core/update-attr assoc event f node)))

(defn set-attr
  ([k v] #(set-attr k v %))
  ([k v node]
   (update node ATTRS assoc k v)))

(defn remove-attr [k node]
  (update node ATTRS dissoc k))

(defn attr-contains
  ([k v] #(attr-contains k v %))
  ([k v node]
   (contains? (alive-core/make-set-from-str (get-in node [ATTRS k])) v)))

(defn append
  ([content] #(append content %))
  ([content node]
   (conj node content)))

(def NIL specter/NONE)

(defn none
  ([] (none (constantly true)))
  ([selector] #(none selector %))
  ([selector node]
   (specter/setval (alive-core/walk selector) NIL node)))

(defn substitute
  ([c] #(substitute c %))
  ([c node]
   (if (vector? c) c [c {}])))

(defn content
  ([c] #(content c %))
  ([c node]
   (into [(first node) (or (second node) {})] (if (string? c) [c] c))))

#?(:clj  (do

           (defmacro load-template-from-resource [resource]
             `~(alive-core/load-hiccup hickory/parse alive-core/tag-as-document resource))

           (defmacro load-snippet-from-path [path]
             `~(alive-core/load-hiccup (comp first hickory/parse-fragment)
                                       alive-core/tag-as-element
                                       (-> (if (string? path)
                                             path
                                             (eval path))
                                           (clojure.java.io/resource))))

           (defmacro load-template-from-path [path]
             `~(alive-core/load-hiccup hickory/parse
                                       alive-core/tag-as-document
                                       (-> (if (string? path)
                                             path
                                             (eval path))
                                           (clojure.java.io/resource))))

           (def html-tag-selector (alive-core/walk :html))

           (defn render [[tag dom :as dom*]]
             (cond
               (vector? dom*) (case tag
                                ::document (hiccup/html {:mode :html}
                                                        (specter/select-first html-tag-selector
                                                                              dom))
                                ::element (hiccup/html {:mode :html} dom)
                                (hiccup/html {:mode :html} dom*))
               (string? dom*) dom*
               :else ""))

           (defmacro transform [& body]
             (me.lomin.alive.macros/+>>* alive-macros/walk*
                                         'com.rpl.specter/transform
                                         body))

           (defmacro defsnippet [name params snippet selector & body]
             (let [tree-selector (alive-macros/walk-or-html-selector* selector)]
               `(def ~name
                  (fn ~params
                    (me.lomin.alive/transform
                      ~@body
                      (com.rpl.specter/select-first ~tree-selector
                                                    ~snippet))))))

           (defmacro clone-for [[bind expr] parent-selector & selector+transformations&coll]
             (let [[selector+transformations coll] (alive-macros/pairs selector+transformations&coll)
                   walking-parent-selector (list 'me.lomin.alive.core/walk parent-selector)
                   seq-f (list 'fn [bind] (cons 'me.lomin.alive/transform (mapcat conj selector+transformations)))]
               (cons 'me.lomin.alive/transform
                     (cons [(list 'com.rpl.specter/putval expr)
                            (list 'com.rpl.specter/putval seq-f)
                            (list 'com.rpl.specter/selected? walking-parent-selector)
                            (list 'com.rpl.specter/collect
                                  ['com.rpl.specter/INDEXED-VALS
                                   (list 'com.rpl.specter/selected? [1 (list 'me.lomin.alive.core/walk-1 parent-selector)])])]
                           (cons 'me.lomin.alive.core/clone
                                 (when coll (list coll))))))))

   :cljs (do

           (defn insert-component
             ([component]
              (fn [node] [component node]))
             ([component & args]
              (fn [node] (conj (into [component] args) node))))

           (defn insert-snippet [& args]
             (fn [_] (vec args)))))

(defn select [selector dom]
  (specter/select (alive-core/walk selector) dom))

(defn select-first [selector dom]
  (specter/select-first (alive-core/walk selector) dom))