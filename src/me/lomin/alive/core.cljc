(ns me.lomin.alive.core
  (:require [com.rpl.specter :as specter]
            [clojure.string :as string]
            [hickory.core :as hiccup]
            [me.lomin.alive.selectors :as alive-selectors]))

(defn decorate [selector]
  (cond
    (keyword? selector) (alive-selectors/keyword-selector selector)
    (vector? selector) (mapv decorate selector)
    :else selector) )

(defn walk [selector]
  (cond
    (keyword? selector) (alive-selectors/walker (decorate selector))
    (vector? selector) (mapv walk selector)
    :else selector))

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
           (tag)))))

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

(defn clone* [{:keys [insert expr last-index f child] :as context} node]
  (if-let [x (first expr)]
    (recur (-> context
               (update :expr rest)
               (assoc :last-index (inc last-index))
               (assoc :insert insert-after-index))
           (insert ((f x) child) last-index node))
    node))

(defn clone [expr selector f indexes node]
  (clone* {:insert     insert-at-index
           :expr       expr
           :child      (specter/select-first [specter/ALL (decorate selector)]
                                             node)
           :last-index (last indexes)
           :f          f}
          node))