package com.artsmvch.strings

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import java.io.File
import kotlin.reflect.KClass

internal class StringsCodeGenerator {
    fun generateCode(
        directory: File,
        packageName: String,
        missingTranslationStrategy: MissingTranslationStrategy,
        supportedLanguages: Set<String>,
        stringResourceContainers: List<StringResourcesContainer>
    ) {
        if (stringResourceContainers.isEmpty()) return
        // TODO: the default string resources must be defined by the user
        val defaultStringResources =
            stringResourceContainers.maxBy { it.stringResources.size }.stringResources
        generateStrings(
            directory = directory,
            packageName = packageName,
            langCode = null,
            targetStringResources = defaultStringResources,
            baseStringResources = defaultStringResources,
            missingTranslationStrategy = missingTranslationStrategy,
            isImplementation = false
        )
        stringResourceContainers.forEach { container ->
            generateStrings(
                directory = directory,
                packageName = packageName,
                langCode = container.langCode,
                targetStringResources = container.stringResources,
                baseStringResources = defaultStringResources,
                missingTranslationStrategy = missingTranslationStrategy,
                isImplementation = true
            )
        }
        generateExtensions(
            directory = directory,
            packageName = packageName
        )
        generateUtils(
            directory = directory,
            packageName = packageName,
            supportedLanguages = supportedLanguages
        )
    }

    private fun generateStrings(
        directory: File,
        packageName: String,
        langCode: String?,
        targetStringResources: Map<String, StringResource>,
        baseStringResources: Map<String, StringResource>,
        missingTranslationStrategy: MissingTranslationStrategy,
        isImplementation: Boolean
    ) {
        val stringsName = if (isImplementation) {
            "$langCode$CLASSNAME_STRINGS"
        } else {
            CLASSNAME_STRINGS
        }
        val fileSpecBuilder = FileSpec.builder(packageName, stringsName)
        val typeSpecBuilder = if (isImplementation) {
            TypeSpec.objectBuilder(stringsName)
                .addSuperinterface(ClassName(packageName, CLASSNAME_STRINGS))
                .addModifiers(KModifier.INTERNAL)
        } else {
            TypeSpec.interfaceBuilder(stringsName)
        }
        baseStringResources.forEach { (key, baseStringResource) ->
            var stringResource = targetStringResources[key]
            if (stringResource == null) {
                when (missingTranslationStrategy) {
                    MissingTranslationStrategy.USE_DEFAULT -> {
                        stringResource = baseStringResource
                    }
                    MissingTranslationStrategy.FAIL_BUILD -> {
                        throw IllegalArgumentException("Missing translations found: " +
                                "lang=$langCode, key=$key")
                    }
                }
            }
            when (stringResource) {
                is StringResource.Singular -> {
                    if (stringResource.argTypes.isEmpty()) {
                        typeSpecBuilder.addProperty(
                            createSingularStringProperty(key, stringResource, isImplementation))
                    } else {
                        typeSpecBuilder.addFunction(
                            createSingularStringFunWithArgs(key, stringResource, isImplementation))
                    }
                }
                is StringResource.Plural -> {
                    typeSpecBuilder.addFunction(
                        createPluralStringFunWithArgs(key, stringResource, isImplementation))
                }
                is StringResource.Array -> {
                    typeSpecBuilder.addFunction(
                        createStringArrayFun(key, stringResource, isImplementation))
                }
            }
        }
        val typeSpec = typeSpecBuilder.build()
        fileSpecBuilder.addType(typeSpec)
        fileSpecBuilder.build().also { fileSpec ->
            fileSpec.writeTo(directory)
        }
    }

    private fun createSingularStringProperty(
        key: String,
        stringResource: StringResource.Singular,
        isImplementation: Boolean
    ): PropertySpec {
        val builder = PropertySpec.builder(key, String::class)
            .mutable(false)
        if (isImplementation) {
            builder
                .addModifiers(KModifier.OVERRIDE)
                .initializer("%S", stringResource.value)
        } else {
            builder.addModifiers(KModifier.ABSTRACT)
        }
        return builder.build()
    }

    private fun createSingularStringFunWithArgs(
        key: String,
        stringResource: StringResource.Singular,
        isImplementation: Boolean
    ): FunSpec {
        val args = args(stringResource.argTypes)
        val builder = FunSpec.builder(key)
            .apply {
                args.forEach { pair -> addParameter(pair.first, pair.second) }
            }
            .returns(String::class)
        if (isImplementation) {
            builder
                .addModifiers(KModifier.OVERRIDE)
                .addCode(CodeBlock.of("return %S.format(${args.joinToString(separator = ", ") { "%L" }})",
                    *(listOf(stringResource.value) + args.map { it.first }).toTypedArray()))
        } else {
            builder.addModifiers(KModifier.ABSTRACT)
        }
        return builder.build()
    }

