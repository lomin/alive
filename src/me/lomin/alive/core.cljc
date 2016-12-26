(ns me.lomin.alive.core
  (:require [com.rpl.specter :as s]
    #?(:clj [me.lomin.alive.macros :as alive-macros]))
  #?(:cljs (:require-macros [me.lomin.alive.macros :as alive-macros])))

(defn- to-s [xs]
  (clojure.string/join " " (sort (vec xs))))

(defn- as-set [class-str]
  (set (clojure.string/split class-str #" ")))

(defn replace-content
  ([content] #(replace-content content %))
  ([content node]
   (into [(first node) (second node)] content)))

(defn remove-attr [k node]
  (update node 1 dissoc k))

(defn update-attr [& args]
  (apply update (last args) 1 (drop-last args)))

(defn update-class [f node]
  (update-in node [1 :class] (comp to-s f as-set)))

(defn add-class
  ([a-class] #(add-class a-class %))
  ([a-class node]
   (update-class #(conj % a-class) node)))

(defn remove-class
  ([a-class] #(remove-class a-class %))
  ([a-class node]
   (update-class #(disj % a-class) node)))

(defn has-class?
  ([a-class] #(has-class? a-class %))
  ([a-class node]
   (contains? (as-set (get-in node [1 :class])) a-class)))

(defn set-listener
  ([event f] #(set-listener event f %))
  ([event f node]
   (update-attr #(assoc % event f) node)))