package com.makemission.makeflow.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolutionTest {

    // ── resolveDarkTheme ─────────────────────────────────────────────

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
    fun `system mode follows isSystemDark parameter`() {
        assertTrue(resolveDarkTheme("system", isSystemDark = true))
        assertFalse(resolveDarkTheme("system", isSystemDark = false))
    }

    @Test
    fun `unknown mode falls through to system behavior`() {
        assertTrue(resolveDarkTheme("unknown", isSystemDark = true))
        assertFalse(resolveDarkTheme("unknown", isSystemDark = false))
    }

    @Test
    fun `empty string falls through to system behavior`() {
        assertTrue(resolveDarkTheme("", isSystemDark = true))
        assertFalse(resolveDarkTheme("", isSystemDark = false))
    }

    // ── resolveColorScheme palette selection ──────────────────────────

    @Test
    fun `forest dark scheme has dark background`() {
        val scheme = resolveColorScheme("forest", darkTheme = true)
        assertEquals(MindfulDarkBackground, scheme.background)
        assertEquals(MindfulDarkText, scheme.onSurface)
        assertEquals(MindfulAccent, scheme.primary)
        assertEquals(MindfulLightText, scheme.onPrimary)
    }

    @Test
    fun `forest light scheme has light background`() {
        val scheme = resolveColorScheme("forest", darkTheme = false)
        assertEquals(MindfulLightBackground, scheme.background)
        assertEquals(MindfulLightText, scheme.onSurface)
        assertEquals(MindfulAccent, scheme.primary)
        assertEquals(MindfulLightText, scheme.onPrimary)
    }

    @Test
    fun `ocean dark scheme has dark background`() {
        val scheme = resolveColorScheme("ocean", darkTheme = true)
        assertEquals(Color(0xFF0F1316), scheme.background)
    }

    @Test
    fun `ocean light scheme has light background`() {
        val scheme = resolveColorScheme("ocean", darkTheme = false)
        assertEquals(Color(0xFFF8FAFC), scheme.background)
    }

    @Test
    fun `sunset dark scheme has dark background`() {
        val scheme = resolveColorScheme("sunset", darkTheme = true)
        assertEquals(Color(0xFF17120F), scheme.background)
    }

    @Test
    fun `sunset light scheme has light background`() {
        val scheme = resolveColorScheme("sunset", darkTheme = false)
        assertEquals(Color(0xFFFFF8F6), scheme.background)
    }

    @Test
    fun `purple dark scheme has dark background`() {
        val scheme = resolveColorScheme("purple", darkTheme = true)
        assertEquals(Color(0xFF141218), scheme.background)
    }

    @Test
    fun `purple light scheme has light background`() {
        val scheme = resolveColorScheme("purple", darkTheme = false)
        assertEquals(Color(0xFFFFFBFD), scheme.background)
    }

    @Test
    fun `unknown palette defaults to forest`() {
        val forest = resolveColorScheme("forest", darkTheme = true)
        val unknown = resolveColorScheme("bogus", darkTheme = true)
        assertEquals(forest.background, unknown.background)
        assertEquals(forest.onSurface, unknown.onSurface)
    }

    // ── applyAmoledOverlay ───────────────────────────────────────────

    @Test
    fun `amoled overlay forces background to pure black`() {
        val darkScheme = resolveColorScheme("forest", darkTheme = true)
        val amoled = darkScheme.applyAmoledOverlay()

        assertEquals(Color(0xFF000000), amoled.background)
        assertEquals(Color(0xFF000000), amoled.surface)
        assertEquals(Color(0xFF000000), amoled.surfaceDim)
        assertEquals(Color(0xFF000000), amoled.surfaceContainerLowest)
        assertEquals(Color(0xFF000000), amoled.surfaceContainerLow)
        assertEquals(Color(0xFF000000), amoled.surfaceContainer)
    }

    @Test
    fun `amoled overlay forces onSurface to white for max contrast`() {
        val darkScheme = resolveColorScheme("forest", darkTheme = true)
        val amoled = darkScheme.applyAmoledOverlay()

        assertEquals(Color(0xFFFFFFFF), amoled.onSurface)
        assertEquals(Color(0xFFFFFFFF), amoled.onBackground)
    }

    @Test
    fun `amoled overlay near-black containers for hierarchy`() {
        val darkScheme = resolveColorScheme("forest", darkTheme = true)
        val amoled = darkScheme.applyAmoledOverlay()

        assertEquals(Color(0xFF0D0D0D), amoled.surfaceContainerHigh)
        assertEquals(Color(0xFF0D0D0D), amoled.surfaceVariant)
        assertEquals(Color(0xFF141414), amoled.surfaceContainerHighest)
    }

    @Test
    fun `amoled overlay does not touch primary color`() {
        val darkScheme = resolveColorScheme("forest", darkTheme = true)
        val amoled = darkScheme.applyAmoledOverlay()

        assertEquals(darkScheme.primary, amoled.primary)
    }

    @Test
    fun `amoled overlay works across all palettes`() {
        for (palette in listOf("forest", "ocean", "sunset", "purple")) {
            val darkScheme = resolveColorScheme(palette, darkTheme = true)
            val amoled = darkScheme.applyAmoledOverlay()

            assertEquals(
                "[$palette] background should be pure black",
                Color(0xFF000000), amoled.background
            )
            assertEquals(
                "[$palette] onSurface should be pure white",
                Color(0xFFFFFFFF), amoled.onSurface
            )
        }
    }

    // ── Integration: amoled only applies when dark ───────────────────

    @Test
    fun `amoled mode ignored when theme is light`() {
        val lightScheme = resolveColorScheme("forest", darkTheme = false)
        val amoledLightScheme = lightScheme.applyAmoledOverlay()

        // Even if we call applyAmoledOverlay, the integration logic
        // (amoledMode && darkTheme) prevents it from being called on light.
        // But the function itself doesn't guard — the guard is in MakeFlowTheme.
        // This test documents that the light scheme's background is NOT black.
        assertNotEquals(Color(0xFF000000), lightScheme.background)
    }

    @Test
    fun `amoled guard prevents overlay on light theme`() {
        // Simulate the integration logic from MakeFlowTheme
        val darkTheme = resolveDarkTheme("light", isSystemDark = true)
        val baseScheme = resolveColorScheme("forest", darkTheme)
        val amoledMode = true

        val finalScheme = if (amoledMode && darkTheme) baseScheme.applyAmoledOverlay() else baseScheme

        // light mode: darkTheme=false, so amoled guard skips overlay
        assertFalse("darkTheme should be false for light mode", darkTheme)
        assertEquals(
            "Background must NOT be pure black when light mode",
            MindfulLightBackground,
            finalScheme.background
        )
    }

    @Test
    fun `amoled guard applies overlay on dark theme`() {
        val darkTheme = resolveDarkTheme("dark", isSystemDark = false)
        val baseScheme = resolveColorScheme("forest", darkTheme)
        val amoledMode = true

        val finalScheme = if (amoledMode && darkTheme) baseScheme.applyAmoledOverlay() else baseScheme

        assertTrue("darkTheme should be true for dark mode", darkTheme)
        assertEquals(
            "Background MUST be pure black when dark + amoled",
            Color(0xFF000000),
            finalScheme.background
        )
    }

    @Test
    fun `system dark with amoled applies overlay`() {
        val darkTheme = resolveDarkTheme("system", isSystemDark = true)
        val baseScheme = resolveColorScheme("ocean", darkTheme)
        val amoledMode = true

        val finalScheme = if (amoledMode && darkTheme) baseScheme.applyAmoledOverlay() else baseScheme

        assertTrue(darkTheme)
        assertEquals(Color(0xFF000000), finalScheme.background)
        assertEquals(Color(0xFFFFFFFF), finalScheme.onSurface)
    }

    @Test
    fun `system light with amoled does not apply overlay`() {
        val darkTheme = resolveDarkTheme("system", isSystemDark = false)
        val baseScheme = resolveColorScheme("ocean", darkTheme)
        val amoledMode = true

        val finalScheme = if (amoledMode && darkTheme) baseScheme.applyAmoledOverlay() else baseScheme

        assertFalse(darkTheme)
        assertEquals(Color(0xFFF8FAFC), finalScheme.background)
    }

    @Test
    fun `amoled off with dark theme does not apply overlay`() {
        val darkTheme = resolveDarkTheme("dark", isSystemDark = false)
        val baseScheme = resolveColorScheme("sunset", darkTheme)
        val amoledMode = false

        val finalScheme = if (amoledMode && darkTheme) baseScheme.applyAmoledOverlay() else baseScheme

        assertTrue(darkTheme)
        // Sunset dark background, not pure black
        assertEquals(Color(0xFF17120F), finalScheme.background)
    }
}
