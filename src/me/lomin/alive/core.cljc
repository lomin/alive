(ns me.lomin.alive.core
  (:require [com.rpl.specter :as specter]
            #?(:clj [me.lomin.alive.macros :as alive-macros])
            [me.lomin.spectree.keyword :as spectree-keyword]
            [me.lomin.spectree.tree-search :as tree-search]
            [clojure.string :as string]
            [hickory.core :as hiccup])
  #?(:cljs (:require-macros me.lomin.alive.core)))

(defn each [selector]
  (cond
    (keyword? selector) (spectree-keyword/each selector)
    (vector? selector) (mapv each selector)
    :else selector))

(defn tree [selector]
  (cond
    (keyword? selector) (tree-search/selector (spectree-keyword/each selector))
    (vector? selector) (mapv tree selector)
    :else selector))

(defn pairs
  ([args] (pairs [] nil args))
  ([pairs spare args]
   (if-let [x0 (first args)]
     (if-let [x1 (second args)]
       (recur (conj pairs [x0 x1]) spare (nnext args))
       [pairs x0])
     [pairs spare])))

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
           (hiccup/as-hiccup)
           (tag)))

     (defn- +>>* [selector-wrapper f args]
       (let [[selector-transformation-pair coll] (pairs args)
             selector+transformer (for [[selector transformation] selector-transformation-pair]
                                    (list f
                                          (selector-wrapper selector)
                                          transformation))]
         (if coll
           (concat (list '->> coll) selector+transformer)
           (let [sym (gensym)]
             (list 'fn [sym] (+>>* selector-wrapper f (concat args [sym])))))))

     (defmacro each+>> [f & args]
       (+>>* alive-macros/each* f args))

     (defmacro tree+>> [f & args]
       (+>>* alive-macros/tree* f args))

     (defmacro +>> [f & args]
       (+>>* identity f args))))

(def TAG 0)
(def ATTRS 1)

(defn make-set-from-str [class-str]
  (if class-str
    (set (clojure.string/split class-str #" "))
    #{}))

(defn update-attr [& args]
  (apply update (last args) ATTRS (drop-last args)))

(defn update-classes [f node]
  (update-in node
             [ATTRS :class]
             (comp #(clojure.string/join " " (sort (vec %)))
                   f
                   make-set-from-str)))

(defn insert-after-index [val index node]
  (specter/setval (specter/before-index index)
                  val
                  node))

(defn insert-at-index [val index node]
  (specter/setval (specter/nthpath index)
                  val
                  node))

(defn clone*
  ([{:keys [insert expr last-index f child] :as context} node]
   (if-let [x (first expr)]
     (recur (-> context
                (update :expr rest)
                (assoc :last-index (inc last-index))
                (assoc :insert insert-after-index))
            (insert ((f x) child) last-index node))
     node)))

(defn clone
  ([expr selector f indexes node]
   (clone* {:insert      insert-at-index
             :expr       expr
             :child      (specter/select-first [specter/ALL (me.lomin.alive.core/each selector)]
                                               node)
             :last-index (last indexes)
             :f          f}
           node)))

(defn tag= [t]
  (specter/comp-paths seqable? (specter/selected? [specter/FIRST (specter/pred= t)])))

(defmethod spectree-keyword/selector nil [ns k ns+k]
  (tag= k))

(defmethod spectree-keyword/selector :. [ns k ns+k]
  [seqable? #(if-let [class-str (:class (second %))]
               (some #{(name k)}
                     (string/split class-str #" ")))])

(defmethod spectree-keyword/selector :# [ns k ns+k]
  [seqable? #(= (name k) (:id (second %)))])

(defmethod spectree-keyword/selector :> [ns k ns+k]
  (tag= k))

(defmethod spectree-keyword/selector :>. [ns k ns+k]
  [seqable? #(if-let [class-str (:class (second %))]
               (some #{(name k)}
                     (string/split class-str #" ")))])

(defmethod spectree-keyword/selector :># [ns k ns+k]
  [seqable? #(= (name k) (:id (second %)))])

(defmethod spectree-keyword/selector :key [_ k _]
  (specter/must k))