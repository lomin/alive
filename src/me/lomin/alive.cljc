(ns me.lomin.alive
  (:require [me.lomin.alive.core :as alive-core]
            [com.rpl.specter :as specter]
            [hickory.core :as h]
            #?(:clj [hiccup.core :as hiccup]))
  #?(:cljs (:require-macros me.lomin.alive)))

#?(:clj (do

          (defmacro load-template-from-resource [resource]
            `~(alive-core/load-hiccup h/parse alive-core/tag-as-document resource))

          (defmacro load-snippet-from-path [path]
            `~(alive-core/load-hiccup (comp first h/parse-fragment)
                                      alive-core/tag-as-element
                                      (-> (if (string? path)
                                            path
                                            (eval path))
                                          (clojure.java.io/resource))))

          (defmacro load-template-from-path [path]
            `~(alive-core/load-hiccup h/parse
                                      alive-core/tag-as-document
                                      (-> (if (string? path)
                                            path
                                            (eval path))
                                          (clojure.java.io/resource))))

          (defn render [[tag dom :as dom*]]
            (case tag
              ::document (hiccup/html {:mode :html}
                                      (specter/select-first #each/key [:html]
                                                            dom))
              ::element (hiccup/html {:mode :html} dom)
              (hiccup/html {:mode :html} dom*)))

          (defn- each* [arg]
            (cond
              (keyword? arg) (list 'me.lomin.spectree.keyword/each arg)
              (vector? arg) (mapv each* arg)
              :else arg))

          (defn- make-each-selector [selector]
            (if (seq selector)
              (each* selector)
              (each* [:html])))

          (defmacro defsnippet [name params snippet selector & body]
            (let [each-selector (make-each-selector selector)]
              `(def ~name
                 (fn ~params
                   (me.lomin.spectree/each+>> com.rpl.specter/transform
                                              ~@body
                                              (com.rpl.specter/select-first ~each-selector
                                                                            ~snippet))))))

          (defmacro clone-for [[n selector] & body]
            (let [each-selector (make-each-selector selector)]
              `(fn clone-for*#
                 ([dom#]
                  (let [nodes# (com.rpl.specter/select ~each-selector dom#)]
                    (clone-for*# dom# nodes#)))
                 ([dom# nodes#]
                  (if-let [~n (first nodes#)]
                    (recur (me.lomin.spectree/each+>> com.rpl.specter/transform
                                                      ~@body
                                                      dom#)
                           (rest nodes#))
                    dom#))))))

   :cljs (do

           (defn make-component [& args]
             (fn [_] (vec args)))))

(def TAG alive-core/TAG)
(def ATTRS alive-core/ATTRS)

(defn map-key [k]
  (specter/must k))

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
   (specter/setval (alive-core/each selector) NIL node)))

(defn content
  ([c] #(content c %))
  ([c node]
   (into [(first node) (second node)] (if (string? c) [c] c))))

(defn select [selector dom]
  (specter/select (alive-core/each selector) dom))