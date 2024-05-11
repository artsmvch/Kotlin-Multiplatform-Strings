package com.strings.sample

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSLocale
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

actual fun getSystemLangCodeStateFlow(): StateFlow<String?> {
    val flow = notificationCenterFlow(
        notificationName = NSCurrentLocaleDidChangeNotification,
        onGetValue = ::getSystemLangCode
    )
    val stateFlow = MutableStateFlow<String?>(getSystemLangCode())
    GlobalScope.launch {
        flow.collect(stateFlow)
    }
    return stateFlow
}

private fun getSystemLangCode(): String? {
    return NSLocale.preferredLanguages.firstOrNull()?.toString()
        ?: NSLocale.currentLocale.languageCode
}

internal fun <T> notificationCenterFlow(
    notificationName: NSNotificationName,
    onGetValue: () -> T
): Flow<T> {
    return callbackFlow {
        val block: (NSNotification?) -> Unit = { _: NSNotification? ->
            channel.trySend(onGetValue())
        }
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = notificationName,
            `object` = null,
            queue = null,
            usingBlock = block
        )
        channel.send(onGetValue())
        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
}