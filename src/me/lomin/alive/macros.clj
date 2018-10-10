(ns me.lomin.alive.macros)

(defn each* [arg]
  (cond
    (keyword? arg) (list 'me.lomin.spectree.keyword/each arg)
    (vector? arg) (mapv each* arg)
    :else arg))

(defn tree* [arg]
  (cond
    (keyword? arg) (list 'me.lomin.spectree.tree-search/selector
                         (list 'me.lomin.spectree.keyword/each arg))
    (vector? arg) (mapv tree* arg)
    :else arg))

(defn tree-or-html-selector* [arg]
  (let [selector (tree* arg)]
    (if (= selector [])
      (tree* :html)
      selector)))

(defmacro each [arg]
  (each* arg))

(defmacro tree [arg]
  (tree* arg))