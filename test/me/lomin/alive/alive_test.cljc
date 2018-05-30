(ns me.lomin.alive.alive-test
  (:require [me.lomin.alive.core :as alive]
            [me.lomin.alive.html :as html]
            [hickory.core :as hickory]
            [com.rpl.specter :as specter]
    #?(:clj
            [clojure.test :refer :all]))
  #?(:cljs
     (:require-macros
       [cljs.test :refer [deftest is testing]])))

(def template (alive/load-template-from-path "html-template.html"))

(deftest ^:unit transforms-to-reagent-props-test
  (is (= [:input
          {:auto-complete "on",
           :auto-focus    true,
           :class         "new-todo",
           :name          "newTodo",
           :placeholder   "What needs to be done?"}]
         (alive/transform [html/ATTRS]
                          (comp (alive/translate :autofocus :auto-focus boolean)
                                (alive/translate :autocomplete :auto-complete))
                          (alive/select [::html/input] template)))))

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
          [:div {:class "question"}]
          [:div {:class "answer"}]]
         (alive/transform [::html/div (alive/not-selected? ::html/a)]
                          alive/remove-content
                          test-article)))
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

(deftest ^:focused select-test
  (is (= "hans" (alive/select [::html/article ::html/header ::html/word 1]
                             [:article
                              [:header [:word "hans"]]
                              [:line
                               [:UID [:number "501"]]
                               [:PID [:number "45427"]]
                               [:CMD [:word "bash"]]]
                              [:line
                               [:UID [:number "501"]]
                               [:PID [:number "45427"]]
                               [:CMD [:word "bash"]]]]))))

