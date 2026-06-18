package com.makemission.makeflow.ui.haptic

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

fun HapticFeedback.lightTick() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

fun HapticFeedback.mediumConfirm() {
    performHapticFeedback(HapticFeedbackType.Confirm)
}

fun HapticFeedback.strongPress() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}
