---
sidebar_position: 2
---

# Setup

### Setup project dependencies
Add those dependencies into your `build.gradle` or `build.gradle.kts` file

:::tip Version

`<latest version>` can be found on project [GitHub](https://github.com/KakaoCup/Multiplatform)

:::

```kotlin
dependencies {
    androidTestImplementation("io.github.kakaocup:compose-multiplatform:<latest version>")
}
```

For a Kotlin Multiplatform project, add it to the common test source set instead:

```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation("io.github.kakaocup:compose-multiplatform:<latest version>")
        }
    }
}
```

:::info Migrating from Kakao Compose

The artifacts were renamed when the library moved to Compose Multiplatform: `io.github.kakaocup:compose` became
`io.github.kakaocup:compose-multiplatform` (likewise `-semantics`, `-ui` and `-test`), starting at version `2.0.0`.
Package names did not change, so imports stay the same. Remove the old artifacts when switching: keeping both
makes Gradle report a capability conflict.

:::