    private fun createPluralStringFunWithArgs(
        key: String,
        stringResource: StringResource.Plural,
        isImplementation: Boolean
    ): FunSpec {
        val args = args(stringResource.argTypes)
        val builder = FunSpec.builder(key)
            .apply {
                addParameter("count", Int::class)
                if (!isImplementation && args.count() == 1 && args.first().second == Int::class) {
                    // If there is only one arg only and its type is Int, we set its default value to 'count'
                    val arg = args.first()
                    addParameter(
                        ParameterSpec.builder(arg.first, arg.second)
                            .defaultValue("count")
                            .build()
                    )
                } else {
                    args.forEach { pair -> addParameter(pair.first, pair.second) }
                }
            }
            .returns(String::class)
        if (isImplementation) {
            val codeBlock = buildCodeBlock {
                beginControlFlow("val value = when {")
                stringResource.zero?.also {
                    addStatement("count == 0 -> %S", it)
                }
                stringResource.one?.also {
                    addStatement("count == 1 -> %S", it)
                }
                stringResource.two?.also {
                    addStatement("count == 2 -> %S", it)
                }
                stringResource.few?.also {
                    addStatement("count in 3..5 -> %S", it)
                }
                stringResource.many?.also {
                    addStatement("count in 6..99 -> %S", it)
                }
                addStatement("else -> %S", stringResource.other.orEmpty())
                endControlFlow()
                addStatement("return value.format(" +
                        "${args.joinToString(separator = ", ") { it.first }})")
            }
            builder
                .addModifiers(KModifier.OVERRIDE)
                .addCode(codeBlock)
        } else {
            builder.addModifiers(KModifier.ABSTRACT)
        }
        return builder.build()
    }

    private fun createStringArrayFun(
        key: String,
        stringResource: StringResource.Array,
        isImplementation: Boolean
    ): FunSpec {
        val builder = FunSpec.builder(key)
            .returns(List::class.parameterizedBy(String::class))
        if (isImplementation) {
            val values = stringResource.values
            val codeBlock = buildCodeBlock {
                addStatement("val list = ArrayList<String>(${values.count()})")
                values.forEach { value ->
                    addStatement("list.add(%S)", value)
                }
                addStatement("return list")
            }
            builder
                .addModifiers(KModifier.OVERRIDE)
                .addCode(codeBlock)
        } else {
            builder.addModifiers(KModifier.ABSTRACT)
        }
        return builder.build()
    }

    private fun args(argTypes: List<ArgType>): List<Pair<String, KClass<*>>> {
        return argTypes.mapIndexed { index, argType ->
            "arg${index + 1}" to when (argType) {
                ArgType.DECIMAL -> Int::class
                ArgType.STRING -> String::class
            }
        }
    }

    private fun generateExtensions(
        directory: File,
        packageName: String
    ) {
        val funSpec = FunSpec.builder("format")
            .receiver(String::class)
            .returns(String::class)
            .addParameter(
                ParameterSpec.builder("args", Any::class.asTypeName().copy(true), KModifier.VARARG)
                    .build()
            )
            // TODO: create an in-house solution for string formatting
            .addCode(
                """
                    return kotlin.runCatching {
                        this.sprintf(*args)
                    }.getOrElse { this }
                """.trimIndent()
            )
            .build()
        FileSpec.builder(packageName, "StringExtensions")
            .addImport("net.sergeych.sprintf", "sprintf")
            .addFunction(funSpec)
            .build().also { fileSpec ->
                fileSpec.writeTo(directory)
            }
    }

    private fun generateUtils(
        directory: File,
        packageName: String,
        supportedLanguages: Set<String>
    ) {
        val utilsClassName = "${CLASSNAME_STRINGS}Utils"
        val fileSpecBuilder = FileSpec.builder(packageName, utilsClassName)
        val typeSpecBuilder = TypeSpec.objectBuilder(utilsClassName)
        typeSpecBuilder.addModifiers(KModifier.INTERNAL)
        typeSpecBuilder.addProperty(
            PropertySpec.builder("supportedLanguages", Set::class.parameterizedBy(String::class))
                .initializer(CodeBlock.of(
                    "setOf(" + supportedLanguages.joinToString(", ") { "\"$it\"" } + ")"
                ))
                .build()
        )
        supportedLanguages.forEach { langCode ->
            val propertyName = langCode.uppercase().replace("-", "_")
            val className = "${langCode}$CLASSNAME_STRINGS"
            typeSpecBuilder.addProperty(
                PropertySpec.builder(propertyName, ClassName(packageName, className))
                    .initializer(CodeBlock.of(className))
                    .build()
            )
        }
        val typeSpec = typeSpecBuilder.build()
        fileSpecBuilder.addType(typeSpec)
        fileSpecBuilder.build().also { fileSpec ->
            fileSpec.writeTo(directory)
        }
    }

    private companion object {
        private const val CLASSNAME_STRINGS = "Strings"
    }
}