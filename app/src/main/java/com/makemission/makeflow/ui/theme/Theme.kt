package com.makemission.makeflow.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

@Immutable
data class MakeFlowColors(
    val colorSuccess: Color,
    val colorWarning: Color,
    val colorGold: Color,
    val warningBanner: Color,
    val onWarningBanner: Color
)

private val LightMakeFlowColors = MakeFlowColors(
    colorSuccess = LightSuccess,
    colorWarning = LightWarning,
    colorGold = LightGold,
    warningBanner = MindfulWarningBanner,
    onWarningBanner = MindfulLightText
)

private val DarkMakeFlowColors = MakeFlowColors(
    colorSuccess = DarkSuccess,
    colorWarning = DarkWarning,
    colorGold = DarkGold,
    warningBanner = DarkWarningBanner,
    onWarningBanner = DarkOnWarningBanner
)

val LocalMakeFlowColors = staticCompositionLocalOf { DarkMakeFlowColors }

private val MakeFlowDarkColorScheme = darkColorScheme(
    primary = DarkPrimaryExpressive,
    onPrimary = MindfulLightText,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondaryColor,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkGold,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = MindfulDarkBackground,
    onBackground = MindfulDarkText,
    surface = MindfulDarkBackground,
    onSurface = MindfulDarkText,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = MindfulDarkMutedText,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = MindfulLightText,
    inverseOnSurface = MindfulLightBackground,
    inversePrimary = DarkInversePrimary,
    surfaceDim = DarkSurfaceDim,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest
)

private val MakeFlowLightColorScheme = lightColorScheme(
    primary = LightPrimaryExpressive,
    onPrimary = MindfulLightText,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondaryColor,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightGold,
    onTertiary = LightOnSecondary,
    tertiaryContainer = MindfulWarningBanner,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = MindfulLightBackground,
    onBackground = MindfulLightText,
    surface = Color(0xFFF9F8EF),
    onSurface = MindfulLightText,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = MindfulLightMutedText,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = MindfulLightText,
    inverseOnSurface = MindfulLightBackground,
    inversePrimary = MindfulAccent,
    surfaceDim = LightSurfaceDim,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest
)

internal fun resolveDarkTheme(themeMode: String, isSystemDark: Boolean): Boolean = when (themeMode) {
    "light" -> false
    "dark" -> true
    else -> isSystemDark
}

internal fun resolveColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) MakeFlowDarkColorScheme else MakeFlowLightColorScheme

internal fun ColorScheme.applyAmoledOverlay(): ColorScheme = copy(
    background = AmoledBackground,
    onBackground = AmoledOnSurface,
    surface = AmoledBackground,
    onSurface = AmoledOnSurface,
    surfaceDim = AmoledBackground,
    surfaceContainerLowest = AmoledBackground,
    surfaceContainerLow = AmoledBackground,
    surfaceContainer = AmoledBackground,
    surfaceContainerHigh = AmoledContainerHigh,
    surfaceContainerHighest = AmoledContainerHighest,
    surfaceVariant = AmoledContainerHigh,
    onSurfaceVariant = AmoledOnSurfaceVariant,
    outline = AmoledOutline,
    outlineVariant = AmoledOutlineVariant,
    inverseSurface = AmoledInverseSurface,
    inverseOnSurface = AmoledInverseOnSurface
)

@Composable
fun MakeFlowTheme(
    themeMode: String = "system",
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(themeMode, isSystemInDarkTheme())

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val baseScheme = resolveColorScheme(darkTheme)
    val colorScheme = if (amoledMode && darkTheme) baseScheme.applyAmoledOverlay() else baseScheme
    val makeFlowColors = if (darkTheme) DarkMakeFlowColors else LightMakeFlowColors

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val dimens = when {
        screenWidth < 600 -> compactDimens
        screenWidth < 840 -> mediumDimens
        else -> expandedDimens
    }

    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalExpressiveMotion provides ExpressiveMotionSpec.Default,
        LocalMakeFlowColors provides makeFlowColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
