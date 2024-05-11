package com.artsmvch.strings

import java.io.File

abstract class StringsExtension {
    var resourcesDir: File? = null
    var packageName: String? = null
    var supportedLanguages: Set<String>? = null
    var missingTranslationStrategy: MissingTranslationStrategy =
        MissingTranslationStrategy.USE_DEFAULT
}