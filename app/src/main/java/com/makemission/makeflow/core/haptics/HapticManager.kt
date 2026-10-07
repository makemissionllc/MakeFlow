package com.makemission.makeflow.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.makemission.makeflow.core.device.PowerStateProvider

enum class HapticEvent(val timings: LongArray, val baseAmplitude: Int) {
    TAB_SWITCH(longArrayOf(0, 10), 40),
    NAV_SELECT(longArrayOf(0, 8), 30),
    TRANSACTION_SAVED(longArrayOf(0, 20), 60),
    TRANSACTION_DELETED(longArrayOf(0, 15, 10, 15), 70),
    DELETE_UNDO_AVAILABLE(longArrayOf(0, 15), 50),
    VALIDATION_ERROR(longArrayOf(0, 10, 20, 10), 60),
    SWIPE_ACTION_ARMED(longArrayOf(0, 12), 45),
    BUDGET_WARNING(longArrayOf(0, 20, 30, 20), 70),
    BUDGET_EXCEEDED(longArrayOf(0, 30, 20, 30, 20, 30), 80),
    ONBOARDING_STEP_COMPLETE(longArrayOf(0, 15), 55),
    TOGGLE(longArrayOf(0, 6), 25),
    ANOMALY_DETECTED(longArrayOf(0, 25, 15, 25, 15, 25), 85),
}

fun isLowPriorityEvent(event: HapticEvent): Boolean = when (event) {
    HapticEvent.TAB_SWITCH,
    HapticEvent.NAV_SELECT,
    HapticEvent.TOGGLE -> true
    else -> false
}

class HapticManager(context: Context) {
    @Volatile
    var strength: HapticStrength = HapticStrength.STANDARD

    private val powerStateProvider = PowerStateProvider(context)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun fire(event: HapticEvent) {
        val selectedStrength = strength
        if (selectedStrength == HapticStrength.OFF || !vibrator.hasVibrator()) return

        val powerState = powerStateProvider.getCurrentState()
        if (powerState.shouldReduceMotion) {
            if (isLowPriorityEvent(event)) return
        }

        val timings = if (powerState.shouldReduceMotion) longArrayOf(0, 10) else event.timings
        val amplitude = (event.baseAmplitude * selectedStrength.amplitudeScale)
            .toInt()
            .coerceIn(1, 255)
        val effect = if (vibrator.hasAmplitudeControl()) {
            if (timings.size == 2) {
                VibrationEffect.createOneShot(timings[1], amplitude)
            } else {
                val amplitudes = IntArray(timings.size) { index ->
                    if (index % 2 == 0) 0 else amplitude
                }
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            }
        } else {
            val adjustedTimings = timings.mapIndexed { index, duration ->
                if (index == 0 || duration == 0L) duration
                else (duration * selectedStrength.durationScale).toLong().coerceAtLeast(1)
            }.toLongArray()
            if (adjustedTimings.size == 2) {
                VibrationEffect.createOneShot(adjustedTimings[1], VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createWaveform(adjustedTimings, -1)
            }
        }
        vibrator.vibrate(effect)
    }
}
