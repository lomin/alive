(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive]
            [me.lomin.alive.html :as html]
            [com.rpl.specter :as specter]
    #?(:clj
            [clojure.test :refer :all]))
  #?(:cljs
     (:require-macros
       [cljs.test :refer [deftest is testing]])))

(def template (alive/load-template-from-path "html-snippet.html"))

(deftest ^:unit select-snippet-test
  (is (= [:div {} [:p {} "I am a snippet!"]]
         (alive/select [::html/div] template))))

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
         (time (specter/transform [(alive/make-selector ::html/#Question1)]
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive/make-path [::html/#Question1])
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive/make-path [::html/.question])
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive/make-path [::html/article])
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive/make-path [::html/article ::html/.question])
                                  (alive/add-class "new-class")
                                  test-article)))))

(deftest ^:unit transform-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class0 new-class1 question"} " e.hmtl Q1 "]
          [:div {:class "answer new-class2 new-class3"} " e.hmtl A1 "]]
         (alive/transform [::html/.question]
                          (comp (alive/add-class "new-class0")
                                (alive/add-class "new-class1"))
                          [::html/.answer]
                          (comp (alive/add-class "new-class2")
                                (alive/add-class "new-class3"))
                          test-article)))
  (testing "transforms within nested structures"
    (is (= [:div {:class "question", :l0 {:l1 2}} "test"]
           (alive/transform
             [::html/.question html/ATTRS :l0 :l1]
             inc
             [:div {:class "question" :l0 {:l1 1}} "test"])))))

(deftest ^:unit remove-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "question"}]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (alive/transform [::html/.question]
                          alive/remove-content
                          test-article))))

(deftest ^:unit comlement-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"}]]
         (alive/transform [::html/div (alive/not-selected? ::html/.question)]
                          alive/remove-content
                          test-article)))
  (is (= test-article
         (alive/transform [::html/article (alive/not-selected? ::html/#Question1)]
                          alive/remove-content
                          test-article)))

  (is (= [:article {:id "Question1"}]
         (alive/transform [::html/article (alive/not-selected? ::html/#NotHere)]
                          alive/remove-content
                          test-article))))

(deftest ^:unit guard-test
  (is (= [:article {:id "Question1"}]
         (alive/transform [(constantly true) ::html/article (alive/not-selected? ::html/#NotHere)]
                          alive/remove-content
                          test-article)))
  (is (= test-article
         (alive/transform [(constantly false) ::html/article (alive/not-selected? ::html/#NotHere)]
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