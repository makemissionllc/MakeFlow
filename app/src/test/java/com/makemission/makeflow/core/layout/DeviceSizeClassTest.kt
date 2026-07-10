package com.makemission.makeflow.core.layout

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceSizeClassTest {

    @Test
    fun compact_width_maps_to_compact_mode() {
        val mode = mapWidthToLayoutMode(WindowWidthSizeClass.Compact)
        assertEquals(DeviceLayoutMode.COMPACT, mode)
    }

    @Test
    fun medium_width_maps_to_medium_mode() {
        val mode = mapWidthToLayoutMode(WindowWidthSizeClass.Medium)
        assertEquals(DeviceLayoutMode.MEDIUM, mode)
    }

    @Test
    fun expanded_width_maps_to_expanded_mode() {
        val mode = mapWidthToLayoutMode(WindowWidthSizeClass.Expanded)
        assertEquals(DeviceLayoutMode.EXPANDED, mode)
    }
}
