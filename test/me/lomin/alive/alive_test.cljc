(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive]
    #?(:clj [clojure.test :refer :all])
    #?(:clj [com.rpl.specter.macros :as sp])
    #?(:clj [me.lomin.alive.macros :refer [import-tag]]))
  #?(:cljs
     (:require-macros
       [cljs.test :refer [deftest is testing]]
       [com.rpl.specter.macros :as sp]
       [me.lomin.alive.macros :refer [import-tag]])))

(deftest add-class-test
  (is (= [:div {:class "c0 c1 c2 c3"}]
         (alive/add-class "c3" [:div {:class "c0 c1 c2"}]))))

(import-tag div)

(deftest add-class-via-specter
  (is (= [:div {:class "c0 c1 c2 c3"}]
         (sp/transform [div]
                       (alive/add-class "c3")
                       [:div {:class "c0 c1 c2"}]))))