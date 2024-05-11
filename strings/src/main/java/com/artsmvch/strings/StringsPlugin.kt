package com.artsmvch.strings

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

abstract class StringsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val kotlinMultiplatform = requireNotNull(target.extensions.findByType(KotlinMultiplatformExtension::class.java)) {
            "KotlinMultiplatform extension not found"
        }
        configureDependencies(target, kotlinMultiplatform)
        val generatedCodeDirectory: File =
            File(target.buildDir, "generated/source/strings").apply { mkdirs() }
        val stringsExtension: StringsExtension =
            target.extensions.create("strings", StringsExtension::class.java)
        kotlinMultiplatform.sourceSets.findByName("commonMain")!!.apply {
            kotlin.srcDir(generatedCodeDirectory)
        }
        target.convention
        val generateStringsTask = target.tasks.register("generateStrings", GenerateStringsTask::class.java) {
            this.generatedCodeDirectoryProperty.set(generatedCodeDirectory)
            this.resourcesDirectoryProperty.set(stringsExtension.resourcesDir ?: defaultResourcesDirectory(project))
            this.packageNameProperty.set(stringsExtension.packageName)
            this.supportedLanguagesProperty.set(stringsExtension.supportedLanguages)
            this.missingTranslationStrategyProperty.set(stringsExtension.missingTranslationStrategy)
        }
        target.tasks.withType(KotlinCompile::class.java) {
            dependsOn(generateStringsTask)
        }
        target.tasks.register("reportMissingTranslations", ReportMissingTranslations::class.java) {
            this.buildDirectoryProperty.set(target.buildDir)
            this.resourcesDirectoryProperty.set(stringsExtension.resourcesDir ?: defaultResourcesDirectory(project))
            this.supportedLanguagesProperty.set(stringsExtension.supportedLanguages)
        }
    }

    private fun configureDependencies(target: Project, kotlinMultiplatform: KotlinMultiplatformExtension) {
        // TODO: create an in-house solution for string formatting
        // "https://github.com/sergeych/mp_stools"
        target.rootProject.subprojects {
            repositories {
                maven("https://maven.universablockchain.com/")
            }
        }
        kotlinMultiplatform.sourceSets.findByName("commonMain")!!.apply {
            dependencies {
                implementation("net.sergeych:mp_stools:1.4.3")
            }
        }
    }

    private fun defaultResourcesDirectory(project: Project): File {
        return project.file("src/commonMain/composeResources")
    }
}