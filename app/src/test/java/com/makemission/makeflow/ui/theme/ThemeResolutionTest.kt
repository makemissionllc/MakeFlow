package com.makemission.makeflow.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolutionTest {

    @Test
    fun `light mode always resolves to light regardless of system state`() {
        assertFalse(resolveDarkTheme("light", isSystemDark = true))
        assertFalse(resolveDarkTheme("light", isSystemDark = false))
    }

    @Test
    fun `dark mode always resolves to dark regardless of system state`() {
        assertTrue(resolveDarkTheme("dark", isSystemDark = true))
        assertTrue(resolveDarkTheme("dark", isSystemDark = false))
    }

    @Test
    fun `system and unknown modes follow system appearance`() {
        assertTrue(resolveDarkTheme("system", isSystemDark = true))
        assertFalse(resolveDarkTheme("system", isSystemDark = false))
        assertTrue(resolveDarkTheme("unknown", isSystemDark = true))
        assertFalse(resolveDarkTheme("", isSystemDark = false))
    }

    @Test
    fun `brand accent is fixed across light and dark themes with readable ink`() {
        val light = resolveColorScheme(darkTheme = false)
        val dark = resolveColorScheme(darkTheme = true)

        assertEquals(MindfulAccent, light.primary)
        assertEquals(light.primary, dark.primary)
        assertEquals(MindfulLightText, light.onPrimary)
        assertEquals(MindfulLightText, dark.onPrimary)
        assertTrue(contrastRatio(light.onPrimary, light.primary) >= 4.5)
    }

    @Test
    fun `light theme uses warm ivory and sage surfaces`() {
        val scheme = resolveColorScheme(darkTheme = false)

        assertEquals(MindfulLightBackground, scheme.background)
        assertEquals(MindfulLightText, scheme.onSurface)
        assertNotEquals(Color(0xFFFFFFFF), scheme.background)
        assertEquals(Color(0xFFDCE7D5), scheme.secondaryContainer)
    }

    @Test
    fun `dark theme keeps warm contrast and the same brand accent`() {
        val scheme = resolveColorScheme(darkTheme = true)

        assertEquals(MindfulDarkBackground, scheme.background)
        assertEquals(MindfulDarkText, scheme.onSurface)
        assertEquals(MindfulAccent, scheme.primary)
    }

    @Test
    fun `amoled overlay uses true black surfaces while preserving brand color`() {
        val darkScheme = resolveColorScheme(darkTheme = true)
        val amoled = darkScheme.applyAmoledOverlay()

        assertEquals(Color(0xFF000000), amoled.background)
        assertEquals(Color(0xFF000000), amoled.surface)
        assertEquals(Color(0xFF000000), amoled.surfaceDim)
        assertEquals(Color(0xFF000000), amoled.surfaceContainerLowest)
        assertEquals(Color(0xFF000000), amoled.surfaceContainerLow)
        assertEquals(Color(0xFF000000), amoled.surfaceContainer)
        assertEquals(Color(0xFFF3F4EA), amoled.onSurface)
        assertEquals(darkScheme.primary, amoled.primary)
        assertEquals(Color(0xFF10130E), amoled.surfaceContainerHigh)
        assertEquals(Color(0xFF191D16), amoled.surfaceContainerHighest)
    }

    @Test
    fun `amoled overlay only applies when dark mode is active`() {
        val lightMode = resolveDarkTheme("light", isSystemDark = true)
        val lightScheme = resolveColorScheme(lightMode)
        val lightResult = if (lightMode) lightScheme.applyAmoledOverlay() else lightScheme

        assertFalse(lightMode)
        assertEquals(MindfulLightBackground, lightResult.background)

        val darkMode = resolveDarkTheme("system", isSystemDark = true)
        val darkScheme = resolveColorScheme(darkMode)
        val darkResult = if (darkMode) darkScheme.applyAmoledOverlay() else darkScheme

        assertTrue(darkMode)
        assertEquals(Color(0xFF000000), darkResult.background)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val luminances = listOf(foreground, background).map { color ->
            val channels = listOf(color.red, color.green, color.blue).map { channel ->
                if (channel <= 0.04045f) channel / 12.92f else Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
            }
            (0.2126f * channels[0] + 0.7152f * channels[1] + 0.0722f * channels[2]).toDouble()
        }
        val lighter = luminances.maxOrNull() ?: 0.0
        val darker = luminances.minOrNull() ?: 0.0
        return (lighter + 0.05) / (darker + 0.05)
    }
}
