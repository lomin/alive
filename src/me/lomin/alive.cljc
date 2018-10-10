(ns me.lomin.alive
  (:require [me.lomin.alive.core :as alive-core]
            #?(:clj [me.lomin.alive.macros :as alive-macros])
            [com.rpl.specter :as specter]
            [hickory.core :as h]
            #?(:clj [hiccup.core :as hiccup]))
  #?(:cljs (:require-macros me.lomin.alive)))


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
   (specter/setval (alive-core/tree selector) NIL node)))

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

           (def html-tag-selector (alive-macros/tree :html))

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
             (cons 'me.lomin.alive.core/tree+>>
                   (cons 'com.rpl.specter/transform
                         body)))

           (defmacro transform2 [& body]
             (cons 'me.lomin.alive.core/each+>>
                   (cons 'com.rpl.specter/transform
                         body)))

           (defmacro defsnippet [name params snippet selector & body]
             (let [tree-selector (alive-macros/tree-or-html-selector* selector)]
               `(def ~name
                  (fn ~params
                    (me.lomin.alive.core/tree+>> com.rpl.specter/transform
                                                 ~@body
                                                 (com.rpl.specter/select-first ~tree-selector
                                                                               ~snippet))))))

           (def MAYBE-ALL (specter/comp-paths seqable? specter/ALL))
           (def SECOND (specter/nthpath 1))

           (defmacro clone-for
             ([bind-expr selector transformation]
              (list 'me.lomin.alive/clone-for bind-expr selector transformation nil))
             ([[bind expr] selector transformation node]
              (let [sel* (alive-macros/each* selector)
                    seq-f (list 'fn [bind] transformation)]
                (cons 'me.lomin.alive/transform2
                      (cons [(list 'com.rpl.specter/putval expr)
                             (list 'com.rpl.specter/putval sel*)
                             (list 'com.rpl.specter/putval seq-f)
                             (list 'me.lomin.spectree.tree-search/selector ['me.lomin.alive/MAYBE-ALL sel*])
                             (list 'com.rpl.specter/collect ['com.rpl.specter/INDEXED-VALS ('com.rpl.specter/selected? ['me.lomin.alive/SECOND sel*]) 'com.rpl.specter/FIRST])]
                            (cons 'me.lomin.alive.core/clone
                                  (when node (list node)))))))))

   :cljs (do

           (defn make-component [& args]
             (fn [_] (vec args)))))

(defn select [selector dom]
  (specter/select (alive-core/tree selector) dom))