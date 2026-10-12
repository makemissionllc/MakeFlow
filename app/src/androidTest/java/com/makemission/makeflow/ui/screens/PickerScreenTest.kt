package com.makemission.makeflow.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.core.format.CurrencyFormatter
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class PickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val accounts = listOf(
        AccountEntity(id = "a1", name = "Cash", balance = 500.0, initialBalance = 0.0),
        AccountEntity(id = "a2", name = "Bank", balance = 1000.0, initialBalance = 0.0, currencyCode = "USD"),
        AccountEntity(id = "a3", name = "M-Pesa", balance = 200.0, initialBalance = 0.0)
    )

    private val parentCategories = listOf(
        CategoryEntity(id = "p1", name = "Food", type = "EXPENSE"),
        CategoryEntity(id = "p2", name = "Transport", type = "EXPENSE"),
        CategoryEntity(id = "p3", name = "Salary", type = "INCOME")
    )

    private val childCategories = listOf(
        CategoryEntity(id = "c1", name = "Groceries", type = "EXPENSE", parentCategoryId = "p1"),
        CategoryEntity(id = "c2", name = "Dining Out", type = "EXPENSE", parentCategoryId = "p1"),
        CategoryEntity(id = "c3", name = "Fuel", type = "EXPENSE", parentCategoryId = "p2")
    )

    private val incomeCategories = parentCategories.filter { it.type == "INCOME" }
    private val expenseCategories = parentCategories.filter { it.type == "EXPENSE" }
    private val allExpenseCategories = expenseCategories + childCategories

    @Test
    fun accountPicker_rendersAllAccounts() {
        composeTestRule.setContent {
            AccountPickerScreen(
                accounts = accounts,
                selectedAccountId = "a1",
                currencyCode = "KES",
                onAccountSelected = {},
                onBack = {}
            )
        }

        accounts.forEach { account ->
            composeTestRule.onNodeWithText(account.name).assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("account_picker_a1").assertIsSelected()
    }

    @Test
    fun accountPicker_showsBalances() {
        composeTestRule.setContent {
            AccountPickerScreen(
                accounts = accounts,
                selectedAccountId = "a1",
                currencyCode = "KES",
                onAccountSelected = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText(CurrencyFormatter.format(500.0, "KES")).assertIsDisplayed()
        composeTestRule.onNodeWithText(CurrencyFormatter.format(1000.0, "USD")).assertIsDisplayed()
        composeTestRule.onNodeWithText(CurrencyFormatter.format(200.0, "KES")).assertIsDisplayed()
    }

    @Test
    fun categoryPicker_showsExpenseParentsAndChildren_groupedByHierarchy() {
        composeTestRule.setContent {
            CategoryPickerScreen(
                selectedType = "EXPENSE",
                categories = allExpenseCategories,
                selectedCategoryId = null,
                accounts = accounts,
                selectedAccountId = null,
                currencyCode = "KES",
                onItemSelected = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transport").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dining Out").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fuel").assertIsDisplayed()
    }

    @Test
    fun categoryPicker_whenTransferType_showsAccounts() {
        composeTestRule.setContent {
            CategoryPickerScreen(
                selectedType = "TRANSFER",
                categories = allExpenseCategories,
                selectedCategoryId = null,
                accounts = accounts,
                selectedAccountId = null,
                currencyCode = "KES",
                onItemSelected = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Cash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bank").assertIsDisplayed()

        composeTestRule.onNodeWithText("Food").assertDoesNotExist()
    }

    @Test
    fun categoryPicker_whenExpenseType_hidesIncomeCategories() {
        composeTestRule.setContent {
            CategoryPickerScreen(
                selectedType = "EXPENSE",
                categories = allExpenseCategories + incomeCategories,
                selectedCategoryId = null,
                accounts = accounts,
                selectedAccountId = null,
                currencyCode = "KES",
                onItemSelected = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salary").assertDoesNotExist()
    }

    @Test
    fun categoryPicker_selectingCategory_triggersCallback() {
        var selectedId: String? = null

        composeTestRule.setContent {
            CategoryPickerScreen(
                selectedType = "EXPENSE",
                categories = allExpenseCategories,
                selectedCategoryId = null,
                accounts = accounts,
                selectedAccountId = null,
                currencyCode = "KSH",
                onItemSelected = { selectedId = it },
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Groceries").performClick()
        assert(selectedId == "c1") { "Expected c1 but got $selectedId" }
    }
}
