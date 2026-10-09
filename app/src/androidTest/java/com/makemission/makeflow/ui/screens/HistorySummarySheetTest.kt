package com.makemission.makeflow.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.domain.HistorySummary
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import java.math.BigDecimal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistorySummarySheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val summary = HistorySummary.Result(
        transactionCount = 3,
        currencies = listOf(HistorySummary.CurrencyTotals("USD", BigDecimal("100"), BigDecimal("35")))
    )

    @Test
    fun summaryShowsScopeSearchCountAndExactAmounts() {
        showSheet()
        listOf("Today / Spending / Daily wallet", "Search: coffee", "3 transactions", "USD", "Income", "Spending", "Net change")
            .forEach { composeTestRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        listOf("100", "35", "65").forEach {
            composeTestRule.onNodeWithText(CurrencyFormatter.format(BigDecimal(it), "USD"))
                .performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun transferOnlyResultsDoNotShowMisleadingTotals() {
        showSheet(summary = HistorySummary.Result(transactionCount = 1, transferCount = 1))
        composeTestRule.onNodeWithText("1 transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 transfer excluded from income and spending.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Net change").assertDoesNotExist()
    }

    @Test
    fun missingCurrencyWarningRemainsReachableWithLargeTextAndReducedMotion() {
        showSheet(
            summary = summary.copy(excludedCount = 2, transferCount = 1),
            fontScale = 2f,
            reducedMotion = true,
            layoutMode = DeviceLayoutMode.EXPANDED
        )
        composeTestRule.onNodeWithText("Net change").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2 transactions omitted from totals: account, currency, type, or amount unavailable.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun openSheetUpdatesWhenResultsChange() {
        val state = mutableStateOf(summary)
        composeTestRule.setContent {
            MakeFlowTheme {
                HistorySummarySheet(state.value, "Today", "", {})
            }
        }
        composeTestRule.runOnIdle { state.value = HistorySummary.Result() }
        composeTestRule.onNodeWithText("0 transactions").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("No matches for the current filters").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("USD").assertDoesNotExist()
    }

    @Test
    fun closeDismissesSummary() {
        var dismissed = false
        showSheet(onDismiss = { dismissed = true }, layoutMode = DeviceLayoutMode.MEDIUM)
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.runOnIdle { assertTrue(dismissed) }
    }

    private fun showSheet(
        summary: HistorySummary.Result = this.summary,
        fontScale: Float = 1f,
        reducedMotion: Boolean = false,
        layoutMode: DeviceLayoutMode = DeviceLayoutMode.COMPACT,
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MakeFlowTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                    LocalReducedMotion provides reducedMotion,
                    LocalDeviceLayoutMode provides layoutMode
                ) {
                    HistorySummarySheet(summary, "Today / Spending / Daily wallet", "coffee", onDismiss)
                }
            }
        }
    }
}
