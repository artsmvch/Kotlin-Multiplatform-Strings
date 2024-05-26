package com.artsmvch.strings

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.Incremental

@CacheableTask
abstract class GenerateStringsTask : DefaultTask() {
    private val codeGenerator by lazy(LazyThreadSafetyMode.NONE) { StringsCodeGenerator() }

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val resourcesDirectoryProperty: DirectoryProperty

    @get:Input
    abstract val packageNameProperty: Property<String>

    @get:Input
    abstract val supportedLanguagesProperty: SetProperty<String>

    @get:Input
    abstract val missingTranslationStrategyProperty: Property<MissingTranslationStrategy>

    @get:OutputDirectory
    abstract val generatedCodeDirectoryProperty: DirectoryProperty

    @Option(
        option = "missing-translation-strategy",
        description = "Strategy for missing translations"
    )
    fun setMissingTranslationStrategy(value: String) {
        val strategy = MissingTranslationStrategy.values().find {
            it.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException("Unknown missing translation strategy: $value")
        missingTranslationStrategyProperty.set(strategy)
    }

    @TaskAction
    fun action() {
        val stringResourceContainers = StringResourcesFinder.findStringResources(
            resourcesDir = resourcesDirectoryProperty.asFile.get(),
            supportedLangCodes = supportedLanguagesProperty.get()
        )
        codeGenerator.generateCode(
            directory = generatedCodeDirectoryProperty.asFile.get(),
            packageName = packageNameProperty.get(),
            stringResourceContainers = stringResourceContainers,
            missingTranslationStrategy = missingTranslationStrategyProperty.get(),
            supportedLanguages = supportedLanguagesProperty.get()
        )
    }
}