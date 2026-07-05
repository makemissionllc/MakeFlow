package com.makemission.makeflow.core.layout

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class DeviceLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberDeviceLayoutMode(windowSizeClass: WindowSizeClass): DeviceLayoutMode {
    return remember(windowSizeClass) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> DeviceLayoutMode.COMPACT
            WindowWidthSizeClass.Medium -> DeviceLayoutMode.MEDIUM
            WindowWidthSizeClass.Expanded -> DeviceLayoutMode.EXPANDED
            else -> DeviceLayoutMode.COMPACT
        }
    }
}
