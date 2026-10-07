package com.makemission.makeflow.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NanoMonthlyReflectionTest {
    @Test
    fun stripsSimpleTextCodeFence() {
        assertEquals(
            "You recorded a steady month.",
            NanoMonthlyReflection.normalize("```text\nYou recorded a steady month.\n```")
        )
    }

    @Test
    fun boundsGeneratedTextAndRejectsEmptyOutput() {
        val bounded = NanoMonthlyReflection.normalize("A grounded reflection. ".repeat(40))

        assertTrue((bounded?.length ?: Int.MAX_VALUE) <= 400)
        assertNull(NanoMonthlyReflection.normalize("  "))
        assertNull(NanoMonthlyReflection.normalize("Too short"))
    }
}
