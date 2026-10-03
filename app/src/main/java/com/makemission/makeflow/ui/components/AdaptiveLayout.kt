package com.makemission.makeflow.ui.components

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode

data class AdaptiveConfig(
    val horizontalPadding: Dp = 20.dp,
    val cardSpacing: Dp = 14.dp,
    val contentMaxWidth: Dp = Dp.Unspecified,
    val fabSize: Dp = 56.dp,
    val barHeight: Dp = 56.dp,
    val titleFontSize: TextUnit = 32.sp,
    val titlePaddingBottom: Dp = 12.dp,
    val navIconSize: Dp = 20.dp,
    val navCapsulePadding: Dp = 24.dp,
    val fadingEdgeHeight: Dp = 80.dp,
    val isTablet: Boolean = false,
    val isLandscape: Boolean = false,
    val layoutMode: DeviceLayoutMode = DeviceLayoutMode.COMPACT
)

val LocalAdaptiveConfig = staticCompositionLocalOf { AdaptiveConfig() }

@Composable
fun rememberAdaptiveConfig(): AdaptiveConfig {
    val layoutMode = LocalDeviceLayoutMode.current
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    return when (layoutMode) {
        DeviceLayoutMode.EXPANDED -> AdaptiveConfig(
            horizontalPadding = 48.dp,
            cardSpacing = 24.dp,
            contentMaxWidth = Dp.Unspecified,
            fabSize = 64.dp,
            barHeight = 64.dp,
            titleFontSize = 34.sp,
            titlePaddingBottom = 16.dp,
            navIconSize = 22.dp,
            navCapsulePadding = 48.dp,
            fadingEdgeHeight = 120.dp,
            isTablet = true,
            isLandscape = isLandscape,
            layoutMode = layoutMode
        )
        DeviceLayoutMode.MEDIUM -> AdaptiveConfig(
            horizontalPadding = 32.dp,
            cardSpacing = 20.dp,
            contentMaxWidth = 640.dp,
            fabSize = 64.dp,
            barHeight = 64.dp,
            titleFontSize = 32.sp,
            titlePaddingBottom = 16.dp,
            navIconSize = 22.dp,
            navCapsulePadding = 48.dp,
            isTablet = true,
            isLandscape = isLandscape,
            layoutMode = layoutMode
        )
        DeviceLayoutMode.COMPACT -> AdaptiveConfig(
            isLandscape = isLandscape,
            layoutMode = layoutMode
        )
    }
}
