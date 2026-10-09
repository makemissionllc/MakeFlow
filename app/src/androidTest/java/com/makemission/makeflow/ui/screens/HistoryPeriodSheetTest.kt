package com.makemission.makeflow.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.domain.HistoryPeriod
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryPeriodSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = ZonedDateTime.parse("2026-10-09T12:00:00+03:00[Africa/Nairobi]")

    @Test
    fun sheetShowsEveryPresetAndSelectedPeriod() {
        showSheet(selectedPeriod = HistoryPeriod.THIS_MONTH)
        listOf("All time", "Today", "Last 7 days", "This month", "Last month").forEach {
            composeTestRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("This month").assertIsSelected()
    }

    @Test
    fun presetSelectionWorksWithReducedMotion() {
        var selected: HistoryPeriod? = null
        showSheet(reducedMotion = true, onSelect = { selected = it })
        composeTestRule.onNodeWithText("Last month").performScrollTo().performClick()
        composeTestRule.runOnIdle { assertEquals(HistoryPeriod.LAST_MONTH, selected) }
    }

    @Test
    fun allPresetsRemainReachableWithLargeText() {
        showSheet(fontScale = 2f)
        listOf("All time", "Today", "Last 7 days", "This month", "Last month").forEach {
            composeTestRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun closeDismissesWithoutSelectingAPreset() {
        var dismissed = false
        var selected: HistoryPeriod? = null
        showSheet(onSelect = { selected = it }, onDismiss = { dismissed = true })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.runOnIdle {
            assertTrue(dismissed)
            assertEquals(null, selected)
        }
    }

    private fun showSheet(
        selectedPeriod: HistoryPeriod = HistoryPeriod.ALL_TIME,
        reducedMotion: Boolean = false,
        fontScale: Float = 1f,
        onSelect: (HistoryPeriod) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MakeFlowTheme {
                CompositionLocalProvider(
                    LocalReducedMotion provides reducedMotion,
                    LocalDensity provides Density(LocalDensity.current.density, fontScale)
                ) {
                    HistoryPeriodSheet(selectedPeriod, now, onSelect, onDismiss)
                }
            }
        }
    }
}
