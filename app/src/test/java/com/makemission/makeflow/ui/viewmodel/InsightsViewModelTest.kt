package com.makemission.makeflow.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsViewModelTest {

    @Test
    fun `pctChange returns null when prior is zero`() {
        assertNull(pctChange(324.0, 0.0))
        assertNull(pctChange(1000.0, 0.0))
        assertNull(pctChange(0.0, 0.0))
    }

    @Test
    fun `pctChange returns positive percentage when current exceeds prior`() {
        assertEquals(50.0, pctChange(150.0, 100.0)!!, 1e-9)
        assertEquals(100.0, pctChange(200.0, 100.0)!!, 1e-9)
    }

    @Test
    fun `pctChange returns negative percentage when current is less than prior`() {
        assertEquals(-50.0, pctChange(50.0, 100.0)!!, 1e-9)
        assertEquals(-100.0, pctChange(0.0, 100.0)!!, 1e-9)
    }

    @Test
    fun `pctChange returns zero when current equals prior`() {
        assertEquals(0.0, pctChange(100.0, 100.0)!!, 1e-9)
    }
}
