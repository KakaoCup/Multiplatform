# Kakao Compose Multiplatform
[![Kotlin version badge](https://img.shields.io/badge/kotlin-2.4.10-blue.svg)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.12.0-blue.svg)](https://www.jetbrains.com/compose-multiplatform/)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=RU&color=0088CC)](https://t.me/kaspresso)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=EN&color=0088CC)](https://t.me/kaspresso_en)
[![Documentation](https://img.shields.io/static/v1?label=Documentation&message=EN&color=008000)](https://kakaocup.github.io/Compose/docs/introduction)

Nice and simple DSL for Compose Multiplatform UI tests in Kotlin

![coco](https://user-images.githubusercontent.com/2812510/30947310-3825724c-a433-11e7-8a0d-3c3bfe00d584.png)

Kakao Compose Multiplatform wraps the Compose UI testing API (`androidx.compose.ui.test`) in a
page-object style DSL. The same `ComposeScreen` and `KNode` definitions run in `commonTest` on
Android and iOS, and keep working in classic Android instrumented tests.

#### Benefits
- Readability
- Reusability
- Extensible DSL
- Interceptors
- One test code base for Android and iOS

### Supported platforms

| Module | Android | iOS (`iosArm64`, `iosSimulatorArm64`) |
|---|:---:|:---:|
| `compose-multiplatform` (core DSL) | ✅ | ✅ |
| `compose-multiplatform-semantics` | ✅ | ✅ |
| `compose-multiplatform-ui` | ✅ | ➖ |
| `compose-multiplatform-test` | ✅ | ➖ |

* `compose-multiplatform` - core module: `ComposeScreen` page objects, `KNode`, matchers, actions, assertions and interceptors
* `compose-multiplatform-semantics` - optional set of semantics keys shared by the `-ui` and `-test` modules
* `compose-multiplatform-ui` - optional wrappers over Material components with the same signatures that publish those semantics
* `compose-multiplatform-test` - optional typed nodes (`KButtonNode`, `KTextNode`, ...) that assert on the published semantics

iOS tests run on the iOS simulator and require macOS with Xcode. Only Apple-silicon simulators are
supported (`iosSimulatorArm64`); there is no `iosX64` target.

A few features of the core module depend on JVM reflection, JUnit4 or Android resources. They are
listed in the [Android-only features](#android-only-features) section together with the iOS alternatives.

### Setup

Kotlin Multiplatform, in the common test source set:
```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation("io.github.kakaocup:compose-multiplatform:<latest version>")
        }
    }
}
```

Android instrumented tests:
```kotlin
dependencies {
    androidTestImplementation("io.github.kakaocup:compose-multiplatform:<latest version>")
}
```

```groovy
dependencies {
    androidTestImplementation 'io.github.kakaocup:compose-multiplatform:<latest version>'
}
```

Optional modules. They are Android-only, so in a Kotlin Multiplatform project put them into Android
source sets (`androidMain`, `androidDeviceTest`) rather than `commonMain` / `commonTest`:
```kotlin
dependencies {
    implementation("io.github.kakaocup:compose-multiplatform-ui:<latest version>")
    androidTestImplementation("io.github.kakaocup:compose-multiplatform-test:<latest version>")
}
```

Every push to `master` publishes `-SNAPSHOT` builds of all modules under the same coordinates. To use them add the
snapshot repository:
```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}
```

Migrating from the Android-only Kakao Compose? See [Migrating from Kakao Compose](#migrating-from-kakao-compose).

### Quick start

#### Kotlin Multiplatform (`commonTest`)
Declare a screen in `commonTest`:
```kotlin
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class MainActivityScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainActivityScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag("MainScreen") }
    ) {

    val myButton: KNode = child {
        hasTestTag("myTestButton")
        hasText("Button 1")
    }
}
```

Run it with `runComposeUiTest`. The `ComposeUiTest` receiver is a `SemanticsNodeInteractionsProvider`,
so it is passed straight into the screen:
```kotlin
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SimpleTest {

    @Test
    fun simpleTest() = runComposeUiTest {
        setContent { MainScreen() }

        MainActivityScreen(this).apply {
            myButton {
                assertIsDisplayed()
                assertTextContains("Button 1")
            }

            onNode {
                hasTestTag("doesNotExist")
            }.invoke {
                assertDoesNotExist()
            }
        }
    }
}
```

On iOS the test runs on the simulator with `:yourModule:iosSimulatorArm64Test`.

On Android, `commonTest` code runs as a device test. Enable the device-test source set in the Android
target and give it a test runner and the activity that hosts `runComposeUiTest`:
```kotlin
kotlin {
    androidLibrary {
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        getByName("androidDeviceTest").dependencies {
            implementation("androidx.test:runner:<version>")
            implementation("androidx.compose.ui:ui-test-manifest:<version>")
        }
    }
}
```
`:yourModule:connectedAndroidDeviceTest` installs and runs the tests on a connected device or emulator.
`:yourModule:assembleAndroidDeviceTest` only builds the test APK, which you can then run elsewhere, for example
through Firebase Test Lab as this repository does in CI. See the [`sample-kmp`](sample-kmp) module for a complete,
working configuration.

#### Android instrumented tests (`androidTest`)
```kotlin
class ExampleInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun simpleTest() {
        onComposeScreen<MainActivityScreen>(composeTestRule) {
            myButton {
                assertIsDisplayed()
                assertTextContains("Button 1")
            }
        }
    }
}
```
`onComposeScreen<T>` instantiates the screen through reflection, so it only works on Android. On iOS
instantiate the screen directly as shown in the multiplatform example above. Direct instantiation
works on Android too. See the [`sample`](sample) module for the full Android test suite.

### Concept
One of the main concepts of Compose is a presentation of UI according to a UI tree (UI hierarchy) with
parent-children relationships. It means that a UI test library for Compose has to support parent-children
relationships for nodes by default. Below is how Kakao Compose Multiplatform supports that approach.

### How to use it
#### Create Screen
Create your entity `ComposeScreen` where you will add the nodes involved in the interactions of the tests:
```kotlin
class MainActivityScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainActivityScreen>(
        semanticsProvider = semanticsProvider
    )
```
`ComposeScreen` can represent the whole user interface or a portion of UI.
If you are using the [Page Object pattern](https://martinfowler.com/bliki/PageObject.html) you can put the interactions of Kakao inside the Page Objects.

Usually a `Screen` in Compose is a UI element (node) too. That's why there is an additional option to declare `ComposeScreen`:
```kotlin
class MainActivityScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainActivityScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag("MainScreen") }
    )
```
So, `ComposeScreen` is a `BaseNode` inheritor. As you've seen above, it is possible to describe
`ComposeScreen` without a `viewBuilderAction` when the screen is an abstraction without a clear connection to any node.

#### Create KNode
`ComposeScreen` contains `KNode`s, the Compose nodes where you want to do the interactions:
```kotlin
class MainActivityScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainActivityScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag("MainScreen") }
    ) {

    val myButton: KNode = child {
        hasTestTag("myTestButton")
    }
}
```

`myButton` was declared as a child of `MainActivityScreen`.
It means that `myButton` will be matched using the matchers specified in the lambda explicitly and the parent matcher implicitly (`MainActivityScreen`).
Under the hood, the `SemanticsMatcher` of `myButton` is equal to `hasTestTag("myTestButton") and hasAnyAncestor(MainActivityScreen.matcher)`.

Also, `KNode` can be declared as a child of another `KNode`:
```kotlin
class MainActivityScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainActivityScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag("MainScreen") }
    ) {

    val myButton: KNode = child {
        hasTestTag("myTestButton")
    }

    val myButton2: KNode = myButton.child {
        hasTestTag("myTestButton2")
    }
}
```
`myButton2` will be matched with
`hasTestTag("myTestButton2") and hasAnyAncestor(myButton.matcher) and hasAnyAncestor(MainActivityScreen.matcher)`.
We advise not to abuse nesting and to use only the chain "ComposeScreen" - "Element of ComposeScreen".

The last, `KNode` can be declared without the `child` function using only explicit matchers:
```kotlin
class MainActivityScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainActivityScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag("MainScreen") }
    ) {

    val myButton = KNode(semanticsProvider) {
        hasTestTag("myTestButton")
    }
}
```

Every `KNode` is described by matchers. Some of the matchers provided by Kakao Compose Multiplatform:

* `hasText`
* `hasTestTag`
* `hasContentDescription`
* `hasClickAction`
* `isEnabled` / `isNotEnabled`
* `isDialog` / `isPopup`
* `hasAnyAncestor` / `hasAnyDescendant` / `hasAnySibling`
* `hasPosition` to pick the n-th node when several nodes match
* `addSemanticsMatcher` to plug in any custom `SemanticsMatcher`
* <b>and more</b>

Matchers combine inside one builder:
```kotlin
val myButton: KNode = child {
    hasTestTag("myTestButton")
    hasText("Button 1")
}
```

#### Write the interaction
Once you have the `ComposeScreen` and the `KNode` defined, you only have to apply
the actions or assertions you know from Compose UI test:
```kotlin
MainActivityScreen(this).apply {
    myButton {
        assertIsDisplayed()
        assertTextContains("Button 1")
        performClick()
    }

    onNode {
        hasTestTag("doesNotExist")
    }.invoke {
        assertDoesNotExist()
    }
}
```

Available on every node: assertions such as `assertIsDisplayed`, `assertExists`, `assertDoesNotExist`,
`assertTextEquals`, `assertTextContains`, `assertIsEnabled`, `assertIsSelected`, `assertHasClickAction`;
actions such as `performClick`, `performTextInput`, `performTextClearance`, `performScrollTo`,
`performScrollToIndex`, `performTouchInput`, `performSemanticsAction`.

#### Intercepting

If you need to add custom logic during the `Kakao Compose -> Compose UI test` call chain (for example, logging) or
if you need to completely change the events/commands that are being sent to Compose UI test
during runtime in some cases, you can use the intercepting mechanism. Interceptors are part of the
common module and work on every platform.

Interceptors are lambdas that you pass to a configuration DSL that will be invoked before calls happening from inside Kakao Compose.

You have the ability to provide interceptors at two different levels: the Kakao Compose runtime and any individual `BaseNode` instance.
Interceptors support the parent-children concept too: any `BaseNode` inherits the interceptors of its parents.

On each invocation of a Compose UI test function that can be intercepted, Kakao Compose will aggregate all available interceptors
for this particular call and invoke them in descending order: `Active BaseNode interceptor -> Interceptor of the parent Active BaseNode ->
... -> Kakao Compose interceptor`.

Each of the interceptors in the chain can break the chain call by setting `isOverride` to true during configuration.
In that case Kakao Compose will not only stop invoking the remaining interceptors in the chain, **but will not perform the
Compose UI test call**. It means that in such case, the responsibility to actually invoke the interaction lies on the shoulders
of the developer.

Here is an example of intercepting configurations, written for `commonTest`:
```kotlin
@OptIn(ExperimentalTestApi::class)
class SomeTest {
    private val log = mutableListOf<String>()

    @BeforeTest
    fun setup() {
        KakaoCompose { // Kakao Compose runtime
            intercept {
                onComposeInteraction {
                    onAll { log.add("ALL") }
                    onCheck { _, _ -> log.add("CHECK") }
                    onPerform { _, _ -> log.add("PERFORM") }
                }
            }
        }
    }

    @AfterTest
    fun tearDown() {
        KakaoCompose.reset() // drop runtime-level interceptors between tests
    }

    @Test
    fun test() = runComposeUiTest {
        setContent { MyContent() }

        MyScreen(this).apply {
            intercept {
                onCheck { interaction, assertion -> // Intercept check() call
                    println("$interaction is checking $assertion")
                }
            }

            myView {
                intercept { // Intercepting ComposeInteraction calls on this individual Node
                    onPerform(true) { interaction, action -> // Intercept perform() call and override the chain
                        // When performing actions on this node, the Kakao level interceptor will not be called
                        // and we have to manually invoke the interaction now.
                        println("$interaction is performing $action")
                        interaction.perform(action)
                    }
                }
            }
        }
    }
}
```
The same `intercept { }` blocks work inside `onComposeScreen<MyScreen>(composeTestRule) { ... }` on Android.
For more detailed info please refer to the documentation.

#### Unmerged tree

By default Compose UI test matches against the merged semantics tree, which creates a lot of inconveniences with node matching.
You can switch a single node to the unmerged tree inside its builder:
```kotlin
val myButton: KNode = child {
    useUnmergedTree = true
    hasTestTag("myTestButton")
}
```
or override the default globally. The override is read when a node is built, so set it before the screens are created,
for example in a `@BeforeTest` method:
```kotlin
KakaoCompose.Override.useUnmergedTree = true
```
On Android the same can be done per test class with `KakaoComposeTestRule` (see below).

### Android-only features

Two groups of features do not work on iOS.

The first group is common API that compiles on every platform but throws `UnsupportedOperationException`
at runtime on iOS, because it needs JVM reflection or Android resources:

* `onComposeScreen<T>()`: instantiate the screen directly instead, `MyScreen(provider).apply { ... }`.
* Resource-id overloads of matchers such as `hasText(R.string.title)` or `hasTestTag(R.string.tag)`:
  iOS has no Android resource ids, so use string literals or Compose Resources there.
* Custom `BaseNode` subclasses as children via `child<MyNode> { ... }`: on Android the subclass needs a public
  `(SemanticsNodeInteractionsProvider, NodeMatcher, BaseNode<*>)` constructor, which is looked up by reflection.
  On iOS `child` always produces a `KNode`, so declare children as `KNode` or construct the custom subclass directly.

The second group exists only in the Android source set of `compose-multiplatform`, so it can be referenced from
`androidTest` or `androidDeviceTest` but not from `commonTest`:

* `KakaoComposeTestRule` (JUnit4 rule)
* `KLazyListNode` / `KLazyListItemNode` and `KListNode` / `KListItemNode`
* `waitUntil { ... }` on any node, built on the JUnit4 `ComposeTestRule`. On iOS use `ComposeUiTest.waitUntil` directly.
* Resource-id overloads of assertions such as `assertTextEquals(R.string.title)`

#### `onComposeScreen<T>`
Instantiates a screen by reflection so you don't repeat the class name:
```kotlin
onComposeScreen<MainActivityScreen>(composeTestRule) {
    myButton { assertIsDisplayed() }
}
```

#### `KakaoComposeTestRule`

Every Compose screen requires a `semanticsProvider`, which creates boilerplate like `onComposeScreen<LazyListScreen>(composeTestRule) {`.
You can provide the `composeTestRule` once through `KakaoComposeTestRule` and then use all screens without injection like `onComposeScreen<LazyListScreen> {`.
Both styles can be mixed: an explicitly injected provider overrides the one supplied via `KakaoComposeTestRule`.
The rule also overrides `useUnmergedTree` for the duration of the test and restores the previous values once the test
passes.

The no-argument `onComposeScreen<T> { }` creates the screen through its no-argument constructor, so such screens must
give `semanticsProvider` a default:
```kotlin
class GlobalSemanticScreen(semanticsProvider: SemanticsNodeInteractionsProvider? = null) :
    ComposeScreen<GlobalSemanticScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag("MainScreen") },
    ) {

    val myText1: KNode = child {
        hasTestTag("mySimpleText")
        hasPosition(0)
    }
}
```

```kotlin
@get:Rule
val composeTestRule = createAndroidComposeRule<MainActivity>()

@get:Rule
val kakaoComposeTestRule = KakaoComposeTestRule(
    semanticsProvider = composeTestRule,
    useUnmergedTree = true
)

@Test
fun simpleTest() {
    onComposeScreen<GlobalSemanticScreen> {
        myText1 {
            assertIsDisplayed()
            assertTextContains("Simple text 1")
        }
    }
}
```

You can find a full example in [SimpleTestGlobalSemantic](sample/src/androidTest/java/io/github/kakaocup/compose/test/SimpleTestGlobalSemantic.kt).

#### Lazy lists testing

:warning: This API is experimental and might change in future!

To test lazy lists such as `LazyRow` or `LazyColumn` add a `KLazyListNode` to your `ComposeScreen`:
```kotlin
val list = KLazyListNode(
    semanticsProvider = semanticsProvider,
    viewBuilderAction = { hasTestTag("LazyList") },
    itemTypeBuilder = {
        itemType(::LazyListItemNode)
        itemType(::LazyListHeaderNode)
    },
    positionMatcher = { position -> SemanticsMatcher.expectValue(LazyListItemPosition, position) }
)
```

Inside the `itemTypeBuilder` function register `KLazyListItemNode` types to differentiate elements in the lazy list:
```kotlin
class LazyListItemNode(
    semanticsNode: SemanticsNode,
    semanticsProvider: SemanticsNodeInteractionsProvider,
) : KLazyListItemNode<LazyListItemNode>(semanticsNode, semanticsProvider)

class LazyListHeaderNode(
    semanticsNode: SemanticsNode,
    semanticsProvider: SemanticsNodeInteractionsProvider,
) : KLazyListItemNode<LazyListHeaderNode>(semanticsNode, semanticsProvider) {
    val title: KNode = child {
        hasTestTag("LazyListHeaderTitle")
    }
}
```

The element position might change during scrolling due to lazy list construction, that's why a `positionMatcher` is needed to determine the element position correctly. It can be achieved in different ways, for example through a `TestTag`:
```kotlin
LazyColumn(
    Modifier
        .fillMaxSize()
        .testTag("LazyList")
) {
    itemsIndexed(items) { index, item ->
        when (item) {
            is LazyListItem.Header -> ListItemHeader(item, Modifier.testTag("position=$index"))
            is LazyListItem.Item -> ListItemCell(item, Modifier.testTag("position=$index"))
        }
    }
}
```

And then check this position inside the `positionMatcher` lambda:
```kotlin
positionMatcher = { position -> hasTestTag("position=$position") }
```

It is more convenient and less error prone to create a custom semantics property and modifier:
```kotlin
val LazyListItemPosition = SemanticsPropertyKey<Int>("LazyListItemPosition")
var SemanticsPropertyReceiver.lazyListItemPosition by LazyListItemPosition

fun Modifier.lazyListItemPosition(position: Int): Modifier {
    return semantics { lazyListItemPosition = position }
}
```

And check an item position with `SemanticsMatcher`:
```kotlin
positionMatcher = { position -> SemanticsMatcher.expectValue(LazyListItemPosition, position) }
```

So the typical lazy list test may look like this:
```kotlin
@Test
@OptIn(ExperimentalTestApi::class)
fun lazyListTest() {
    onComposeScreen<LazyListScreen>(composeTestRule) {
        list {
            firstChild<LazyListHeaderNode> {
                title.assertTextEquals("Items from 1 to 10")
            }
            childWith<LazyListItemNode> {
                hasText("Item 1")
            } perform {
                assertTextEquals("Item 1")
            }
            childAt<LazyListItemNode>(10) {
                assertTextEquals("Item 10")
            }
        }
    }
}
```

Check the lazy list test [example](sample/src/androidTest/java/io/github/kakaocup/compose/test/LazyListTest.kt) for more information.
Non-lazy lists are covered by `KListNode` / `KListItemNode` in the `node.element.list` package, see the
[list tests](sample/src/androidTest/java/io/github/kakaocup/compose/test/list).

### Migrating from Kakao Compose
The artifacts were renamed when the library moved to Compose Multiplatform, starting at version `2.0.0`:

| Kakao Compose | Kakao Compose Multiplatform |
|---|---|
| `io.github.kakaocup:compose` | `io.github.kakaocup:compose-multiplatform` |
| `io.github.kakaocup:compose-semantics` | `io.github.kakaocup:compose-multiplatform-semantics` |
| `io.github.kakaocup:compose-ui` | `io.github.kakaocup:compose-multiplatform-ui` |
| `io.github.kakaocup:compose-test` | `io.github.kakaocup:compose-multiplatform-test` |

Package names did not change, so imports stay the same. Remove the old artifacts when switching: keeping both makes Gradle report a capability conflict.

Existing Android tests keep working without code changes. To share tests between Android and iOS, move your
screens and tests to `commonTest`, replace `onComposeScreen<T>(rule)` with direct instantiation, and run them
with `runComposeUiTest`.

### Contribution Policy

**Kakao Compose Multiplatform** is an open source project, and depends on its users to improve it. We are more than happy to find you interested in taking the project forward.

Kindly refer to the [Contribution Guidelines](https://github.com/KakaoCup/Multiplatform/blob/master/CONTRIBUTING.md) for detailed information.

### Code of Conduct

Please refer to [Code of Conduct](https://github.com/KakaoCup/Multiplatform/blob/master/CODE_OF_CONDUCT.md) document.

### License

Kakao Compose Multiplatform is open source and available under the [Apache License, Version 2.0](https://github.com/KakaoCup/Multiplatform/blob/master/LICENSE).

### Thanks for supporting Open Source
<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.png" height="100"> <img src="https://firebase.google.com/static/downloads/brand-guidelines/SVG/logo-logomark.svg" width="100" height="100">
