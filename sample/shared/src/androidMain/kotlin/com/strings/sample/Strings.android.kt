package com.strings.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.os.ConfigurationCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

actual fun getSystemLangCodeStateFlow(): StateFlow<String?> {
    val flow = broadcastReceiverFlow(
        intentAction = Intent.ACTION_LOCALE_CHANGED,
        onGetValue = ::getSystemLangCode
    )
    val stateFlow = MutableStateFlow<String?>(getSystemLangCode())
    GlobalScope.launch {
        flow.collect(stateFlow)
    }
    return stateFlow
}

private fun getSystemLangCode(): String? {
    val resources = ContentProviderImpl.instance.context!!.resources
    val localeList = ConfigurationCompat.getLocales(resources.configuration)
    return if (!localeList.isEmpty) localeList[0]?.language else null
}

private fun <T> broadcastReceiverFlow(
    intentAction: String,
    onGetValue: () -> T
): Flow<T> {
    val applicationContext = ContentProviderImpl.instance.context!!.applicationContext
    return callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == intentAction) {
                    channel.trySend(onGetValue())
                }
            }
        }
        applicationContext.registerReceiver(receiver,
            IntentFilter().apply { addAction(intentAction) }
        )
        channel.send(onGetValue())
        awaitClose {
            applicationContext.unregisterReceiver(receiver)
        }
    }
}