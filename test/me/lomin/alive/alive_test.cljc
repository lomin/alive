(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive]
            [me.lomin.alive.html :as html]
            [me.lomin.spectree :refer [each each+>> +>>]]
            [com.rpl.specter :as specter]
            #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is testing]])
            [me.lomin.spectree.tree-search :as tree-search]))

(def template (alive/load-template-from-path "html-template.html"))
(def index-template (alive/load-template-from-path "test-index.html"))

(extend-type #?(:clj java.lang.Object)
  me.lomin.spectree/Each
  (each [self]
    self))

(def test-article
  [:article
   {:id "Question1"}
   [:div {:class "question"} " e.hmtl Q1 "]
   [:div {:class "answer"} " e.hmtl A1 "]])

(comment ^:unit transforms-to-reagent-props-test
         (is (= [:input
                 {:auto-complete "on",
                  :auto-focus    true,
                  :class         "new-todo",
                  :name          "newTodo",
                  :placeholder   "What needs to be done?"}]
                (each+>> specter/transform
                         [1] (comp (alive/translate :autofocus :auto-focus boolean)
                                   (alive/translate :autocomplete :auto-complete))
                         (specter/select-one (each [:html/input]) template)))))

(deftest ^:unit add-class-test
  (is (= [:div {:class "c0 c1 c2 c3"}]
         (alive/add-class "c3" [:div {:class "c0 c1 c2"}]))))

(def test-article
  [:article
   {:id "Question1"}
   [:div {:class "question"} " e.hmtl Q1 "]
   [:div {:class "answer"} " e.hmtl A1 "]])

(deftest ^:unit replace-content-test
  (is (= [:article {:id "Question1"} :p {} "Replaced!"]
         (alive/replace-content [:p {} "Replaced!"]
                                test-article))))

(deftest ^:unit contains-class?-test
  (is (not (alive/contains-class? "question" [:div {} " e.hmtl Q1 "])))
  (is (alive/contains-class? "question" [:div {:class "answer question"} " e.hmtl Q1 "])))

(deftest ^:unit transformation-test
  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (each+>> specter/transform
                        [:html/#Question1] (alive/add-class "new-class")
                        test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (each+>> specter/transform
                        [:html/#Question1] (alive/add-class "new-class")
                        test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (each+>> specter/transform
                        [:html/.question] (alive/add-class "new-class")
                        test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (each+>> specter/transform
                        [:html/article] (alive/add-class "new-class")
                        test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (each+>> specter/transform
                        [:html/article :html/.question] (alive/add-class "new-class")
                        test-article)))))

(deftest ^:unit transform-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class0 new-class1 question"} " e.hmtl Q1 "]
          [:div {:class "answer new-class2 new-class3"} " e.hmtl A1 "]]
         (each+>> specter/transform
                  [:html/.question]
                  (comp (alive/add-class "new-class0")
                        (alive/add-class "new-class1"))
                  [:html/.answer]
                  (comp (alive/add-class "new-class2")
                        (alive/add-class "new-class3"))
                  test-article)))
  (testing "transforms within nested structures"
    (is (= [:div {:class "question", :l0 {:l1 2}} "test"]
           (each+>> specter/transform
                    [:html/.question html/ATTRS :l0 :l1] inc
                    [:div {:class "question" :l0 {:l1 1}} "test"])))))

(deftest ^:unit remove-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "question"}]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (each+>> specter/transform
                  [:html/.question] alive/remove-content
                  test-article))))

(deftest ^:unit complement-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "question"}]
          [:div {:class "answer"}]]
         (each+>> specter/transform
                  [:html/div (alive/not-selected? :html/a)] alive/remove-content
                  test-article)))
  (is (= [:article
          {:id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"}]]
         (each+>> specter/transform
                  [:html/div (alive/not-selected? :html/.question)] alive/remove-content
                  test-article)))

  (is (= test-article
         (each+>> specter/transform
                  [:html/article (alive/not-selected? :html/#Question1)] alive/remove-content
                  test-article)))

  (is (= [:article {:id "Question1"}]
         (each+>> specter/transform
                  [:html/article (alive/not-selected? :html/#NotHere)] alive/remove-content
                  test-article))))

(deftest ^:unit guard-test
  (is (= [:article {:id "Question1"}]
         (each+>> specter/transform
                  [(constantly true) :html/article (alive/not-selected? :html/#NotHere)]
                  alive/remove-content
                  test-article)))
  (is (= test-article
         (each+>> specter/transform
                  [(constantly false) :html/article (alive/not-selected? :html/#NotHere)]
                  alive/remove-content
                  test-article))))



(deftest ^:unit state-test
  (is (= [1
          3]
         (alive/filter-sub-states #{:a :c}
                                  [[#{:a} 1]
                                   [#{:b} 2]
                                   [#{:c} 3]
                                   [#{:d} 4]])))
  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class0 question"} " e.hmtl Q1 "]
          [:div {:class "answer new-class2"} " e.hmtl A1 "]]
         (alive/choose-state
           #{:a :b}
           #{:a} (alive/transform [::html/.question] (alive/add-class "new-class0"))
           #{:b} (alive/transform [::html/.answer]
                                  (alive/add-class "new-class2"))
           test-article))))

(defn hans #alive/html {:ret inc}
  ([x] x))

(deftest lolllllllllltesr
  (is (= [:section#main
          [:input#toggle-all
           {:type    "checkbox"
            :checked true}]
          [:label
           {:for "toggle-all"}
           "Mark all as complete"]
          [:ul#todo-list
           ]]
         (specter/select
           #alive/html [#alive/html [:html/li] (specter/filterer #(do (prn "--!->" %) false))]
           index-template))))

(comment ^:focused select-test

         (is (= [test-article]
                (specter/select
                  (each [:html/article])
                  test-article)))
         (is (= [[:div {:class "question"} " e.hmtl Q1 "]
                 [:div {:class "answer"} " e.hmtl A1 "]]
                (specter/select
                  (each [:html/article :html/div])
                  test-article)))

         (is (= ["hans"]
                (specter/select (each [:html/article :html/header :html/word 1])
                                [:article
                                 [:header [:word "hans"]]
                                 [:line
                                  [:UID [:number "501"]]
                                  [:PID [:number "45427"]]
                                  [:CMD [:word "bash"]]]
                                 [:line
                                  [:UID [:number "501"]]
                                  [:PID [:number "45427"]]
                                  [:CMD [:word "bash"]]]])))
         (is (=
               (type (each [:html/li (specter/filterer any?)]))
               (type [(each :html/li) (specter/filterer any?)])))

         (is (= [:section#main
                 [:input#toggle-all
                  {:type    "checkbox"
                   :checked true}]
                 [:label
                  {:for "toggle-all"}
                  "Mark all as complete"]
                 [:ul#todo-list
                  ]]
                (specter/select
                  #alive/html [:html/li (specter/filterer #(do (prn "--!->" %) false))]
                  index-template))))