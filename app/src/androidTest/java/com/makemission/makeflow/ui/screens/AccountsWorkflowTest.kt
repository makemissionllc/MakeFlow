package com.makemission.makeflow.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.ui.theme.compactDimens
import com.makemission.makeflow.ui.theme.expandedDimens
import com.makemission.makeflow.ui.theme.mediumDimens
import com.makemission.makeflow.ui.viewmodel.AccountsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AccountsWorkflowTest {
    @get:Rule val rule = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store = ViewModelStore()
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepositoryImpl
    private lateinit var viewModel: AccountsViewModel
    private val mode = mutableStateOf(DeviceLayoutMode.COMPACT)
    private val cash = AccountEntity("cash", "Daily wallet", 100.0, 0.0, currencyCode = "USD")
    private val bank = AccountEntity("bank", "Travel bank", 1500.0, 0.0, currencyCode = "KES")
    private val transfer = TransactionEntity("transfer", 10.0, "TRANSFER", accountId = "cash",
        fromAccountId = "cash", toAccountId = "bank", destinationAmount = 1500.0,
        note = "Holiday funds", externalReference = "TRANSFER-123", timestamp = 1700000000000L)

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FinanceRepositoryImpl(database.categoryDao(), database.transactionDao(), database.taskDao(), database.accountDao())
        val dataStore = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        val preferences = UserPreferencesDataStore(dataStore)
        runBlocking { preferences.saveCurrency("USD") }
        instrumentation.runOnMainSync {
            viewModel = AccountsViewModel(repository, preferences)
            store.put("accounts", viewModel)
        }
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync { store.clear() }
        database.close()
    }

    @Test fun phoneSearchCombinesNameAndCurrencyWithoutChangingNetWorth() {
        seed()
        show()
        rule.waitUntil(5000) { viewModel.accounts.value.size == 2 && viewModel.totalBalance.value == 100.0 }
        rule.onNodeWithText("Search accounts or currency").performScrollTo().performTextInput("WALLET usd")
        rule.onNodeWithText(cash.name).assertIsDisplayed()
        rule.onNodeWithText(bank.name).assertDoesNotExist()
        rule.runOnIdle { assertEquals(100.0, viewModel.totalBalance.value, 0.0) }
        rule.onNodeWithContentDescription("Clear account search").performClick()
        rule.onNodeWithText(bank.name).performScrollTo().assertIsDisplayed()
    }

    @Test fun mediumNoMatchesKeepsClearSearchReachable() {
        seed()
        show(DeviceLayoutMode.MEDIUM)
        rule.waitUntil(5000) { viewModel.accounts.value.size == 2 }
        rule.onNodeWithText("Search accounts or currency").performScrollTo().performTextInput("missing")
        rule.onNodeWithText("No matching accounts").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Clear account search").performScrollTo().performClick()
        rule.onNodeWithText(bank.name).performScrollTo().assertIsDisplayed()
    }

    @Test fun tabletTransfersUseAccountSpecificAmountsAndEditUsesExistingSheet() {
        seed()
        runBlocking { repository.insertTransaction(transfer) }
        show(DeviceLayoutMode.EXPANDED)
        select("bank")
        rule.onNodeWithText("Transfer in").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("+" + CurrencyFormatter.format(1500.0, "KES")).assertIsDisplayed()
        rule.onNodeWithContentDescription("Edit account").performClick()
        rule.onNodeWithText("Edit Account").assertIsDisplayed()
        rule.onNodeWithContentDescription("Close").performClick()
        select("cash")
        rule.onNodeWithText("Transfer out").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("-" + CurrencyFormatter.format(10.0, "USD")).assertIsDisplayed()
        saveScreenshot("accounts_tablet_activity.png")
    }

    @Test fun missingDestinationAmountIsDisclosedAndOtherAccountActivityDoesNotLeak() {
        seed()
        runBlocking {
            repository.insertTransaction(transfer.copy(destinationAmount = null))
            repository.insertTransaction(TransactionEntity("cash-only", 4.0, "EXPENSE", accountId = "cash", note = "Cash only"))
        }
        show(DeviceLayoutMode.EXPANDED)
        select("cash")
        rule.onNodeWithTag("account_activity_cash-only").performScrollTo().assertIsDisplayed()
        select("bank")
        rule.onNodeWithText("Amount unavailable").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("account_activity_cash-only").assertDoesNotExist()
    }

    @Test fun selectionAndSearchSurviveRestorationAndWindowResizing() {
        seed()
        val restoration = StateRestorationTester(rule)
        show(DeviceLayoutMode.EXPANDED, restoration = restoration)
        select("bank")
        rule.onNodeWithText("Search accounts or currency").performScrollTo().performTextInput("kes")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("account_bank").assertIsSelected()
        rule.onNodeWithTag("account_cash").assertDoesNotExist()
        rule.runOnIdle { mode.value = DeviceLayoutMode.COMPACT }
        rule.onNodeWithTag("account_activity_pane").assertDoesNotExist()
        rule.onNodeWithText(cash.name).assertDoesNotExist()
        rule.runOnIdle { mode.value = DeviceLayoutMode.EXPANDED }
        rule.onNodeWithTag("account_bank").performScrollTo().assertIsSelected()
        rule.onNodeWithContentDescription("Clear account search").performScrollTo().performClick()
        rule.onNodeWithTag("account_cash").performScrollTo().assertIsDisplayed()
    }

    @Test fun selectedAccountAndActivityStayLiveAndClearAfterDeletion() {
        seed()
        runBlocking { repository.insertTransaction(transfer) }
        show(DeviceLayoutMode.EXPANDED)
        select("bank")
        rule.onNodeWithTag("account_activity_transfer").performScrollTo().assertIsDisplayed()
        runBlocking {
            repository.updateAccount(bank.copy(name = "Updated bank", balance = 2000.0))
            repository.updateTransaction(transfer.copy(note = "Updated transfer", destinationAmount = 1700.0))
        }
        rule.onNodeWithText("Updated transfer").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("+" + CurrencyFormatter.format(1700.0, "KES")).assertIsDisplayed()
        rule.onNodeWithContentDescription("Edit account").performClick()
        rule.onNodeWithTag("account_edit_name_input").assertTextEquals("Updated bank")
        rule.onNodeWithContentDescription("Close").performClick()
        runBlocking { repository.deleteTransaction(transfer.copy(destinationAmount = 1700.0, note = "Updated transfer")) }
        rule.onNodeWithText("No transactions in this account yet").performScrollTo().assertIsDisplayed()
        runBlocking { repository.deleteAccount(bank.copy(name = "Updated bank", balance = 2000.0)) }
        rule.onNodeWithText("Select an account to view activity").assertIsDisplayed()
        rule.onNodeWithContentDescription("Edit account").assertDoesNotExist()
    }

    @Test fun longNotesAndReferencesRemainReachableWithLargeText() {
        seed()
        runBlocking { repository.insertTransaction(transfer.copy(note = "A longer transfer note. ".repeat(80))) }
        show(DeviceLayoutMode.EXPANDED, fontScale = 2f)
        select("bank")
        rule.onNodeWithText("Reference: TRANSFER-123").performScrollTo().assertIsDisplayed()
        saveScreenshot("accounts_tablet_large_text.png")
        rule.onNodeWithContentDescription("Clear account selection").performClick()
        rule.onNodeWithText("Select an account to view activity").assertIsDisplayed()
    }

    @Test fun emptyTabletKeepsAddAccountActionAndEmptyState() {
        show(DeviceLayoutMode.EXPANDED)
        rule.onNodeWithText("No accounts yet").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Add account").performClick()
        rule.onNodeWithText("New Account").assertIsDisplayed()
    }

    private fun seed() = runBlocking {
        repository.insertAccount(cash)
        repository.insertAccount(bank)
    }

    private fun select(id: String) {
        rule.waitUntil(5000) { viewModel.accounts.value.size == 2 }
        rule.onNodeWithTag("account_$id").performScrollTo().performClick()
        rule.onNodeWithTag("account_$id").assertIsSelected()
    }

    private fun saveScreenshot(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun show(layout: DeviceLayoutMode = DeviceLayoutMode.COMPACT, fontScale: Float = 1f,
        restoration: StateRestorationTester? = null) {
        mode.value = layout
        val content: @Composable () -> Unit = {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(when (mode.value) {
                DeviceLayoutMode.EXPANDED -> DpSize(1200.dp, 900.dp)
                DeviceLayoutMode.MEDIUM -> DpSize(700.dp, 900.dp)
                else -> DpSize(400.dp, 800.dp)
            }) then DeviceConfigurationOverride.FontScale(fontScale)) {
                MakeFlowTheme {
                    CompositionLocalProvider(LocalDeviceLayoutMode provides mode.value,
                        LocalAppDimens provides when (mode.value) {
                            DeviceLayoutMode.EXPANDED -> expandedDimens
                            DeviceLayoutMode.MEDIUM -> mediumDimens
                            else -> compactDimens
                        }, LocalReducedMotion provides true) {
                        val haptics = remember { HapticManager(context).apply { strength = HapticStrength.OFF } }
                        AccountsScreen({}, viewModel, hapticManager = haptics)
                    }
                }
            }
        }
        if (restoration == null) rule.setContent(content) else restoration.setContent(content)
    }
}
