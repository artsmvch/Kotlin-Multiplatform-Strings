package com.artsmvch.strings

import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

internal object StringResourcesLookup {
    private const val VALUES_DIR_NAME = "values"
    private const val STRINGS_FILE_NAME = "strings.xml"

    private val ARG_PATTERN = Pattern.compile("%([0-9]+)(\\\$s|\\\$d)")

    fun findStringResources(
        resourcesDir: File,
        supportedLangCodes: Set<String>
    ): List<StringResourcesContainer> {
        require(resourcesDir.exists()) { "Resources dir does not exist" }
        require(resourcesDir.isDirectory) { "Resources dir is not a directory" }
        val stringResourceContainers =
            ArrayList<StringResourcesContainer>(supportedLangCodes.count())
        resourcesDir.listFiles()?.forEach { file ->
            if (file.exists() && file.isDirectory) {
                val filenameParts = file.name.split("-")
                if (filenameParts[0] == VALUES_DIR_NAME) {
                    val langCode = filenameParts.intersect(supportedLangCodes).firstOrNull()
                    val stringsFile = file.listFiles()?.find { it.name == STRINGS_FILE_NAME }
                    if (langCode != null && stringsFile != null) {
                        val container = StringResourcesContainer(
                            langCode = langCode,
                            stringResources = parseStringResources(stringsFile)
                        )
                        stringResourceContainers.add(container)
                    }
                }
            }
        }
        return stringResourceContainers
    }

    private fun parseStringResources(xmlFile: File): Map<String, StringResource> {
        val parser = XmlParser()
        val reader = InputStreamReader(FileInputStream(xmlFile), StandardCharsets.UTF_8)
        val rootNode = parser.parse(reader)
        val resourceNodes = rootNode.value() as NodeList
        val stringResources = HashMap<String, StringResource>(resourceNodes.count())
        resourceNodes.forEach { node ->
            if (node is Node) {
                val key = node.attribute("name").toString()
                parseStringResource(node)?.also {
                    stringResources[key] = it
                }
            }
        }
        return stringResources
    }

    private fun parseStringResource(node: Node): StringResource? {
        return when (node.name()) {
            "string" -> parseSingular(node)
            "plurals" -> parsePlural(node)
            "string-array" -> parseArray(node)
            else -> null
        }
    }

    private fun parseSingular(node: Node): StringResource.Singular {
        val value = node.text().let(StringResourcesLookup::processString)
        val argTypes = getArgTypes(value)
        return StringResource.Singular(value, argTypes)
    }

    private fun getArgTypes(value: String): List<ArgType> {
        val matcher = ARG_PATTERN.matcher(value)
        val argTypes = ArrayList<ArgType>()
        matcher.results().forEach { matchResult ->
            val result = value.substring(matchResult.start(), matchResult.end())
            val argType = when {
                result.endsWith("\$d") -> ArgType.DECIMAL
                result.endsWith("\$s") -> ArgType.STRING
                else -> throw IllegalArgumentException("Unknown argument type: $result")
            }
            argTypes.add(argType)
        }
        return argTypes
    }

    private fun parsePlural(node: Node): StringResource.Plural {
        var zero: String? = null
        var one: String? = null
        var two: String? = null
        var few: String? = null
        var many: String? = null
        var other: String? = null
        var argTypes: List<ArgType>? = null
        (node.value() as NodeList).forEach { childNode ->
            if (childNode is Node) {
                val value = childNode.text().let(StringResourcesLookup::processString)
                when (childNode.attribute("quantity")) {
                    "zero" -> zero = value
                    "one" -> one = value
                    "two" -> two = value
                    "few" -> few = value
                    "many" -> many = value
                    "other" -> other = value
                }
                if (argTypes == null) {
                    argTypes = getArgTypes(value)
                }
            }
        }
        return StringResource.Plural(
            zero = zero,
            one = one,
            two = two,
            few = few,
            many = many,
            other = other,
            argTypes = argTypes ?: emptyList()
        )
    }

    private fun parseArray(node: Node): StringResource.Array {
        val values = ArrayList<String>()
        (node.value() as NodeList).forEach { childNode ->
            if (childNode is Node) {
                val value = childNode.text().let(StringResourcesLookup::processString)
                values.add(value)
            }
        }
        return StringResource.Array(values)
    }

    private fun processString(value: String): String {
        // Removing escape characters
        return value.replace("\\'", "'")
    }
}