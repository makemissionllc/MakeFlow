package com.makemission.makeflow.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.components.AdaptiveConfig
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.ui.viewmodel.HomeDashboardViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeDashboardPrivacyTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val viewModelStore = ViewModelStore()
    private lateinit var database: AppDatabase
    private lateinit var viewModel: HomeDashboardViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = FinanceRepositoryImpl(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            taskDao = database.taskDao(),
            accountDao = database.accountDao(),
            budgetDao = database.budgetDao(),
            recurringPatternDao = database.recurringPatternDao(),
            recurringTransactionDao = database.recurringTransactionDao(),
            exchangeRateDao = database.exchangeRateDao()
        )
        val preferences = UserPreferencesDataStore(InMemoryDataStore())
        viewModel = HomeDashboardViewModel(repository, preferences, database.cardInteractionDao())
        viewModelStore.put("home", viewModel)
        val haptics = HapticManager(context).apply { strength = HapticStrength.OFF }

        composeTestRule.setContent {
            MakeFlowTheme {
                CompositionLocalProvider(
                    LocalDeviceLayoutMode provides DeviceLayoutMode.COMPACT,
                    LocalAdaptiveConfig provides AdaptiveConfig(layoutMode = DeviceLayoutMode.COMPACT),
                    LocalReducedMotion provides true
                ) {
                    HomeDashboard(viewModel = viewModel, hapticManager = haptics)
                }
            }
        }
    }

    @After
    fun tearDown() {
        composeTestRule.runOnIdle { viewModelStore.clear() }
        database.close()
    }

    @Test
    fun dashboardBalanceCanBeHiddenAndShownAgain() {
        composeTestRule.waitUntil(5_000) {
            viewModel.focusedAccountReady.value && !viewModel.dashboardBalanceHidden.value
        }

        composeTestRule.onNodeWithContentDescription("Hide balance").performClick()
        composeTestRule.waitUntil(5_000) { viewModel.dashboardBalanceHidden.value }
        composeTestRule.onNodeWithContentDescription("Balance hidden").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Show balance").performClick()
        composeTestRule.waitUntil(5_000) { !viewModel.dashboardBalanceHidden.value }
        composeTestRule.onNodeWithText(CurrencyFormatter.formatCompact(0.0, "KES")).assertIsDisplayed()
    }

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
