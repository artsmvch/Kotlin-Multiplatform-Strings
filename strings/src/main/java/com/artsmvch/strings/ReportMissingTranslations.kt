package com.artsmvch.strings

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.Incremental
import java.io.File
import java.io.FileWriter

abstract class ReportMissingTranslations : DefaultTask() {
    @get:OutputDirectory
    abstract val buildDirectoryProperty: DirectoryProperty

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val resourcesDirectoryProperty: DirectoryProperty

    @get:Input
    abstract val supportedLanguagesProperty: SetProperty<String>

    @get:Input
    abstract val missingTranslationReportOutputProperty: Property<MissingTranslationReportOutput>

    @Option(
        option = "output",
        description = "Report output for missing translations"
    )
    fun setMissingTranslationReportOutput(value: String) {
        val output = MissingTranslationReportOutput.values().find {
            it.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException("Unknown missing translation report output: $value")
        missingTranslationReportOutputProperty.set(output)
    }

    @TaskAction
    fun action() {
        val stringResourceContainers = StringResourcesFinder.findStringResources(
            resourcesDir = resourcesDirectoryProperty.asFile.get(),
            supportedLangCodes = supportedLanguagesProperty.get()
        )
        val missingTranslations = findMissingTranslations(stringResourceContainers)
        reportMissingTranslations(missingTranslations,
            missingTranslationReportOutputProperty.getOrElse(MissingTranslationReportOutput.CONSOLE))
    }

    /**
     * Returns a map of missing translations: [key -> languages for which there is no translation].
     */
    private fun findMissingTranslations(
        containers: List<StringResourcesContainer>
    ): List<MissingTranslation> {
        val allStringKeys = containers.flatMap { it.stringResources.keys }
        val missingTranslations = ArrayList<MissingTranslation>()
        allStringKeys.forEach { key ->
            var missingTranslationLanguages: MutableSet<String>? = null
            var defaultResource: StringResource? = null
            containers.forEach { container ->
                if (!container.stringResources.contains(key)) {
                    if (missingTranslationLanguages == null) {
                        missingTranslationLanguages = HashSet()
                    }
                    missingTranslationLanguages!!.add(container.langCode)
                } else {
                    // TODO: the default string resources must be defined by the user
                    // The default translation is taken from the first container containing the key
                    if (defaultResource == null) {
                        defaultResource = container.stringResources[key]
                    }
                }
            }
            if (!missingTranslationLanguages.isNullOrEmpty()) {
                val missingTranslation = MissingTranslation(
                    key = key,
                    languages = missingTranslationLanguages!!,
                    defaultResource = defaultResource
                )
                missingTranslations.add(missingTranslation)
            }
        }
        return missingTranslations
    }

    private fun reportMissingTranslations(
        missingTranslations: List<MissingTranslation>,
        output: MissingTranslationReportOutput
    ) {
        when(output) {
            MissingTranslationReportOutput.CONSOLE ->
                reportMissingTranslationsToConsole(missingTranslations)
            MissingTranslationReportOutput.CSV ->
                reportMissingTranslationsToCSV(missingTranslations)
        }
    }

    private fun reportMissingTranslationsToConsole(missingTranslations: List<MissingTranslation>) {
        if (missingTranslations.isEmpty()) {
            println("No missing translations")
            return
        }
        val rows = ArrayList<List<String>>()
        rows.add(listOf("Key", "Languages", "Default"))
        missingTranslations.mapTo(rows) { missingTranslation ->
            listOf(
                missingTranslation.key,
                missingTranslation.defaultResource?.let(::getType).orEmpty(),
                missingTranslation.languages.joinToString(", "),
                missingTranslation.defaultResource?.let(::getValues).orEmpty()
            )
        }
        println(PrettyFormatter.table(rows))
    }

    private fun reportMissingTranslationsToCSV(missingTranslations: List<MissingTranslation>) {
        val reportDir = File(buildDirectoryProperty.asFile.get(), "report/strings").apply { mkdirs() }
        val outputFile = File(reportDir, "missing_translations.csv")
        val writer = FileWriter(outputFile)
        writer.write("key,type,languages,default\n")
        missingTranslations.forEach { missingTranslation ->
            val line = StringBuilder().apply {
                // Key
                append(missingTranslation.key)
                append(',')
                // Type
                append(missingTranslation.defaultResource?.let(::getType))
                append(',')
                // Languages
                append('\"').append(missingTranslation.languages.joinToString(separator = "/")).append('\"')
                append(',')
                // Values
                append('\"').append(missingTranslation.defaultResource?.let(::getValues)).append('\"')
                append('\n')
            }.toString()
            writer.write(line)
        }
        writer.flush()
        writer.close()
    }

    private data class MissingTranslation(
        val key: String,
        val languages: Set<String>,
        val defaultResource: StringResource?
    )

    private fun getType(resource: StringResource): String {
        return when(resource) {
            is StringResource.Array -> "array"
            is StringResource.Plural -> "plural"
            is StringResource.Singular -> "singular"
        }
    }

    private fun getValues(resource: StringResource): String {
        return when(resource) {
            is StringResource.Array -> resource.values.joinToString(separator = ",")
            is StringResource.Plural -> {
                listOf(
                    "zero" to resource.zero,
                    "one" to resource.one,
                    "two" to resource.two,
                    "few" to resource.few,
                    "many" to resource.many,
                    "other" to resource.other
                ).filter {
                    it.second != null
                }.joinToString(separator = ",") {
                    it.first + ":" + it.second
                }
            }
            is StringResource.Singular -> resource.value
        }
    }
}