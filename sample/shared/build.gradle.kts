buildscript {
    dependencies {
        classpath("com.artsmvch.plugins:strings")
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("com.artsmvch.strings")
}

strings {
    supportedLanguages = setOf("en", "es", "pl")
    packageName = "com.strings.sample"
}

version = "1.0"

kotlin {
    android()

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }

    ios()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.androidx.core)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
            }
        }
        val androidMain by getting
        val iosMain by getting
    }
}

android {
    namespace = "com.strings.sample.shared"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/commonMain/resources", "src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = 26
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
