package com.makemission.makeflow.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.SavingsGoalEntity
import org.junit.Rule
import org.junit.Test

class SavingsGoalAccountCreationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val accounts = listOf(
        AccountEntity(id = "a1", name = "Cash", balance = 500.0, initialBalance = 0.0),
        AccountEntity(id = "a2", name = "Bank", balance = 1000.0, initialBalance = 0.0)
    )

    @Test
    fun savingsGoalSheet_rendersWithoutCrash() {
        composeTestRule.setContent {
            AdaptiveSheetDialog(onDismiss = {}) {
                AddEditSavingsGoalBottomSheet(
                    existingGoal = null,
                    accounts = accounts,
                    currencyCode = "KES",
                    currencySymbol = "Ksh",
                    onDismiss = {},
                    onSave = { _, _, _, _ -> },
                    onAddAccount = {}
                )
            }
        }

        composeTestRule.onNodeWithText("New Goal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Goal name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("Linked account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create new account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Goal").assertIsDisplayed()
    }

    @Test
    fun savingsGoalSheet_showsExistingAccounts() {
        composeTestRule.setContent {
            AdaptiveSheetDialog(onDismiss = {}) {
                AddEditSavingsGoalBottomSheet(
                    existingGoal = null,
                    accounts = accounts,
                    currencyCode = "KES",
                    currencySymbol = "Ksh",
                    onDismiss = {},
                    onSave = { _, _, _, _ -> },
                    onAddAccount = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bank").assertIsDisplayed()
    }

    @Test
    fun accountSheet_rendersWithoutCrash() {
        composeTestRule.setContent {
            AdaptiveSheetDialog(onDismiss = {}) {
                AddEditAccountBottomSheet(
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                    currencySymbol = "Ksh"
                )
            }
        }

        composeTestRule.onNodeWithText("New Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Current balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Account").assertIsDisplayed()
    }

    @Test
    fun savingsGoalSheet_onAddAccount_triggersCallback() {
        var addAccountCalled = false

        composeTestRule.setContent {
            AdaptiveSheetDialog(onDismiss = {}) {
                AddEditSavingsGoalBottomSheet(
                    existingGoal = null,
                    accounts = accounts,
                    currencyCode = "KES",
                    currencySymbol = "Ksh",
                    onDismiss = {},
                    onSave = { _, _, _, _ -> },
                    onAddAccount = { addAccountCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Create new account").performClick()
        composeTestRule.waitForIdle()
        assert(addAccountCalled) { "onAddAccount callback should have been called" }
    }

    @Test
    fun savingsGoalSheet_editMode_rendersExistingGoal() {
        val existingGoal = SavingsGoalEntity(
            id = "g1",
            name = "Vacation Fund",
            targetAmount = 100000.0,
            linkedAccountId = "a1"
        )

        composeTestRule.setContent {
            AdaptiveSheetDialog(onDismiss = {}) {
                AddEditSavingsGoalBottomSheet(
                    existingGoal = existingGoal,
                    accounts = accounts,
                    currencyCode = "KES",
                    currencySymbol = "Ksh",
                    onDismiss = {},
                    onSave = { _, _, _, _ -> },
                    onAddAccount = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Goal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vacation Fund").assertIsDisplayed()
    }

    @Test
    fun accountSheet_editMode_rendersExistingAccount() {
        val existingAccount = AccountEntity(
            id = "a1",
            name = "Existing Account",
            balance = 2500.0,
            initialBalance = 0.0
        )

        composeTestRule.setContent {
            AdaptiveSheetDialog(onDismiss = {}) {
                AddEditAccountBottomSheet(
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                    account = existingAccount,
                    currencySymbol = "Ksh"
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Existing Account").assertIsDisplayed()
    }
}
