plugins {
    id("convention.library")
    id("convention.publishing")
}

android {
    namespace = "io.github.kakaocup.compose.test"
}

dependencies {
    implementation(project(":compose-multiplatform"))
    implementation(project(":compose-multiplatform-semantics"))

    implementation(libs.androidx.compose.ui.uiTooling)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.ui.uiTestJunit4)
}
