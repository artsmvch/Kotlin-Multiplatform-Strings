plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false

    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false

    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}