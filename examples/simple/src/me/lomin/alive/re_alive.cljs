(ns me.lomin.alive.re-alive
  (:require [re-frame.core :as re-frame]))

(def log (.-log js/console))

(defn log2
  ([prefix x]
   (log prefix x)
   x)
  ([x]
   (log x)
   x))

(defn to-json [x]
  (.stringify js/JSON (clj->js x)))

(defn <sub [subscription]
  (->> subscription
       (re-frame/subscribe)
       (deref)
       (log2 (to-json subscription))))

(defn sub?
  ([subscription]
   (sub? boolean subscription))
  ([pred subscription]
   (fn [_]
     (pred (<sub subscription))))
  ([pred subscription & args]
   (sub? (apply partial pred args) subscription)))

(defn make-state [[k :as subscription]]
  [k (<sub subscription)])