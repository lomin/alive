(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive.core]
            [me.lomin.alive :as alive]
            [com.rpl.specter :as specter]
            [clojure.test :refer [deftest is testing]]
            [me.lomin.alive.core :as alive-core]))

(def template (alive/load-template-from-path "html-snippet.html"))

(deftest ^:unit select-snippet-test
  (is (= [:me.lomin.alive/document
          [[:html {} [:head {}] [:body {} [:div {} [:p {} "I am a snippet!"]]]]]]
         template))
  (is (= [[:div {} [:p {} "I am a snippet!"]]]
         (alive/select [:div] template))))

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
         (alive/content [:p {} "Replaced!"]
                        test-article))))

(deftest ^:unit contains-class?-test
  (is (not (alive/class-contains "question" [:div {} " e.hmtl Q1 "])))
  (is (alive/class-contains "question" [:div {:class "answer question"} " e.hmtl Q1 "])))

(deftest ^:unit transformation-test
  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform [(alive.core/each :#/Question1)]
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive.core/each [:#/Question1])
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive.core/tree [:./question])
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive.core/each [:article])
                                  (alive/add-class "new-class")
                                  test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (specter/transform (alive.core/tree [:article :./question])
                                  (alive/add-class "new-class")
                                  test-article)))))

(deftest ^:unit transform-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class0 new-class1 question"} " e.hmtl Q1 "]
          [:div {:class "answer new-class2 new-class3"} " e.hmtl A1 "]]
         (alive/transform [:./question] (comp (alive/add-class "new-class0")
                                              (alive/add-class "new-class1"))
                          [:./answer] (comp (alive/add-class "new-class2")
                                            (alive/add-class "new-class3"))
                          test-article)))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "answer"} " e.hmtl A1 "]]
         (alive/transform [] (alive/none :./question)
                          test-article)))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "answer"} " e.hmtl A1 "]]
         (alive/transform [:./question]
                          (alive/none (constantly true))
                          test-article)))

  (testing "transforms within nested structures"
    (is (= [:div {:class "question", :l0 {:l1 2}} "test"]
           (alive/transform [:./question alive/ATTRS (alive/map-key :l0) (alive/map-key :l1)]
                            inc
                            [:div {:class "question" :l0 {:l1 1}} "test"])))))

(deftest ^:unit transform-macro-test
  (testing "transform macro test"
    (is (= {:a 6 :b 4 :c {:d 6 :e 4 :f [0 2 2]}}
           ((alive/transform2
              [:key/a] inc
              [(alive/map-key :c)] (alive/transform
                                     [(alive/map-key :d)] inc
                                     [(alive/map-key :e)] dec
                                     [(alive/map-key :f)] (alive/transform
                                                            1 inc))
              [(alive/map-key :b)] dec)
             {:a 5 :b 5 :c {:d 5 :e 5 :f [0 1 2]}})))))