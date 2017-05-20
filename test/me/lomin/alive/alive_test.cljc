(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive]
            [com.rpl.specter :as s]
    #?(:clj
            [clojure.test :refer :all]))
  #?(:cljs
     (:require-macros
       [cljs.test :refer [deftest is testing]])))

(def template (alive/load-template-from-path "html-snippet.html"))

(deftest ^:unit to-snippet-test
  (is (= [:div {} [:p {} "I am a snippet!"]]
         (alive/make-snippet template))))

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
         (time (s/transform [(alive/make-selector :#Question1)]
                            (alive/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive/make-path [:#Question1])
                            (alive/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive/make-path [:.question])
                            (alive/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:class "new-class", :id "Question1"}
          [:div {:class "question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive/make-path [:article])
                            (alive/add-class "new-class")
                            test-article))))

  (is (= [:article
          {:id "Question1"}
          [:div {:class "new-class question"} " e.hmtl Q1 "]
          [:div {:class "answer"} " e.hmtl A1 "]]
         (time (s/transform (alive/make-path [:article :.question])
                            (alive/add-class "new-class")
                            test-article)))))