(def dom "<html lang=\"en\"><head>\n    <meta charset=\"utf-8\">\n    <title>Reframe Todomvc</title>\n    <link rel=\"stylesheet\" href=\"base.css\">\n    <link rel=\"stylesheet\" href=\"index.css\">\n<style type=\"text/css\">\n:root #header + #content > #left > #rlblock_left\n{ display: none !important; }</style><style id=\"style-1-cropbar-clipper\">/* Copyright 2014 Evernote Corporation. All rights reserved. */\n.en-markup-crop-options {\n    top: 18px !important;\n    left: 50% !important;\n    margin-left: -100px !important;\n    width: 200px !important;\n    border: 2px rgba(255,255,255,.38) solid !important;\n    border-radius: 4px !important;\n}\n\n.en-markup-crop-options div div:first-of-type {\n    margin-left: 0px !important;\n}\n</style></head>\n<body>\n<div id=\"app\"><div data-reactroot=\"\" id=\"app\"><section class=\"todoapp\"><header id=\"header\" class=\"header\"><h1>todos</h1><input placeholder=\"What needs to be done?\" name=\"newTodo\" class=\"new-todo\" value=\"\"></header><section class=\"main\"><input type=\"checkbox\" id=\"toggle-all\" class=\"toggle-all\" name=\"toggle\" value=\"on\"><label for=\"toggle-all\">Mark all as complete</label><ul class=\"todo-list\"><li class=\"\"><div class=\"view\"><input type=\"checkbox\" class=\"toggle\" value=\"on\"><label>todo</label><button class=\"destroy\"></button></div><input class=\"edit\" name=\"title\" id=\"todo-1\"></li></ul></section><footer class=\"footer\"><span class=\"todo-count\"><strong>1</strong><!-- react-text: 19 --> item left<!-- /react-text --></span><ul class=\"filters\"><li><a data-query=\"showing-all\" class=\"selected\" href=\"#/\">All</a></li><li><a data-query=\"showing-active\" class=\"\" href=\"#/active\">Active</a></li><li><a data-query=\"showing-done\" class=\"\" href=\"#/done\">Completed</a></li></ul><button class=\"clear-completed\">Clear completed (1)</button></footer></section><section class=\"todoapp\"><header class=\"header\"><h1>todos</h1><input class=\"new-todo\" placeholder=\"What needs to be done?\" name=\"newTodo\"></header><section class=\"main\"><input type=\"checkbox\" id=\"toggle-all\" class=\"toggle-all\" name=\"toggle\" value=\"on\"><ul class=\"todo-list\"><li class=\"completed \"><div class=\"view\"><input type=\"checkbox\" class=\"toggle\" value=\"on\"><label>done</label><button class=\"destroy\"></button></div><input class=\"edit\" name=\"title\" id=\"todo-0\"></li></ul></section><footer class=\"footer\"><span class=\"todo-count\"><strong>1</strong><!-- react-text: 44 --> item left<!-- /react-text --></span><ul class=\"filters\"><li><a class=\"\" href=\"#/\">All</a></li><li><a class=\"\" href=\"#/active\">Active</a></li><li><a class=\"selected\" href=\"#/done\">Completed</a></li></ul></footer></section><section id=\"no-todos\" class=\"todoapp\"><header class=\"header\"><h1>todos</h1><input class=\"new-todo\" placeholder=\"What needs to be done?\" name=\"newTodo\"></header><section class=\"main\"><input type=\"checkbox\" id=\"toggle-all\" class=\"toggle-all\" name=\"toggle\" value=\"on\"><ul class=\"todo-list\"></ul></section><footer class=\"footer\"><span class=\"todo-count\"><strong>0</strong><!-- react-text: 62 --> items left<!-- /react-text --></span><ul class=\"filters\"><li><a class=\"\" href=\"#/\">All</a></li><li><a class=\"selected\" href=\"#/active\">Active</a></li><li><a class=\"\" href=\"#/done\">Completed</a></li></ul><button class=\"clear-completed\">Clear completed (1)</button></footer></section></div></div>\n<script src=\"js/client.js\"></script><script src=\"js/goog/base.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/deps.js\"></script><script src=\"js/cljs_deps.js\"></script><script>if (typeof goog == \"undefined\") console.warn(\"ClojureScript could not load :main, did you forget to specify :asset-path?\");</script><script>if (typeof goog != \"undefined\") { goog.require(\"figwheel.connect.build_client\"); }</script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/string/string.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/object/object.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/math/integer.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/string/stringbuffer.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/debug/error.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/nodetype.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/asserts/asserts.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/array/array.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/reflect/reflect.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/math/long.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../clojure/set.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../clojure/string.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/pprint.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../clojure/walk.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../com/rpl/specter/protocols.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../com/rpl/specter/impl.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../com/rpl/specter/navs.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../com/rpl/specter.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/interop.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljsjs/react/development/react.inc.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/debug.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/impl/util.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/impl/batching.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/ratom.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/impl/component.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/impl/template.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljsjs/react-dom/development/react-dom.inc.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/dom.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../reagent/core.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/debug/entrypointregistry.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/tagname.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/functions/functions.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/labs/useragent/util.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/labs/useragent/browser.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/labs/useragent/engine.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/async/nexttick.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/interop.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/loggers.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/interceptor.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/trace.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/registrar.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/utils.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/db.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/events.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/router.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/fx.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/cofx.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../clojure/data.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/std_interceptors.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/subs.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../re_frame/core.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../me/lomin/alive/re_alive.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../clojure/zip.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../hickory/utils.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../hickory/core.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../me/lomin/alive/html.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../me/lomin/alive/core.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../todomvc/views.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/spec/gen/alpha.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/spec/alpha.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/reader.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../todomvc/db.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../todomvc/events.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/history/eventtype.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../todomvc/subs.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/promise/thenable.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/async/freelist.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/async/workqueue.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/async/run.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/promise/resolver.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/promise/promise.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/disposable/idisposable.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/disposable/disposable.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/labs/useragent/platform.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/useragent/useragent.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/browserfeature.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/eventid.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/event.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/eventtype.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/browserevent.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/listenable.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/listener.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/listenermap.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/events.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/eventtarget.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/timer/timer.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/browserfeature.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/tags.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/string/typedstring.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/string/const.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/html/safestyle.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/html/safestylesheet.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/fs/url.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/i18n/bidi.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/html/safeurl.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/html/trustedresourceurl.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/html/safehtml.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/safe.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/html/safescript.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/html/uncheckedconversions.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/math/math.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/math/coordinate.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/math/size.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/dom.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/inputtype.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/events/eventhandler.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/history/event.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/labs/useragent/device.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/memoize/memoize.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/history/history.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../secretary/core.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../todomvc/core.js\"></script><input type=\"text\" name=\"history_state0\" id=\"history_state0\" style=\"display:none\"><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/useragent/product.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/structs/structs.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/iter/iter.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/structs/map.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/uri/utils.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/uri/uri.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core/async/impl/protocols.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core/async/impl/buffers.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core/async/impl/dispatch.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core/async/impl/channels.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core/async/impl/ioc_helpers.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core/async/impl/timers.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/core/async.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/mochikit/async/deferred.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/net/jsloader.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../figwheel/client/utils.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../figwheel/client/file_reloading.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../cljs/repl.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/dom/dataset.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../figwheel/client/socket.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../figwheel/client/heads_up.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../figwheel/client.js\"></script><script type=\"text/javascript\" src=\"http://0.0.0.0:3449/js/goog/../figwheel/connect/build_client.js\"></script><script>goog.require(\"todomvc.core\");</script>\n<script>\n    window.onload = function () {\n        todomvc.core.main();\n    }\n</script>\n\n\n<div id=\"figwheel-heads-up-container\" style=\"transition: all 0.2s ease-in-out; font-size: 13px; border-top: 1px solid rgb(245, 245, 245); box-shadow: rgb(170, 170, 170) 0px 0px 1px; line-height: 18px; color: rgb(51, 51, 51); font-family: monospace; padding: 10px 0px; position: fixed; bottom: 0px; left: 0px; height: 68px; opacity: 1; box-sizing: border-box; z-index: 10000; text-align: left; border-radius: 35px; width: 68px; background-color: rgb(211, 234, 172); min-height: 68px;\"><!--?xml version='1.0' encoding='utf-8'?-->\n\n<svg width=\"49px\" height=\"49px\" style=\"position:absolute; top:9px; left: 10px;\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\" viewBox=\"0 0 428 428\" enable-background=\"new 0 0 428 428\" xml:space=\"preserve\">\n<circle fill=\"#fff\" cx=\"213\" cy=\"214\" r=\"213\"></circle>\n<g>\n<path fill=\"#96CA4B\" d=\"M122,266.6c-12.7,0-22.3-3.7-28.9-11.1c-6.6-7.4-9.9-18-9.9-31.8c0-14.1,3.4-24.9,10.3-32.5\n  s16.8-11.4,29.9-11.4c8.8,0,16.8,1.6,23.8,4.9l-5.4,14.3c-7.5-2.9-13.7-4.4-18.6-4.4c-14.5,0-21.7,9.6-21.7,28.8\n  c0,9.4,1.8,16.4,5.4,21.2c3.6,4.7,8.9,7.1,15.9,7.1c7.9,0,15.4-2,22.5-5.9v15.5c-3.2,1.9-6.6,3.2-10.2,4\n  C131.5,266.2,127.1,266.6,122,266.6z\"></path>\n<path fill=\"#96CA4B\" d=\"M194.4,265.1h-17.8V147.3h17.8V265.1z\"></path>\n<path fill=\"#5F7FBF\" d=\"M222.9,302.3c-5.3,0-9.8-0.6-13.3-1.9v-14.1c3.4,0.9,6.9,1.4,10.5,1.4c7.6,0,11.4-4.3,11.4-12.9v-93.5h17.8\n  v94.7c0,8.6-2.3,15.2-6.8,19.6C237.9,300.1,231.4,302.3,222.9,302.3z M230.4,159.2c0-3.2,0.9-5.6,2.6-7.3c1.7-1.7,4.2-2.6,7.5-2.6\n  c3.1,0,5.6,0.9,7.3,2.6c1.7,1.7,2.6,4.2,2.6,7.3c0,3-0.9,5.4-2.6,7.2c-1.7,1.7-4.2,2.6-7.3,2.6c-3.2,0-5.7-0.9-7.5-2.6\n  C231.2,164.6,230.4,162.2,230.4,159.2z\"></path>\n<path fill=\"#5F7FBF\" d=\"M342.5,241.3c0,8.2-3,14.4-8.9,18.8c-6,4.4-14.5,6.5-25.6,6.5c-11.2,0-20.1-1.7-26.9-5.1v-15.4\n  c9.8,4.5,19,6.8,27.5,6.8c10.9,0,16.4-3.3,16.4-9.9c0-2.1-0.6-3.9-1.8-5.3c-1.2-1.4-3.2-2.9-6-4.4c-2.8-1.5-6.6-3.2-11.6-5.1\n  c-9.6-3.7-16.2-7.5-19.6-11.2c-3.4-3.7-5.1-8.6-5.1-14.5c0-7.2,2.9-12.7,8.7-16.7c5.8-4,13.6-5.9,23.6-5.9c9.8,0,19.1,2,27.9,6\n  l-5.8,13.4c-9-3.7-16.6-5.6-22.8-5.6c-9.4,0-14.1,2.7-14.1,8c0,2.6,1.2,4.8,3.7,6.7c2.4,1.8,7.8,4.3,16,7.5\n  c6.9,2.7,11.9,5.1,15.1,7.3c3.1,2.2,5.4,4.8,7,7.7C341.7,233.7,342.5,237.2,342.5,241.3z\"></path>\n</g>\n<path fill=\"#96CA4B\" stroke=\"#96CA4B\" stroke-width=\"6\" stroke-miterlimit=\"10\" d=\"M197,392.7c-91.2-8.1-163-85-163-178.3\n  S105.8,44.3,197,36.2V16.1c-102.3,8.2-183,94-183,198.4s80.7,190.2,183,198.4V392.7z\"></path>\n<path fill=\"#5F7FBF\" stroke=\"#5F7FBF\" stroke-width=\"6\" stroke-miterlimit=\"10\" d=\"M229,16.1v20.1c91.2,8.1,163,85,163,178.3\n  s-71.8,170.2-163,178.3v20.1c102.3-8.2,183-94,183-198.4S331.3,24.3,229,16.1z\"></path>\n</svg><div id=\"figwheel-heads-up-content-area\"></div></div></body></html>")

;(deftest ^:unit parse-html-test (is (= [] (vec (hickory/as-hiccup (hickory/parse dom))))))