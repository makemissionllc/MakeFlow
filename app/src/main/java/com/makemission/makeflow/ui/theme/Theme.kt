package com.makemission.makeflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

// ── Forest Green (Default) Color Schemes ─────────────────────────────
private val ForestDarkColorScheme = darkColorScheme(
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

private val ForestLightColorScheme = lightColorScheme(
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

// ── Ocean Blue Color Schemes ─────────────────────────────────────────
private val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF63B3F4),
    onPrimary = Color(0xFF002A44),
    primaryContainer = Color(0xFF004B76),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = Color(0xFFB5C9DF),
    onSecondary = Color(0xFF203243),
    background = Color(0xFF0F1316),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF0F1316),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF242B30),
    onSurfaceVariant = Color(0xFFC0C7CE),
    outline = Color(0xFF8A9198),
    surfaceContainerLowest = Color(0xFF090D0F),
    surfaceContainerLow = Color(0xFF171B1F),
    surfaceContainer = Color(0xFF1C2024),
    surfaceContainerHigh = Color(0xFF262A2F),
    surfaceContainerHighest = Color(0xFF31353B)
)

private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF006399),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E32),
    secondary = Color(0xFF4E6072),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7ECEF),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF72777E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainer = Color(0xFFE2E8F0),
    surfaceContainerHigh = Color(0xFFCBD5E1),
    surfaceContainerHighest = Color(0xFF94A3B8)
)

// ── Sunset Orange Color Schemes ───────────────────────────────────────
private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB595),
    onPrimary = Color(0xFF561F00),
    primaryContainer = Color(0xFF7B3000),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFE7BDB0),
    onSecondary = Color(0xFF442A21),
    background = Color(0xFF17120F),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF17120F),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF2D2420),
    onSurfaceVariant = Color(0xFFD7C2BA),
    outline = Color(0xFF9F8D86),
    surfaceContainerLowest = Color(0xFF100C0A),
    surfaceContainerLow = Color(0xFF201A17),
    surfaceContainer = Color(0xFF241E1B),
    surfaceContainerHigh = Color(0xFF2F2824),
    surfaceContainerHighest = Color(0xFF3B322D)
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFFAB4700),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF381300),
    secondary = Color(0xFF77574E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF221A17),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF221A17),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433F),
    outline = Color(0xFF85736E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFEE5DB),
    surfaceContainerHigh = Color(0xFFFCD9CD),
    surfaceContainerHighest = Color(0xFFF7CBBF)
)

// ── Royal Purple Color Schemes ────────────────────────────────────────
private val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF26242C),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1C1A22),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B)
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBFD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFD),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

@Composable
fun MakeFlowTheme(
    themeMode: String = "dark",
    themePalette: String = "forest",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = if (themePalette == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        when (themePalette) {
            "ocean" -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
            "sunset" -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
            "purple" -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
            else -> if (darkTheme) ForestDarkColorScheme else ForestLightColorScheme
        }
    }

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
