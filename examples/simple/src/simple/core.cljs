(ns simple.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [me.lomin.alive.core :as alive]))


;; -- Domino 1 - Event Dispatch -----------------------------------------------

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))  ;; <-- dispatch used

;; Call the dispatching function every second.
;; `defonce` is like `def` but it ensures only instance is ever
;; created in the face of figwheel hot-reloading of this file.
(defonce do-timer (js/setInterval dispatch-timer-event 1000))


;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-event-db              ;; sets up initial application state
  :initialize                 ;; usage:  (dispatch [:initialize])
  (fn [_ _]                   ;; the two parameters are not important here, so use _
    {:time (js/Date.)         ;; What it returns becomes the new application state
     :time-color "#f88"}))    ;; so the application state will initially be a map with two keys


(rf/reg-event-db                ;; usage:  (dispatch [:time-color-change 34562])
  :time-color-change            ;; dispatched when the user enters a new colour into the UI text field
  (fn [db [_ new-color-value]]  ;; -db event handlers given 2 parameters:  current application state and event (a vector)
    (assoc db :time-color new-color-value)))   ;; compute and return the new application state


(rf/reg-event-db                 ;; usage:  (dispatch [:timer a-js-Date])
  :timer                         ;; every second an event of this kind will be dispatched
  (fn [db [_ new-time]]          ;; note how the 2nd parameter is destructured to obtain the data value
    (assoc db :time new-time)))  ;; compute and return the new application state


;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :time
  (fn [db _]     ;; db is current app state. 2nd unused param is query vector
    (-> db
        :time)))

(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))


;; -- Domino 5 - View Functions ----------------------------------------------

(defn clock
  [dom]
  (alive/transform dom
                   [:.example-clock alive/ATTRS (alive/map-key :style) (alive/map-key :color)]
                   (constantly @(rf/subscribe [:time-color]))
                   [:.example-clock]
                   (alive/replace-content (-> @(rf/subscribe [:time])
                                              .toTimeString
                                              (clojure.string/split " ")
                                              first))))

(defn color-input
  [dom]
  (alive/transform dom
                   [:input alive/ATTRS (alive/map-key :value)]
                   (constantly @(rf/subscribe [:time-color]))
                   [:input]
                   (alive/set-listener :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)]))))

(defn ui
  [dom]
  (alive/transform dom
                   [:.example-clock]
                   (alive/make-component clock)
                   [:.color-input]
                   (alive/make-component color-input)))

;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])                          ;; puts a value into application state
  (reagent/render [ui (alive/select-snippet [:#app]         ;; mount the application's ui into '<div id="app" />'
                                            (alive/load-template-from-path "public/example.html"))]
                  (js/document.getElementById "app")))

