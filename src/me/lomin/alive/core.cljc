(ns me.lomin.alive.core
  (:refer-clojure :exclude [clone])
  (:require [com.rpl.specter :as specter]
            [me.lomin.alive.reload]
            [me.lomin.alive.selectors :refer [TAG ATTRS CONTENT] :as alive-selectors]
            #?(:clj [hickory.core :as hickory])))

(defn- walk* [walk-f wrapper-f selector]
  (cond
    (keyword? selector) (let [[wrapper sel] (alive-selectors/keyword-selector selector)]
                          (wrapper-f wrapper sel))
    (vector? selector) (mapv walk-f selector)
    :else selector))

(defn walk [selector]
  (walk* walk (fn [wrapper sel] (wrapper sel)) selector))

(defn walk-1 [selector]
  (walk* walk-1 (fn [_ sel] sel) selector))

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
           (hickory/as-hiccup)
           (tag)))))

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

(defn clone* [{:keys [insert expr last-index f child] :as context} node]
  (if-let [x (first expr)]
    (recur (-> context
               (update :expr rest)
               (assoc :last-index (inc last-index))
               (assoc :insert insert-after-index))
           (insert ((f x) child) last-index node))
    node))

(defn clone
  ([expr f indexes node]
   (clone expr f indexes 0 node))
  ([expr f indexes offset node]
   (if-let [[i child] (first indexes)]
     (recur expr
            f
            (rest indexes)
            (+ offset (max 0 (dec (count expr))))
            (clone* {:insert     insert-at-index
                     :expr       expr
                     :child      child
                     :last-index (+ i offset)
                     :f          f}
                    node))
     node)))