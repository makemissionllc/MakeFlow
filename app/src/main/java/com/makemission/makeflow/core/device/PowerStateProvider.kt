package com.makemission.makeflow.core.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

data class PowerState(
    val isPowerSaveMode: Boolean,
    val batteryPercent: Int,
    val shouldReduceMotion: Boolean
)

class PowerStateProvider(private val context: Context) {
    fun getCurrentState(): PowerState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isPowerSave = powerManager.isPowerSaveMode
        return PowerState(
            isPowerSaveMode = isPowerSave,
            batteryPercent = batteryPct,
            shouldReduceMotion = isPowerSave || batteryPct < 15
        )
    }

    @OptIn(FlowPreview::class)
    fun observeState(): Flow<PowerState> = callbackFlow {
        trySend(getCurrentState())
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(getCurrentState())
            }
        }
        context.registerReceiver(receiver, filter)
        awaitClose { context.unregisterReceiver(receiver) }
    }.debounce(2000).distinctUntilChanged()
}

@Composable
fun rememberPowerState(): State<PowerState> {
    val context = LocalContext.current
    val provider = remember { PowerStateProvider(context) }
    return provider.observeState().collectAsState(initial = provider.getCurrentState())
}
