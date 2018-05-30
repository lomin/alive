(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive]
            [me.lomin.alive.html :as html]
            [hickory.core :as hickory]
            [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [re-frame.subs :as subs]
            [com.rpl.specter :as specter]

            [clojure.test :refer :all]))

(deftest ^:focused base-html-test
  (let [template (alive/load-template-from-path "public/index.html")]
    (is (= [] (alive/transform [identity (specter/filterer #(not= :label %))]
                               specter/NONE
                               template)))))


(comment
  (re-frame/reg-sub :willi (fn [db _] (:willi db)))

  (reset! subs/query->reaction {})
  (subs/clear-all-handlers!)
  (require '[todomvc.subs :as sub2])
  (re-frame/reg-sub :showing (fn [db _] (:willi db)))

  (def safe-deref (fnil deref (atom nil)))

  (deftest ^:unit transforms-to-reagent-props-test
    (swap! db/app-db assoc :showing 1)
    (swap! db/app-db assoc :showing 2)
    (is (= 3 (safe-deref (re-frame/subscribe [:willi]))))))

