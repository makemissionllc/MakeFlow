package com.makemission.makeflow.ui.components

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AdaptiveConfig(
    val horizontalPadding: Dp = 20.dp,
    val cardSpacing: Dp = 14.dp,
    val contentMaxWidth: Dp = Dp.Unspecified,
    val fabSize: Dp = 56.dp,
    val barHeight: Dp = 56.dp,
    val titleFontSize: TextUnit = 28.sp,
    val titlePaddingBottom: Dp = 12.dp,
    val navIconSize: Dp = 20.dp,
    val navCapsulePadding: Dp = 24.dp,
    val isTablet: Boolean = false
)

val LocalAdaptiveConfig = staticCompositionLocalOf { AdaptiveConfig() }

@Composable
fun rememberAdaptiveConfig(): AdaptiveConfig {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return if (widthDp >= 600) {
        AdaptiveConfig(
            horizontalPadding = 32.dp,
            cardSpacing = 20.dp,
            contentMaxWidth = 640.dp,
            fabSize = 64.dp,
            barHeight = 64.dp,
            titleFontSize = 32.sp,
            titlePaddingBottom = 16.dp,
            navIconSize = 22.dp,
            navCapsulePadding = 48.dp,
            isTablet = true
        )
    } else {
        AdaptiveConfig()
    }
}
