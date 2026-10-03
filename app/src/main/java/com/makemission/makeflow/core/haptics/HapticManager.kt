package com.makemission.makeflow.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.makemission.makeflow.core.device.PowerStateProvider

enum class HapticEvent(val effect: VibrationEffect) {
    TAB_SWITCH(VibrationEffect.createOneShot(10, 40)),
    NAV_SELECT(VibrationEffect.createOneShot(8, 30)),
    TRANSACTION_SAVED(VibrationEffect.createOneShot(20, 60)),
    TRANSACTION_DELETED(VibrationEffect.createWaveform(longArrayOf(0, 15, 10, 15), -1)),
    DELETE_UNDO_AVAILABLE(VibrationEffect.createOneShot(15, 50)),
    VALIDATION_ERROR(VibrationEffect.createWaveform(longArrayOf(0, 10, 20, 10), -1)),
    SWIPE_ACTION_ARMED(VibrationEffect.createOneShot(12, 45)),
    BUDGET_WARNING(VibrationEffect.createWaveform(longArrayOf(0, 20, 30, 20), -1)),
    BUDGET_EXCEEDED(VibrationEffect.createWaveform(longArrayOf(0, 30, 20, 30, 20, 30), -1)),
    ONBOARDING_STEP_COMPLETE(VibrationEffect.createOneShot(15, 55)),
    TOGGLE(VibrationEffect.createOneShot(6, 25)),
    ANOMALY_DETECTED(VibrationEffect.createWaveform(longArrayOf(0, 25, 15, 25, 15, 25), -1)),
}

private val SIMPLE_SHORT_PULSE = VibrationEffect.createOneShot(10, 40)

fun isLowPriorityEvent(event: HapticEvent): Boolean = when (event) {
    HapticEvent.TAB_SWITCH,
    HapticEvent.NAV_SELECT,
    HapticEvent.TOGGLE -> true
    else -> false
}

class HapticManager(context: Context) {
    @Volatile
    var isEnabled: Boolean = true

    private val powerStateProvider = PowerStateProvider(context)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun fire(event: HapticEvent) {
        if (!isEnabled) return

        val powerState = powerStateProvider.getCurrentState()
        if (powerState.shouldReduceMotion) {
            if (isLowPriorityEvent(event)) return
            vibrator.vibrate(SIMPLE_SHORT_PULSE)
        } else {
            vibrator.vibrate(event.effect)
        }
    }
}
