plugins {
    `kotlin-dsl`
}

version = "1.0.0"
group = "com.artsmvch.plugins"

gradlePlugin {
    plugins {
        create("strings") {
            id = "com.artsmvch.strings"
            implementationClass = "com.artsmvch.strings.StringsPlugin"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleKotlinDsl())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0-RC2")
    implementation("com.squareup:kotlinpoet:1.15.3") {
        exclude(module = "kotlin-reflect")
    }
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.11.1")
}