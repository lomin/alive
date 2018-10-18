(ns me.lomin.alive.enlive-compatibility-test
  (:require [clojure.test :refer :all]
            [me.lomin.alive :as alive]
            [net.cgrand.enlive-html :as enlive]
            [clojure.walk :as walk]
            [clojure.string :as str]))

(def template-file "html-template.html")
(def snippet-file "html-snippet.html")

(def alive-template (alive/load-template-from-path template-file))
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

(enlive/defsnippet enlive-clone-for-snippet template-file [:body] []
  [:div :p]
  (enlive/clone-for [i (range 3)]
                    [:p] (enlive/content (str "I am #" i))))

(alive/defsnippet alive-clone-for-snippet [] alive-template [:body]
  [:div] (alive/clone-for [i (range 3)]
                          :p (alive/content (str "I am #" i))))

(alive/defsnippet alive-clone-for-snippet2 [] alive-template [:body]
  [:div] (alive/clone-for [i (range 3)]
                          :p (alive/content (str "I am #" i))))

(deftest ^:unit clone-for-test
  (testing "there are slight differences for clone-for in selector usage, but output is the same"
    (is (= (enlive-render (enlive-clone-for-snippet))
           (alive/render (alive-clone-for-snippet))))

    (is (= "<body class=\"body-class\"><div><p class=\"p-class\">I am #0</p><p class=\"p-class\">I am #1</p><p class=\"p-class\">I am #2</p></div></body>"
           (time (enlive-render (enlive-clone-for-snippet)))))

    (is (= "<body class=\"body-class\"><div><p class=\"p-class\">I am #0</p><p class=\"p-class\">I am #1</p><p class=\"p-class\">I am #2</p></div></body>"
           (time (alive/render (alive-clone-for-snippet)))))

    (is (= "<body class=\"body-class\"><div><p class=\"p-class\">I am #0</p><p class=\"p-class\">I am #1</p><p class=\"p-class\">I am #2</p></div></body>"
           (time (alive/render (alive-clone-for-snippet2)))))))