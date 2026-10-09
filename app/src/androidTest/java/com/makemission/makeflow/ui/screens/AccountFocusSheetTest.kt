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
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountFocusSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val accounts = listOf(
        AccountEntity("cash", "Cash", 0.0, 0.0),
        AccountEntity("bank", "Bank with a longer account name", 0.0, 0.0)
    )

    @Test
    fun selectedAccountHasRadioSemanticsAndCanSwitchToAllAccounts() {
        var selection: String? = "bank"
        showSheet(onSelect = { selection = it })
        composeTestRule.onNodeWithText(accounts[1].name).assertIsSelected()
        composeTestRule.onNodeWithText("All accounts").performClick()
        composeTestRule.runOnIdle { assertEquals(null, selection) }
    }

    @Test
    fun longAccountNameIsReachableWithLargeText() {
        var selection: String? = null
        showSheet(fontScale = 2f, onSelect = { selection = it })
        composeTestRule.onNodeWithText(accounts[1].name).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertEquals("bank", selection) }
    }

    @Test
    fun closeDoesNotChangeAccount() {
        var dismissed = false
        var changed = false
        showSheet(onSelect = { changed = true }, onDismiss = { dismissed = true })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.runOnIdle { assertTrue(dismissed && !changed) }
    }

    private fun showSheet(fontScale: Float = 1f, onSelect: (String?) -> Unit = {}, onDismiss: () -> Unit = {}) {
        composeTestRule.setContent {
            MakeFlowTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    AccountFocusSheet(accounts, "bank", onSelect, onDismiss)
                }
            }
        }
    }
}
