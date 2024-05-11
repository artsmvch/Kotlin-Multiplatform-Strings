package com.strings.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

private const val LANG_EN = "en"
private const val LANG_ES = "es"
private const val LANG_PL = "pl"

expect fun getSystemLangCodeStateFlow(): StateFlow<String?>

@Composable
internal fun strings(): Strings {
    val lang = getSystemLangCodeStateFlow().collectAsState().value
    return when(lang) {
        LANG_EN -> enStrings
        LANG_ES -> esStrings
        LANG_PL -> plStrings
        else -> enStrings
    }
}