package com.makemission.makeflow.core.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberHapticManager(): HapticManager {
    val context = LocalContext.current
    return remember { HapticManager(context) }
}
