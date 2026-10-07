package com.makemission.makeflow.core.haptics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticStrengthTest {
    @Test
    fun fromStorage_restoresEveryStrengthAndDefaultsUnknownValues() {
        assertEquals(HapticStrength.OFF, HapticStrength.fromStorage("off"))
        assertEquals(HapticStrength.SOFT, HapticStrength.fromStorage("soft"))
        assertEquals(HapticStrength.STANDARD, HapticStrength.fromStorage("standard"))
        assertEquals(HapticStrength.STRONG, HapticStrength.fromStorage("strong"))
        assertEquals(HapticStrength.STANDARD, HapticStrength.fromStorage("unrecognized"))
        assertEquals(HapticStrength.STANDARD, HapticStrength.fromStorage(null))
    }

    @Test
    fun strengthsIncreaseWithOffAsTheOnlyDisabledLevel() {
        assertEquals(0f, HapticStrength.OFF.amplitudeScale, 0f)
        assertTrue(HapticStrength.SOFT.amplitudeScale < HapticStrength.STANDARD.amplitudeScale)
        assertTrue(HapticStrength.STANDARD.amplitudeScale < HapticStrength.STRONG.amplitudeScale)
        assertTrue(HapticStrength.SOFT.durationScale < HapticStrength.STRONG.durationScale)
    }
}
