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
     (pred (p<sub subscription))))
  ([pred subscription & args]
   (sub? (apply partial pred args) subscription)))

(defn make-state [[k :as subscription]]
  [k (<sub subscription)])


(comment header-input [dom]
  (alive/transform [:html/input]
                   (comp
                     (alive/transform [html/ATTRS]
                                      (comp
                                        (alive/translate :autofocus :auto-focus some?)
                                        (alive/transform [:value] (constantly @(subscribe [:header-input-value-sub])))))
                     (alive/set-listener :on-blur #(dispatch [:header-input-blur-event]))
                     (alive/set-listener :on-change #(dispatch [:header-input-change-event (-> % .-target .-value)]))
                     (alive/set-listener :on-key-down #(dispatch [:header-input-key-down-event (.-which %)])))
                   dom))

(comment todo-app [main-dom
                toggle-all-done
                todo-done
                no-todos]
  (alive/transform [:html/#header]
                   (alive/make-component header-input)
                   (comment [:html/#todo-list]
                            (alive/make-component todo-list))
                   (comment [:html/footer]
                            (alive/make-component footer-controls))
                   main-dom))