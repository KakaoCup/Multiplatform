plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.jetbrains.compose)
    id("org.jetbrains.kotlin.plugin.compose")
    id("convention.publishing")
}

kotlin {
    jvmToolchain(libs.versions.jvmVersion.get().toInt())

    android {
        namespace = "io.github.kakaocup.compose"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            // Compose UI test types are part of the public API (nodes, matchers, rules), so they are exposed to consumers
            api(libs.jetbrains.compose.ui)
            api(libs.jetbrains.compose.ui.test)
        }
        androidMain.dependencies {
            api(libs.androidx.compose.ui.uiTestJunit4)
            api(libs.androidx.test.ext.junit)
        }
    }
}
