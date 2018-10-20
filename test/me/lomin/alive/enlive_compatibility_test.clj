(ns me.lomin.alive.enlive-compatibility-test
  (:require [clojure.test :refer :all]
            [me.lomin.alive :as alive]
            [net.cgrand.enlive-html :as enlive]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [me.lomin.alive.core :as alive-core]))

(def template-file "html-template.html")
(def clone-template-file "html-for-clone-test.html")
(def snippet-file "html-snippet.html")

(def alive-template (alive/load-template-from-path template-file))
(def alive-clone-template (alive/load-template-from-path clone-template-file))
(def alive-snippet (alive/load-snippet-from-path snippet-file))
(def enlive-template (enlive/html-resource template-file))

(defn remove-string-blank [template]
  (walk/postwalk #(if (and (map? %) (vector? (:content %)))
                    (update % :content
                            (partial remove
                                     (fn [x]
                                       (and (string? x) (str/blank? x))))) %)
                 template))

(defn enlive-render [dom]
  (str/trim (apply str
                   (map #(str/replace % "\n" " ")
                        (enlive/emit* (remove-string-blank dom))))))

(deftest ^:unit template-test
  '({:data ["html" nil nil], :type :dtd}
    {:attrs   {:lang "de"}
     :content ("\n"
               {:attrs   nil
                :content ("\n    "
                          {:attrs {:charset "utf-8"}, :content nil, :tag :meta}
                          "\n    "
                          {:attrs   {:href "", :rel "stylesheet"}
                           :content nil
                           :tag     :link}
                          "\n    "
                          {:attrs   {:src "src", :type "text/javascript"}
                           :content nil
                           :tag     :script}
                          "\n    "
                          {:attrs   nil
                           :content ("This is a title placeholder")
                           :tag     :title}
                          "\n")
                :tag     :head}
               "\n"
               {:attrs   {:class "body-class"}
                :content ("\n"
                          {:attrs   nil
                           :content ("\n    "
                                     {:attrs   {:class "p-class"}
                                      :content ("I am a snippet!")
                                      :tag     :p}
                                     "\n")
                           :tag     :div}
                          "\n")
                :tag     :body}
               "\n")
     :tag     :html})
  (is (= [:me.lomin.alive/document
          ["<!DOCTYPE html>"
           [:html
            {:lang "de"}
            [:head
             {}
             [:meta {:charset "utf-8"}]
             [:link {:href "", :rel "stylesheet"}]
             [:script {:src "src", :type "text/javascript"}]
             [:script {:src "", :type "text/javascript"}]
             [:title {} "This is a title placeholder"]]
            [:body
             {:class "body-class"}
             [:div {} [:p {:class "p-class"} "I am a snippet!"]]]]]]
         alive-template))
  (is (= [::alive/element
          [:div {} [:p {} "I am a snippet!"]]]
         alive-snippet)))

(deftest ^:unit render-fn-test
  (is (string? (enlive-render enlive-template)))
  (is (string? (alive/render alive-template)))
  (is (= "<html lang=\"de\"><head><meta charset=\"utf-8\"><link href=\"\" rel=\"stylesheet\"><script src=\"src\" type=\"text/javascript\"></script><script src=\"\" type=\"text/javascript\"></script><title>This is a title placeholder</title></head><body class=\"body-class\"><div><p class=\"p-class\">I am a snippet!</p></div></body></html>"
         (alive/render alive-template)))
  (is (= "<div><p>I am a snippet!</p></div>"
         (alive/render alive-snippet)))
  (is (not= (alive/render alive-template)
            (enlive-render enlive-template))))

(alive/defsnippet alive-add-classes-snippet [x] alive-template [:body :./body-class]
  [:div] (alive/add-class x)
  [:p :./p-class] (alive/add-class "willi"))

(deftest ^:unit alive-snippet-test
  (is (= "<body class=\"body-class\"><div class=\"!!!\"><p class=\"p-class willi\">I am a snippet!</p></div></body>"
         (alive/render (alive-add-classes-snippet "!!!"))
         (alive/render (alive-add-classes-snippet "!!!"))
         (alive/render (alive-add-classes-snippet "!!!"))
         (alive/render (alive-add-classes-snippet "!!!")))))

(enlive/defsnippet enlive-snippet-0 template-file [:body.body-class] [x]
  [:div] (enlive/append "test-append")
  [:p.p-class] (enlive/content (str "test-content" x)))

(alive/defsnippet alive-snippet-0 [x] alive-template [:body :./body-class]
  [:div] (alive/append "test-append")
  [:p :./p-class] (alive/content (str "test-content" x)))

(deftest ^:unit snippet-0-test
  (is (= "<body class=\"body-class\"><div><p class=\"p-class\">test-content!!!</p>test-append</div></body>"
         (enlive-render (enlive-snippet-0 "!!!"))
         (alive/render (alive-snippet-0 "!!!")))))

