(ns me.lomin.alive.selectors-test
  (:require [me.lomin.alive :as alive]
            [me.lomin.alive.reload]
            [clojure.test :refer [deftest is testing]]))

(def template (alive/load-template-from-path "html-for-selectors-test.html"))

(deftest ^:unit selectors-test
  (testing "css-selector: 'div a'"
    (is (= [[:a {:class "link link1", :href "/?i=1"} "I am link #1!"]
            [:a {:id "link2" :class "link link2", :href "/?i=2"} "I am link #2!"]]
           (alive/select [:div :a] template))))

  (testing "css-selector: >div"
    (is (= [[:div {} "before"]
            [:div
             {}
             [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #2!"]
             [:p {:class "p-class"} "I am a snippet!"]]
            [:div {} "after"]]
           (alive/select :>/div
                         [:li {}
                          [:div {} "before"]
                          [:div {}
                           [:a {:id "link2", :class "link link2", :href "/?i=2"} "I am link #2!"]
                           [:p {:class "p-class"} "I am a snippet!"]] [:div {} "after"]]))))

  (testing "css-selector: 'div>a'"
    (is (= [[:a {:class "link link1", :href "/?i=1"} "I am link #1!"]]
           (alive/select [:div :>/a] template))))

  (testing "css-selector: 'div>.link'"
    (is (= [[:a {:class "link link1", :href "/?i=1"} "I am link #1!"]]
           (alive/select [:div :>./link] template))))

  (testing "css-selector: 'a.link1'"
    (is (= [[:a {:class "link link1", :href "/?i=1"} "I am link #1!"]]
           (alive/select [:a :&./link1] template)))
    (is (= [[:div {:class "test"} [:p {} "I am a snippet!"]]]
           (alive/select [:&./test]
                         [:div {:class "test"} [:p {} "I am a snippet!"]]))))

  (testing "css-selector: 'a.link1'"
    (is (= [[:a {:id "link2" :class "link link2", :href "/?i=2"} "I am link #2!"]]
           (alive/select [:a :&#/link2] template))))

  (testing "css-selector: '#link2'"
    (is (= [[:a {:id "link2" :class "link link2", :href "/?i=2"} "I am link #2!"]]
           (alive/select [:#/link2] template)))))