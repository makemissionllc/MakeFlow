package com.makemission.makeflow.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.DuplicateCheckResult
import com.makemission.makeflow.domain.CategoryUsage
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RepeatTransactionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val accounts = listOf(
        AccountEntity(id = "cash", name = "Cash", balance = 5000.0, initialBalance = 0.0),
        AccountEntity(id = "usd", name = "Dollar account", balance = 100.0, initialBalance = 0.0, currencyCode = "USD")
    )
    private val original = TransactionEntity(
        id = "original",
        amount = 250.0,
        type = "EXPENSE",
        categoryId = "food",
        accountId = "cash",
        note = "Lunch",
        timestamp = 1_700_000_000_000L,
        externalReference = "MPESA-ORIGINAL"
    )
    private var candidate: TransactionEntity? = null
    private var insertCount = 0
    private var updateCount = 0

    @Test
    fun logAgainCreatesNewEntryWithCurrentDateAndNoCopiedReference() {
        showEditor(original)
        val beforeRepeat = System.currentTimeMillis()

        composeTestRule.onNodeWithText("Log again").performClick()
        composeTestRule.onNodeWithText("Log again").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.runOnIdle {
            val entry = candidate
            assertNotNull(entry)
            assertNotEquals(original.id, entry!!.id)
            assertNull(entry.externalReference)
            assertTrue(entry.timestamp in beforeRepeat..System.currentTimeMillis())
            assertEquals(original.amount, entry.amount, 0.0)
            assertEquals(original.type, entry.type)
            assertEquals(original.categoryId, entry.categoryId)
            assertEquals(original.accountId, entry.accountId)
            assertEquals(original.note, entry.note)
            assertEquals(1, insertCount)
            assertEquals(0, updateCount)
        }
    }

    @Test
    fun logAgainRequiresConfirmationForPossibleDuplicate() {
        showEditor(original, duplicate = DuplicateCheckResult(original, 1L, "recent payment"))
        composeTestRule.onNodeWithText("Log again").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Possible duplicate").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(0, insertCount)
            assertEquals(0, updateCount)
        }

        composeTestRule.onNodeWithText("Save Anyway").performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, insertCount)
            assertEquals(0, updateCount)
        }
    }

    @Test
    fun logAgainTransferUsesSourceAccountAndCurrentConversion() {
        val transfer = original.copy(
            amount = 10.0,
            type = "TRANSFER",
            categoryId = null,
            fromAccountId = "usd",
            toAccountId = "cash",
            destinationAmount = 1200.0
        )
        showEditor(
            transfer,
            rates = listOf(ExchangeRateEntity("USD", 150.0, baseCurrencyCode = "KES"))
        )
        composeTestRule.onNodeWithText("Log again").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.runOnIdle {
            val entry = candidate!!
            assertEquals("usd", entry.accountId)
            assertEquals("usd", entry.fromAccountId)
            assertEquals("cash", entry.toAccountId)
            assertEquals(1500.0, entry.destinationAmount!!, 0.0)
            assertEquals(1, insertCount)
            assertEquals(0, updateCount)
        }
    }

    @Test
    fun logAgainCannotSaveToAnAccountThatNoLongerExists() {
        showEditor(original.copy(accountId = "removed-account"))
        composeTestRule.onNodeWithText("Log again").performClick()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
        composeTestRule.runOnIdle {
            assertEquals(0, insertCount)
            assertEquals(0, updateCount)
        }
    }

    @Test
    fun expandedTransactionEntryShowsSelectedAccountBalance() {
        showNewEntry()

        composeTestRule.onNodeWithText(
            "Available balance · ${CurrencyFormatter.format(5000.0, "KES")}"
        ).assertIsDisplayed()
    }

    @Test
    fun frequentCategoryQuickPickIsAvailableOnPhone() {
        showEditorContent(DeviceLayoutMode.COMPACT) {
            AddTransactionScreen(
                onDismiss = {},
                onSave = { _, _, _, _, _, _, _, _, _ -> },
                accounts = accounts,
                categories = listOf(CategoryEntity(id = "food", name = "Food", type = "EXPENSE")),
                currencyCode = "KES",
                hapticManager = haptics(),
                smartCategorizationEnabled = false,
                onGetCategoryUsages = {
                    val now = System.currentTimeMillis()
                    listOf(
                        CategoryUsage("food", "EXPENSE", now),
                        CategoryUsage("food", "EXPENSE", now - 1_000),
                        CategoryUsage("food", "EXPENSE", now - 2_000)
                    )
                }
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("frequent_category_food").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Frequent categories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("frequent_category_food").assertIsDisplayed()
    }

    private fun showEditor(
        transaction: TransactionEntity,
        rates: List<ExchangeRateEntity> = emptyList(),
        duplicate: DuplicateCheckResult? = null
    ) {
        showEditorContent(DeviceLayoutMode.COMPACT) {
            AddTransactionScreen(
                onDismiss = {},
                onSave = { _, _, _, _, _, _, _, _, _ -> insertCount += 1 },
                accounts = accounts,
                categories = listOf(CategoryEntity(id = "food", name = "Food", type = "EXPENSE")),
                currencyCode = "KES",
                exchangeRates = rates,
                hapticManager = haptics(),
                smartCategorizationEnabled = false,
                editTransaction = transaction,
                onUpdate = { updateCount += 1 },
                onCheckDuplicate = {
                    candidate = it
                    duplicate
                }
            )
        }
    }

    private fun showNewEntry() {
        showEditorContent(DeviceLayoutMode.EXPANDED) {
            AddTransactionScreen(
                onDismiss = {},
                onSave = { _, _, _, _, _, _, _, _, _ -> insertCount += 1 },
                accounts = accounts,
                categories = listOf(CategoryEntity(id = "food", name = "Food", type = "EXPENSE")),
                currencyCode = "KES",
                initialAmount = "100",
                hapticManager = haptics(),
                smartCategorizationEnabled = false,
            )
        }
    }

    private fun showEditorContent(mode: DeviceLayoutMode, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDeviceLayoutMode provides mode) {
                MakeFlowTheme(themeMode = "light") {
                    content()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun haptics(): HapticManager {
        val context = LocalContext.current
        return remember(context) { HapticManager(context).apply { strength = HapticStrength.OFF } }
    }
}