(enlive/defsnippet enlive-snippet-1 template-file [:body.body-class] []
  [:body] (enlive/set-attr :key "value")
  [:div] (enlive/set-attr "key" "value"))

(alive/defsnippet alive-snippet-1 [] alive-template [:body :./body-class]
  [:body] (alive/set-attr :key "value")
  [:div] (alive/set-attr "key" "value"))

(deftest ^:unit enlive-snippet-1-test
  (is (= "<body class=\"body-class\" key=\"value\"><div key=\"value\"><p class=\"p-class\">I am a snippet!</p></div></body>"
         (enlive-render (enlive-snippet-1))
         (alive/render (alive-snippet-1)))))

(declare enlive-clone-for-snippet)
(enlive/defsnippet enlive-clone-for-snippet clone-template-file [:body] []
  [:div :li :div]
  (enlive/clone-for [i (range 3)]
                    [:p] (enlive/content (str "I am #" i))
                    [:a] (enlive/content (str "I am link #" i))))

(alive/defsnippet alive-clone-for-snippet [] alive-clone-template [:body]
  [:div :li] (alive/clone-for [i (range 3)]
                              :div
                              :p (alive/content (str "I am #" i))
                              :a (alive/content (str "I am link #" i))))

(alive/defsnippet alive-clone-for-snippet2 [] alive-clone-template [:body]
  [:div :li] (alive/clone-for [i (range 3)]
                              :>/div
                              :>/p (alive/content (str "I am #" i))
                              :>/a (alive/content (str "I am link #" i))))

(deftest ^:unit clone-test
  (is (= [:li
          {}
          [:div {} "before"]
          [:div {} "before"]
          [:div {} "before"]
          [:div {} [:p {:class "p-class"} "Hi #0"]]
          [:div {} [:p {:class "p-class"} "Hi #1"]]
          [:div {} [:p {:class "p-class"} "Hi #2"]]
          [:div {} "after"]
          [:div {} "after"]
          [:div {} "after"]]
         (alive-core/clone '(0 1 2)
                           (fn [i] (alive/transform :p (alive/content (str "Hi #" i))))
                           [[2 [:div {} "before"]]
                            [3 [:div {} [:p {:class "p-class"} "I am a snippet!"]]]
                            [4 [:div {} "after"]]]
                           [:li {} [:div {} "before"]
                            [:div {}
                             [:p {:class "p-class"} "I am a snippet!"]]
                            [:div {} "after"]]))))

(deftest ^:unit clone-for-test

  (testing "enlive/clone-for is the reference"
    (is (= "<body class=\"body-class\"><div><a class=\"link link1\" href=\"/?i=1\">I am link #1!</a><ul><li><div>before</div><div>before</div><div>before</div><div><a id=\"link2\" class=\"link link2\" href=\"/?i=2\">I am link #0</a><p class=\"p-class\">I am #0</p></div><div><a id=\"link2\" class=\"link link2\" href=\"/?i=2\">I am link #1</a><p class=\"p-class\">I am #1</p></div><div><a id=\"link2\" class=\"link link2\" href=\"/?i=2\">I am link #2</a><p class=\"p-class\">I am #2</p></div><div>after</div><div>after</div><div>after</div></li></ul></div></body>"
           (time (enlive-render (enlive-clone-for-snippet))))))

  (testing "alive/clone-for"
    (is (= [:body
            {:class "body-class"}
            [:div
             {}
             [:a {:class "link link1", :href "/?i=1"} "I am link #1!"]
             [:ul
              {}
              [:li
               {}
               [:div {} "before"]
               [:div {} "before"]
               [:div {} "before"]
               [:div
                {}
                [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #0"]
                [:p {:class "p-class"} "I am #0"]]
               [:div
                {}
                [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #1"]
                [:p {:class "p-class"} "I am #1"]]
               [:div
                {}
                [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #2"]
                [:p {:class "p-class"} "I am #2"]]
               [:div {} "after"]
               [:div {} "after"]
               [:div {} "after"]]]]]
           (alive-clone-for-snippet)
           (alive-clone-for-snippet2)))))

(deftest ^:unit clone-for-as-function-test
  (is (= [:li
          {}
          [:div {} "before"]
          [:div {} "before"]
          [:div {} "before"]
          [:div
           {}
           [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #0"]
           [:p {:class "p-class"} "I am #0"]]
          [:div
           {}
           [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #1"]
           [:p {:class "p-class"} "I am #1"]]
          [:div
           {}
           [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #2"]
           [:p {:class "p-class"} "I am #2"]]
          [:div {} "after"]
          [:div {} "after"]
          [:div {} "after"]]
         (alive/clone-for [i (range 3)]
                          :div
                          :p (alive/content (str "I am #" i))
                          :a (alive/content (str "I am link #" i))
                          [:li
                           {}
                           [:div {} "before"]
                           [:div
                            {}
                            [:a {:class "link link2", :href "/?i=2", :id "link2"} "I am link #2!"]
                            [:p {:class "p-class"} "I am a snippet!"]]
                           [:div {} "after"]]))))