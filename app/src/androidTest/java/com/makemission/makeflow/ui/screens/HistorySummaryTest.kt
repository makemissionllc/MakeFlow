package com.makemission.makeflow.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistorySummaryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val viewModelStore = ViewModelStore()
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepositoryImpl
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FinanceRepositoryImpl(database.categoryDao(), database.transactionDao(), database.taskDao(), database.accountDao())
        val dataStore = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        instrumentation.runOnMainSync {
            viewModel = HistoryViewModel(repository, UserPreferencesDataStore(dataStore))
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

    private fun showHistory() {
        composeTestRule.setContent {
            MakeFlowTheme {
                val haptics = remember { HapticManager(context).apply { strength = HapticStrength.OFF } }
                HistoryScreen(viewModel, haptics)
            }
        }
    }
}
