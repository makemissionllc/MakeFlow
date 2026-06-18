package com.makemission.makeflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryExpressive,
    onPrimary = Color(0xFF00210A),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFFB4CCBB),
    onSecondary = Color(0xFF1E3527),
    secondaryContainer = Color(0xFF364B3E),
    onSecondaryContainer = Color(0xFFD0E9D8),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF033544),
    tertiaryContainer = Color(0xFF224C59),
    onTertiaryContainer = Color(0xFFBFE9F7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111411),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF111411),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFFC1C8BE),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414840),
    inverseSurface = Color(0xFFE1E3DE),
    inverseOnSurface = Color(0xFF191C1A),
    inversePrimary = Color(0xFF1B6B2F),
    surfaceDim = DarkSurfaceDim,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryExpressive,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF4D6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E9D8),
    onSecondaryContainer = Color(0xFF0A1F14),
    tertiary = Color(0xFF3C6472),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F7),
    onTertiaryContainer = Color(0xFF001F29),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF717870),
    outlineVariant = Color(0xFFC1C8BE),
    inverseSurface = Color(0xFF2D312E),
    inverseOnSurface = Color(0xFFE1E3DE),
    inversePrimary = Color(0xFF6DD881),
    surfaceDim = LightSurfaceDim,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
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
            shapes = ExpressiveShapes,
            content = content
        )
    }
}
