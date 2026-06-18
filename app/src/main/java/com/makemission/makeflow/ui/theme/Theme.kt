package com.makemission.makeflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration

private val DarkColorScheme = darkColorScheme(
    primary         = DarkPrimary,
    onPrimary       = DarkBackground,
    secondary       = DarkSecondary,
    onSecondary     = DarkPrimary,
    background      = DarkBackground,
    onBackground    = DarkPrimary,
    surface         = DarkSurface,
    onSurface       = DarkPrimary,
    surfaceVariant  = DarkSurface,
    onSurfaceVariant = DarkSecondary,
    outline         = DarkSecondary,
    outlineVariant  = DarkSecondary
)

private val LightColorScheme = lightColorScheme(
    primary         = LightPrimary,
    onPrimary       = LightSurface,
    secondary       = LightSecondary,
    onSecondary     = LightPrimary,
    background      = LightBackground,
    onBackground    = LightPrimary,
    surface         = LightSurface,
    onSurface       = LightPrimary,
    surfaceVariant  = LightSurface,
    onSurfaceVariant = LightSecondary,
    outline         = LightSecondary,
    outlineVariant  = LightSecondary
)

@Composable
fun MakeFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp
    val dimens = when {
        screenWidth < 600 -> compactDimens
        screenWidth < 840 -> mediumDimens
        else -> expandedDimens
    }
    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalExpressiveMotion provides ExpressiveMotionSpec.Default
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
