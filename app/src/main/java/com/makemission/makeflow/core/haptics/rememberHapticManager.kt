package com.makemission.makeflow.core.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberHapticManager(strength: HapticStrength = HapticStrength.STANDARD): HapticManager {
    val context = LocalContext.current
    val manager = remember { HapticManager(context) }
    manager.strength = strength
    return manager
}
