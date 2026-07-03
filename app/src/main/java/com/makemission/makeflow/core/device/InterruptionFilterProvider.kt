package com.makemission.makeflow.core.device

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class InterruptionState { NORMAL, PRIORITY_ONLY, ALARMS_ONLY, TOTAL_SILENCE }

open class InterruptionFilterProvider(private val context: Context?) {
    open fun getCurrentState(): InterruptionState {
        val nm = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return InterruptionState.NORMAL
        return when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> InterruptionState.NORMAL
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> InterruptionState.PRIORITY_ONLY
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> InterruptionState.ALARMS_ONLY
            NotificationManager.INTERRUPTION_FILTER_NONE -> InterruptionState.TOTAL_SILENCE
            else -> InterruptionState.NORMAL
        }
    }

    open fun observeState(): Flow<InterruptionState> = callbackFlow {
        trySend(getCurrentState())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(getCurrentState())
            }
        }
        context?.registerReceiver(
            receiver,
            IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        )
        awaitClose { context?.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
