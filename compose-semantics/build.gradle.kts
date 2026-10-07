plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.jetbrains.compose)
    id("org.jetbrains.kotlin.plugin.compose")
    id("convention.publishing")
}
kotlin {
    android {
        namespace = "io.github.kakaocup.compose.semantics"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.material)
        }
        androidMain.dependencies {
            implementation(libs.jetbrains.compose.ui.uiTooling)
        }
    }
}
