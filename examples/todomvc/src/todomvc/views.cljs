(ns todomvc.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [me.lomin.alive.core :as alive]
            [me.lomin.alive.html :as html]
            [me.lomin.alive.re-alive :as re-alive]))

clojure.data/diff
(defn todo-input [{:keys [title on-save on-stop]}]
  (let [val (reagent/atom title)
        stop #(do (reset! val "")
                  (when on-stop (on-stop)))
        save #(let [v (-> @val str clojure.string/trim)]
                (when (seq v) (on-save v))
                (stop))]
    (fn [props]
      [:input (merge props
                     {:type        "text"
                      :value       @val
                      :auto-focus  true
                      :on-blur     save
                      :on-change   #(reset! val (-> % .-target .-value))
                      :on-key-down #(case (.-which %)
                                      13 (save)
                                      27 (stop)
                                      nil)})])))


(defn todo-item
  []
  (let [editing (reagent/atom false)]
    (fn [{:keys [id done title]}]
      [:li {:class (str (when done "completed ")
                        (when @editing "editing"))}
       [:div.view
        [:input.toggle
         {:type      "checkbox"
          :checked   done
          :on-change #(dispatch [:toggle-done id])}]
        [:label
         {:on-double-click #(reset! editing true)}
         title]
        [:button.destroy
         {:on-click #(dispatch [:delete-todo id])}]]
       (when @editing
         [todo-input
          {:class   "edit"
           :title   title
           :on-save #(dispatch [:save id %])
           :on-stop #(reset! editing false)}])])))


(defn task-list
  []
  (let [visible-todos @(subscribe [:visible-todos])
        all-complete? @(subscribe [:all-complete?])]
    [:section#main
     [:input#toggle-all
      {:type      "checkbox"
       :checked   all-complete?
       :on-change #(dispatch [:complete-all-toggle (not all-complete?)])}]
     [:label
      {:for "toggle-all"}
      "Mark all as complete"]
     [:ul#todo-list
      (for [todo visible-todos]
        ^{:key (:id todo)} [todo-item todo])]]))


(defn footer-controls [dom]
  (let [[active done] @(subscribe [:footer-counts])
        showing @(subscribe [:showing])
        a-fn (fn [filter-kw txt]
               [:a {:class (when (= filter-kw showing) "selected")
                    :href  (str "#/" (name filter-kw))} txt])]
    [:footer#footer
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:li (a-fn :all "All")]
      [:li (a-fn :active "Active")]
      [:li (a-fn :done "Completed")]]
     (when (pos? done)
       [:button#clear-completed {:on-click #(dispatch [:clear-completed])}
        "Clear completed"])]))


(defn todo-list [dom]
  (when (seq @(subscribe [:todos]))
    [task-list]))

(defn todo-app [main-dom
                toggle-all-done
                todo-done
                no-todos]
  (alive/transform [::html/header]
                   (alive/set-listener :on-save #(dispatch [:add-todo %]))
                   (comment [::html/#todo-list]
                            (alive/make-component todo-list))
                   (comment [::html/footer]
                            (alive/make-component footer-controls))
                   main-dom)
  main-dom)
