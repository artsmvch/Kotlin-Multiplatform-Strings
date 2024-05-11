package com.artsmvch.strings

enum class ArgType {
    DECIMAL,
    STRING
}

sealed class StringResource {
    data class Singular(
        val value: String,
        val argTypes: List<ArgType>
    ): StringResource()

    data class Plural(
        val zero: String?,
        val one: String?,
        val two: String?,
        val few: String?,
        val many: String?,
        val other: String?,
        val argTypes: List<ArgType>
    ): StringResource()

    data class Array(
        val values: List<String>
    ): StringResource()
}

data class StringResourcesContainer(
    val langCode: String,
    val stringResources: Map<String, StringResource>
)