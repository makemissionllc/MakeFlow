package com.makemission.makeflow.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.domain.PastTransactionForSuggestion
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import org.junit.Rule
import org.junit.Test

class UsualAccountSuggestionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val accounts = listOf(
        AccountEntity("cash", "Cash", 100.0, 0.0, currencyCode = "KES"),
        AccountEntity("mpesa", "M-PESA", 200.0, 0.0, currencyCode = "KES")
    )
    private val categories = listOf(CategoryEntity("food", "Food", "EXPENSE"))

    @Test
    fun exactNoteOffersUsualAccountAndAppliesItOnlyWhenChosen() {
        composeTestRule.setContent {
            val context = LocalContext.current
            val haptics = remember(context) { HapticManager(context).apply { strength = HapticStrength.OFF } }
            MakeFlowTheme {
                AddTransactionScreen(
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> },
                    accounts = accounts,
                    categories = categories,
                    currencyCode = "KES",
                    hapticManager = haptics,
                    initialAccountId = "cash",
                    initialNote = "Coffee",
                    onGetPastTransactions = {
                        listOf(
                            PastTransactionForSuggestion("Coffee", "food", "mpesa"),
                            PastTransactionForSuggestion("coffee", "food", "mpesa")
                        )
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Use usual account: M-PESA").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cash · KES").assertIsDisplayed()

        composeTestRule.onNodeWithText("Use usual account: M-PESA").performClick()

        composeTestRule.onNodeWithText("M-PESA · KES").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Use usual account: M-PESA").assertDoesNotExist()
    }

    @Test
    fun transferEntryDoesNotOfferSingleAccountSuggestion() {
        composeTestRule.setContent {
            val context = LocalContext.current
            val haptics = remember(context) { HapticManager(context).apply { strength = HapticStrength.OFF } }
            MakeFlowTheme {
                AddTransactionScreen(
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> },
                    accounts = accounts,
                    categories = categories,
                    currencyCode = "KES",
                    hapticManager = haptics,
                    initialType = "TRANSFER",
                    initialNote = "Coffee",
                    onGetPastTransactions = {
                        listOf(PastTransactionForSuggestion("Coffee", "food", "mpesa"))
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Use usual account: M-PESA").assertDoesNotExist()
    }

    @Test
    fun switchingFromTransferToExpenseReplacesTransferChipsImmediately() {
        composeTestRule.setContent {
            val context = LocalContext.current
            val haptics = remember(context) { HapticManager(context).apply { strength = HapticStrength.OFF } }
            MakeFlowTheme {
                AddTransactionScreen(
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> },
                    accounts = accounts,
                    categories = categories,
                    currencyCode = "KES",
                    hapticManager = haptics,
                    initialType = "TRANSFER",
                    initialTransferFromId = "cash",
                    initialTransferToId = "mpesa"
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("From · Cash · KES").assertIsDisplayed()
        composeTestRule.onNodeWithText("To · M-PESA · KES").assertIsDisplayed()

        composeTestRule.onNodeWithTag("transaction_type_EXPENSE").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("From · Cash · KES").assertDoesNotExist()
        composeTestRule.onNodeWithText("To · M-PESA · KES").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_type_EXPENSE").assertIsSelected()
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cash · KES").assertIsDisplayed()
    }

    @Test
    fun quickAddTypeChoicesExposeSelectedRadioState() {
        composeTestRule.setContent {
            val context = LocalContext.current
            val haptics = remember(context) { HapticManager(context).apply { strength = HapticStrength.OFF } }
            MakeFlowTheme {
                AddTransactionBottomSheet(
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> },
                    accounts = accounts,
                    categories = categories,
                    currencyCode = "KES",
                    hapticManager = haptics
                )
            }
        }

        composeTestRule.onNodeWithText("Income").assertIsSelected()
        composeTestRule.onNodeWithText("Expense").performClick().assertIsSelected()
    }
}
