#HSLIDE

### True Separation of  the V in MVC

#HSLIDE

#### What is the problem?

Most view-components of current web apps are designer-unfriendly. This complicates the collaboration between designers and developers and hinders designers to leverage their area of expertise.

#VSLIDE

#### HTML obscured by extensive display logic
```HTML
 <#if ((products?size % (columns * 2) > 0 && products?size % (columns * 2) <= columns - 1 )) && menuProperties?? && showBanner>
            <a href="/product">Product</a>
<#elseif (isRowEven || (isRowUneven && (!menuProperties?? || !showBanner))) && product_has_next>
            <hr class="line"/>
</#if>
```

#VSLIDE

#### HTML obscured by framework code 

```JavaScript
class Clock extends React.Component {
  constructor(props) {
    super(props);
    this.state = {date: new Date()};
  }

  componentDidMount() {
    this.timerID = setInterval(() => this.tick(), 1000);
  }

  componentWillUnmount() {
    clearInterval(this.timerID);
  }

  tick() {
    this.setState({date: new Date()});
  }

  render() {
    return (
      <div>
        <h1>Hello, world!</h1>
        <h2>It is {this.state.date.toLocaleTimeString()}.</h2>
      </div>
    );
  }
}
```

#VSLIDE

#### Visual verification impossible without special tools and/or running server

```HTML
<div ng-controller="Controller">
  Hello <input ng-model='name'> <hr/>
  <span ng-bind="name"></span> <br/>
</div>
```

![Angular](docs/angular.png)

#HSLIDE

#### Why is this a problem? I

"I am a full stack developer, I do not need a dedicated designer."

- While certainly possible, being excellent on two somewhat unrelated skills like UX/UI design and clean coding is rare.       <!-- .element: class="fragment" -->
- When working on the layout and structure of a website, one profits of the bounded context between designing and coding.      <!-- .element: class="fragment" -->

#VSLIDE

#### Why is this a problem? II

"Designers can hand over mockups or concept art and developers implement them."

- Designers can iterate directly in HTML and get feedback immediately.       <!-- .element: class="fragment" -->
- Less waste gets produced. There are no throw away design artefacts.    <!-- .element: class="fragment" -->
- Designers and developers work with each other, not one for the other. This result in less misunderstandings.  <!-- .element: class="fragment" -->

#VSLIDE

#### Why is this a problem? III

"This is not a problem in our project, since we automated all the moving parts. All designers only have to install docker, set the right flags, pull the image, start the container, install npm, gulp, webpack, babel, yarn, react, redux ..."

- Every additional tool makes the whole development process a little more brittle. <!-- .element: class="fragment" -->
- The direction of the dependency is wrong. Development should simplify the design process, not complicate it. <!-- .element: class="fragment" -->

#HSLIDE

#### What is the solution?

- Write static, pure HTML files.
- Any dynamic parts are represented by placeholders.
- Add all elements that are displayed eventually.
- Transformations of the DOM with the actual values, depending on the app state, are either applied on the server or on the client-side.

#VSLIDE

#### Example

Local static html file for large displays
![evermento_L](docs/evermento_L.png)

#VSLIDE

#### Example

Same html file for small displays

![evermento_S](docs/evermento_S.png)

#VSLIDE

#### Example of a DOM transformation

This example uses [alive](https://github.com/lomin/alive), a selector-based (à la CSS) templating library for Clojure and ClojureScript

The following  either displays the answer with the  

```Clojure
(me.lomin.alive.macros/deftemplate evermento-html "evermento.html")
(me.lomin.alive.macros/import-id id#memo-container)
(me.lomin.alive.macros/import-class _subcontainer)
(me.lomin.alive.macros/import-class _answer)

(def page-container
  (com.rpl.specter.macros/select-first
    [id#page-container]
    evermento-html))

(defn get-attrs [node]
  (comment A hiccup structure looks like this:
           [:a {:href "http://github.com"} "GitHub"])
  (second node))

(defn select-container [container id]
  (com.rpl.specter.macros/transform
    [_subcontainer #(= id (:id (get-attrs %)))]
    (me.lomin.alive/replace-content [])
    container))

(defn render-answer [node]
  (let [change-visibility (if (show-answer?)
                            me.lomin.alive/remove-class
                            me.lomin.alive/add-class)]
    (->> node
         (alive/replace-content [(get-answer)])
         (change-visibility "hidden"))))

(com.rpl.specter.macros/transform
  [id#memo-container _answer]
  render-answer
  (select-container page-container "main-container"))
```

#HSLIDE

#### Why is that the solution?

 - Designers can use any tools they like and are used to <!-- .element: class="fragment" -->
 - No sophisticated build chain, preprocessors, etc. necessary <!-- .element: class="fragment" -->
 - Developers can use the full power and expressiveness of their language <!-- .element: class="fragment" -->
 - Easy to unit test  <!-- .element: class="fragment" -->

#VSLIDE

#### Example for unit test with alive
```Clojure
(deftest ^:unit render-answer-test
  (let [answer #(transform
                 [evermento/id#memo-container evermento/_answer]
                 evermento/render-answer)]
    
    (testing "show answer"
      (evermento/assoc-in* [:show-answer] true)
      (evermento/assoc-in* [:answer] "A test answer.")
      (is (= [:div {:class "answer"} "A test answer."]
             (answer))))

    (testing "do not show answer"
      (evermento/assoc-in* [:show-answer] false)
      (is (= [:div {:class "answer hidden"} "A test answer."]
             (answer))))))
```

