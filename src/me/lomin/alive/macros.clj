(ns me.lomin.alive.macros)

(defn decorate* [arg]
  (cond
    (keyword? arg) (list 'me.lomin.alive.core/decorate arg)
    (vector? arg) (mapv decorate* arg)
    :else arg))

(defn walk* [arg]
  (cond
    (keyword? arg) (list 'me.lomin.alive.core/walk arg)
    (vector? arg) (mapv walk* arg)
    :else arg))

(defn walk-or-html-selector* [arg]
  (let [selector (walk* arg)]
    (if (= selector [])
      (walk* :html)
      selector)))

(defn pairs
  ([args] (pairs [] nil args))
  ([pairs spare args]
   (if-let [x0 (first args)]
     (if-let [x1 (second args)]
       (recur (conj pairs [x0 x1]) spare (nnext args))
       [pairs x0])
     [pairs spare])))

(defn +>>* [selector-wrapper f args]
  (let [[selector-transformation-pair coll] (pairs args)
        selector+transformer (for [[selector transformation] selector-transformation-pair]
                               (list f
                                     (selector-wrapper selector)
                                     transformation))]
    (if coll
      (concat (list '->> coll) selector+transformer)
      (let [sym (gensym)]
        (list 'fn [sym] (+>>* selector-wrapper f (concat args [sym])))))))