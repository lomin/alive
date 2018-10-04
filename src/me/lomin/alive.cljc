(ns me.lomin.alive
  (:require [me.lomin.alive.core :as alive-core]
            [me.lomin.spectree.hiccup]
            [com.rpl.specter :as specter]
            #?(:clj [hiccup.core :as hiccup])))

#?(:clj (do
          (defn render [[tag dom]]
            (case tag
              ::document (hiccup/html {:mode :html}
                                      (specter/select-first #each/key [:html]
                                                            dom))
              ::element (hiccup/html {:mode :html} dom)))

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
                   [::element
                    (me.lomin.spectree/each+>> com.rpl.specter/transform
                                               ~@body
                                               (com.rpl.specter/select-first ~each-selector
                                                                             ~snippet))]))))))

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
   (contains? (alive-core/make-set-from-str (get-in node [alive-core/ATTRS :class])) a-class)))

(defn set-listener
  ([event f] #(set-listener event f %))
  ([event f node]
   (alive-core/update-attr assoc event f node)))

(defn set-attr
  ([k v] #(set-attr k v %))
  ([k v node]
   (update node alive-core/ATTRS assoc k v)))

(defn remove-attr [k node]
  (update node alive-core/ATTRS dissoc k))

(defn attr-contains
  ([k v] #(attr-contains k v %))
  ([k v node]
   (contains? (alive-core/make-set-from-str (get-in node [alive-core/ATTRS k])) v)))

(defn append
  ([content] #(append content %))
  ([content node]
   (conj node content)))

(defn content
  ([c] #(content c %))
  ([c node]
   (into [(first node) (second node)] (if (string? c) [c] c))))

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
          dom#)))))

(defn select [selector dom]
  (specter/select (alive-core/each selector) dom))