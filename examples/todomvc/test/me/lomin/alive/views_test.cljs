(ns me.lomin.alive.views-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [todomvc.views :as views]
            [me.lomin.spectree :refer [each+>>]]
            [com.rpl.specter :as specter]
            [me.lomin.spectree :as spectree]))

(deftest ^:focused transforms-to-reagent-props-test
  (is (= [:section#main
          [:input#toggle-all
           {:type    "checkbox"
            :checked true}]
          [:label
           {:for "toggle-all"}
           "Mark all as complete"]
          [:ul#todo-list
           ]]
         (specter/select-one
           (spectree/each [:html/#main])
           views/template))))