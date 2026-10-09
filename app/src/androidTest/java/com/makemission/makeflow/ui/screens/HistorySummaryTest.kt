package com.makemission.makeflow.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.compactDimens
import com.makemission.makeflow.ui.theme.expandedDimens
import com.makemission.makeflow.ui.theme.mediumDimens
import com.makemission.makeflow.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HistorySummaryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val viewModelStore = ViewModelStore()
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepositoryImpl
    private lateinit var viewModel: HistoryViewModel
    private lateinit var preferences: UserPreferencesDataStore
    private val layoutMode = mutableStateOf(DeviceLayoutMode.COMPACT)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FinanceRepositoryImpl(database.categoryDao(), database.transactionDao(), database.taskDao(), database.accountDao())
        val dataStore = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        preferences = UserPreferencesDataStore(dataStore)
        instrumentation.runOnMainSync {
            viewModel = HistoryViewModel(repository, preferences)
            viewModelStore.put("history", viewModel)
        }
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync { viewModelStore.clear() }
        database.close()
    }

    @Test
    fun summaryActionOpensPersistedResultsAndFollowsSearch() {
        runBlocking {
            repository.insertAccount(AccountEntity("cash", "Daily wallet", 0.0, 0.0, currencyCode = "USD"))
            repository.insertTransaction(TransactionEntity(amount = 100.0, type = "INCOME", accountId = "cash", note = "Salary"))
            repository.insertTransaction(TransactionEntity(amount = 35.0, type = "EXPENSE", accountId = "cash", note = "Coffee"))
        }
        showHistory()
        composeTestRule.waitUntil(5000) { viewModel.resultSummary.value.transactionCount == 2 }
        composeTestRule.onNodeWithText("Summary").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Net change").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(CurrencyFormatter.format(65.0, "USD")).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        composeTestRule.onNodeWithText("Search transactions").performScrollTo().performTextInput("coffee")
        composeTestRule.waitUntil(5000) { viewModel.resultSummary.value.transactionCount == 1 }
        composeTestRule.onNodeWithText("Summary").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Search: coffee").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(CurrencyFormatter.format(0.0, "USD")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun emptyHistoryDisablesSummaryAction() {
        showHistory()
        composeTestRule.onNodeWithText("Summary").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithText("0 transactions").assertIsDisplayed()
    }

    @Test
    fun accountPickerFiltersHistoryWithoutLeavingTheScreen() {
        runBlocking {
            repository.insertAccount(AccountEntity("cash", "Daily wallet", 0.0, 0.0, currencyCode = "USD"))
            repository.insertAccount(AccountEntity("bank", "Bank", 0.0, 0.0, currencyCode = "USD"))
            repository.insertTransaction(TransactionEntity(amount = 100.0, type = "INCOME", accountId = "cash", note = "Salary"))
            repository.insertTransaction(TransactionEntity(amount = 35.0, type = "EXPENSE", accountId = "bank", note = "Coffee"))
        }
        showHistory(DeviceLayoutMode.MEDIUM)
        composeTestRule.waitUntil(5000) { viewModel.resultSummary.value.transactionCount == 2 }
        composeTestRule.onNodeWithContentDescription("Choose account").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Bank").performClick()
        composeTestRule.waitUntil(5000) { viewModel.focusedAccountId.value == "bank" && viewModel.filteredTransactions.value.size == 1 }
        composeTestRule.onNodeWithText("1 transaction").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Choose account").performScrollTo().performClick()
        composeTestRule.onNodeWithText("All accounts").performClick()
        composeTestRule.waitUntil(5000) { viewModel.resultSummary.value.transactionCount == 2 }
        composeTestRule.onNodeWithText("2 transactions").assertIsDisplayed()
    }

    @Test
    fun deletedAccountFocusIsDisclosedAndCanBeCleared() {
        seedReviewEntries()
        runBlocking { preferences.saveFocusedAccountId("deleted") }
        showHistory()
        composeTestRule.waitUntil(5000) { viewModel.focusedAccountReady.value }
        composeTestRule.onNodeWithText("Unavailable account").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Choose account").performClick()
        composeTestRule.onNodeWithText("All accounts").performClick()
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 2 }
        composeTestRule.onNodeWithText("2 transactions").assertIsDisplayed()
    }

    @Test
    fun tabletReviewNavigatesWithButtonsAndKeyboardAndEditsCurrentRecord() {
        val entries = seedReviewEntries()
        var edited: TransactionEntity? = null
        showHistory(DeviceLayoutMode.EXPANDED, onEdit = { edited = it })
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 2 }
        composeTestRule.onNodeWithText("Start review").performClick()
        composeTestRule.onNodeWithText("1 of 2").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Previous transaction").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("history_review_pane").performKeyInput { pressKey(Key.PageDown) }
        composeTestRule.onNodeWithText("2 of 2").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next transaction").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Edit transaction").performClick()
        composeTestRule.runOnIdle { assertEquals(entries[1].id, edited?.id) }
        composeTestRule.onNodeWithContentDescription("Previous transaction").performClick()
        composeTestRule.onNodeWithText("1 of 2").assertIsDisplayed()
        saveScreenshot("history_tablet_review.png")
    }

    @Test
    fun tabletSelectionSurvivesSavedStateAndWindowSizeChanges() {
        seedReviewEntries()
        val restoration = StateRestorationTester(composeTestRule)
        showHistory(DeviceLayoutMode.EXPANDED, restoration = restoration)
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 2 }
        composeTestRule.onNodeWithText("Start review").performClick()
        composeTestRule.onNodeWithContentDescription("Next transaction").performClick()
        restoration.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithText("2 of 2").assertIsDisplayed()
        composeTestRule.runOnIdle { layoutMode.value = DeviceLayoutMode.COMPACT }
        composeTestRule.onNodeWithTag("history_review_pane").assertDoesNotExist()
        composeTestRule.runOnIdle { layoutMode.value = DeviceLayoutMode.EXPANDED }
        composeTestRule.onNodeWithText("2 of 2").assertIsDisplayed()
    }

    @Test
    fun tabletDetailsStayLiveAndDoNotKeepDeletedOrFilteredOutRecords() {
        val entries = seedReviewEntries()
        showHistory(DeviceLayoutMode.EXPANDED)
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 2 }
        composeTestRule.onNodeWithText("Start review").performClick()
        composeTestRule.onNodeWithContentDescription("Next transaction").performClick()
        runBlocking { repository.updateTransaction(entries[1].copy(note = "Updated coffee", amount = 40.0)) }
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.any { it.note == "Updated coffee" } }
        composeTestRule.onNodeWithText("Updated coffee").performScrollTo().assertIsDisplayed()
        composeTestRule.runOnIdle { viewModel.searchQuery.value = "Salary" }
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 1 }
        composeTestRule.onNodeWithText("Updated coffee").assertDoesNotExist()
        composeTestRule.runOnIdle { viewModel.searchQuery.value = "" }
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 2 }
        composeTestRule.onNodeWithText("Updated coffee").performScrollTo().assertIsDisplayed()
        runBlocking { repository.deleteTransaction(entries[1].copy(note = "Updated coffee", amount = 40.0)) }
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 1 }
        composeTestRule.onNodeWithText("Updated coffee").assertDoesNotExist()
        composeTestRule.onNodeWithText("Start review").assertIsDisplayed()
    }

    @Test
    fun tabletTransferDoesNotInventMissingDestinationAmount() {
        runBlocking {
            repository.insertAccount(AccountEntity("cash", "Cash", 0.0, 0.0, currencyCode = "USD"))
            repository.insertAccount(AccountEntity("bank", "Bank", 0.0, 0.0, currencyCode = "KES"))
            repository.insertTransaction(TransactionEntity(amount = 10.0, type = "TRANSFER", accountId = "cash", fromAccountId = "cash", toAccountId = "bank"))
        }
        showHistory(DeviceLayoutMode.EXPANDED)
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 1 }
        composeTestRule.onNodeWithText("Start review").performClick()
        composeTestRule.onNodeWithText("Destination amount unavailable").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tabletLongNotesAndReferencesRemainReachableWithLargeText() {
        val entries = seedReviewEntries()
        runBlocking { repository.updateTransaction(entries[1].copy(note = "A longer transaction note. ".repeat(80))) }
        showHistory(DeviceLayoutMode.EXPANDED, fontScale = 2f)
        composeTestRule.waitUntil(5000) { viewModel.filteredTransactions.value.size == 2 }
        composeTestRule.onNodeWithText("Start review").performClick()
        composeTestRule.onNodeWithContentDescription("Next transaction").performClick()
        composeTestRule.onNodeWithText("REFERENCE-123").performScrollTo().assertIsDisplayed()
        saveScreenshot("history_tablet_large_text.png")
    }

    private fun seedReviewEntries(): List<TransactionEntity> = runBlocking {
        repository.insertAccount(AccountEntity("cash", "Daily wallet", 0.0, 0.0, currencyCode = "USD"))
        listOf(
            TransactionEntity(id = "salary", amount = 100.0, type = "INCOME", accountId = "cash", note = "Salary", timestamp = 1700000001000L),
            TransactionEntity(id = "coffee", amount = 35.0, type = "EXPENSE", accountId = "cash", note = "Coffee", timestamp = 1700000000000L, externalReference = "REFERENCE-123")
        ).also { entries -> entries.forEach { repository.insertTransaction(it) } }
    }

    private fun saveScreenshot(name: String) {
        val image = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun showHistory(
        mode: DeviceLayoutMode = DeviceLayoutMode.COMPACT,
        restoration: StateRestorationTester? = null,
        onEdit: ((TransactionEntity) -> Unit)? = null,
        fontScale: Float = 1f
    ) {
        layoutMode.value = mode
        val content: @Composable () -> Unit = {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(
                when (layoutMode.value) {
                    DeviceLayoutMode.EXPANDED -> DpSize(1200.dp, 900.dp)
                    DeviceLayoutMode.MEDIUM -> DpSize(700.dp, 900.dp)
                    else -> DpSize(400.dp, 800.dp)
                }
            ) then DeviceConfigurationOverride.FontScale(fontScale)) {
                MakeFlowTheme {
                    CompositionLocalProvider(
                        LocalDeviceLayoutMode provides layoutMode.value,
                        LocalAppDimens provides when (layoutMode.value) {
                            DeviceLayoutMode.EXPANDED -> expandedDimens
                            DeviceLayoutMode.MEDIUM -> mediumDimens
                            else -> compactDimens
                        },
                        LocalReducedMotion provides true
                    ) {
                        val haptics = remember { HapticManager(context).apply { strength = HapticStrength.OFF } }
                        HistoryScreen(viewModel, haptics, onEdit)
                    }
                }
            }
        }
        if (restoration != null) restoration.setContent(content) else composeTestRule.setContent(content)
    }
}
