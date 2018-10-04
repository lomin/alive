(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive.core]
            [me.lomin.alive :as alive]
            [com.rpl.specter :as s]
            [clojure.test :refer [deftest is testing]]))

(def template (alive.core/load-template-from-path "html-snippet.html"))

(deftest ^:unit select-snippet-test
  (is (= [:me.lomin.alive/document
          [[:html {} [:head {}] [:body {} [:div {} [:p {} "I am a snippet!"]]]]]]
         template))
  (is (= [[:div {} [:p {} "I am a snippet!"]]]
         (alive/select [:div] template))))

(deftest ^:unit add-class-test
  (is (= [:div {:class "c0 c1 c2 c3"}]
         (alive.core/add-class "c3" [:div {:class "c0 c1 c2"}]))))

(def test-article
  [:article
   {:id "Question1"}
   [:div {:class "question"} " e.hmtl Q1 "]
   [:div {:class "answer"} " e.hmtl A1 "]])

(deftest ^:unit replace-content-test
  (is (= [:article {:id "Question1"} :p {} "Replaced!"]
         (alive.core/replace-content [:p {} "Replaced!"]
                                     test-article))))

(deftest ^:unit contains-class?-test
  (is (not (alive.core/contains-class? "question" [:div {} " e.hmtl Q1 "])))
  (is (alive.core/contains-class? "question" [:div {:class "answer question"} " e.hmtl Q1 "])))

(deftest ^:unit transformation-test
  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform [(alive.core/each :#/Question1)]
                            (alive.core/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive.core/each [:#/Question1])
                            (alive.core/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive.core/each [:./question])
                            (alive.core/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive.core/each [:article])
                            (alive.core/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive.core/each [:article :./question])
                            (alive.core/add-class "new-class")
                            test-article)))))

(deftest ^:unit transform-test
  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class0 new-class1 question"} " e.hmtl Q1 "]
          [:div {:class "answer new-class2 new-class3"} " e.hmtl A1 "]]
         (alive.core/transform test-article
                               [:./question] (comp (alive.core/add-class "new-class0")
                                                  (alive.core/add-class "new-class1"))
                               [:./answer] (comp (alive.core/add-class "new-class2")
                                                (alive.core/add-class "new-class3")))))
  (testing "transforms within nested structures"
    (is (= [:div {:class "question", :l0 {:l1 2}} "test"]
           (alive.core/transform
             [:div {:class "question" :l0 {:l1 1}} "test"]
             [:./question alive.core/ATTRS (alive.core/map-key :l0) (alive.core/map-key :l1)] inc)))))