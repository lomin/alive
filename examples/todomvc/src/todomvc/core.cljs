(ns todomvc.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [secretary.core :as secretary]
            [me.lomin.alive.core :as alive]
            [me.lomin.alive.html :as html]
            [todomvc.events]
            [todomvc.subs]
            [todomvc.views]
            [me.lomin.alive.re-alive :as re-alive]
            [com.rpl.specter :as specter])
  (:import [goog History]
           [goog.history EventType]))


;; -- Debugging aids ----------------------------------------------------------
(enable-console-print!)                                     ;; so that println writes to `console.log`

;; -- Routes and History ------------------------------------------------------
;; Although we use the secretary library below, that's mostly a historical
;; accident. You might also consider using:
;;   - https://github.com/DomKM/silk
;;   - https://github.com/juxt/bidi
;; We don't have a strong opinion.
;;
(defroute "/" [] (re-frame/dispatch [:set-showing :all]))
(defroute "/:filter" [filter] (re-frame/dispatch [:set-showing (keyword filter)]))

(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


;; -- Entry Point -------------------------------------------------------------
;; Within ../../resources/public/index.html you'll see this code
;;    window.onload = function () {
;;      todomvc.core.main();
;;    }
;; So this is the entry function that kicks off the app once the HTML is loaded.
;;
(def log3 (comp re-alive/log re-alive/to-json))

(def main-dom (alive/select [::html/#app] (alive/load-template-from-path "public/index.html")))
(def toggle-all-done (alive/select [::html/#toggle-all] (alive/load-template-from-path "public/index.html")))
(def todo-done (alive/select [::html/li ::html/.completed] (alive/load-template-from-path "public/index.html")))
(def no-todos (alive/select [::html/#no-todos ::html/ul] (alive/load-template-from-path "public/index.html")))

(defn ^:export main
  []
  ;; Put an initial value into app-db.
  ;; The event handler for `:initialise-db` can be found in `events.cljs`
  ;; Using the sync version of dispatch means that value is in
  ;; place before we go onto the next step.
  (re-frame/dispatch-sync [:initialise-db])

  ;; Render the UI into the HTML's <div id="app" /> element
  ;; The view function `todomvc.views/todo-app` is the
  ;; root view for the entire UI.
  (reagent/render [todomvc.views/todo-app
                   main-dom
                   toggle-all-done
                   todo-done
                   no-todos]
                  (js/document.getElementById "app")))