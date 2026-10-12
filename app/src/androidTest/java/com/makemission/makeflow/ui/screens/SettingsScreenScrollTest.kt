package com.makemission.makeflow.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.ui.viewmodel.SettingsViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsScreenScrollTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
        val preferences = UserPreferencesDataStore(context.userPrefDataStore)
        val viewModel = SettingsViewModel(repository, preferences)

        composeTestRule.setContent {
            MakeFlowTheme {
                SettingsScreen(onBack = {}, viewModel = viewModel)
            }
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun csvExportOpensSharedDateRangeSheet() {
        composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasText("Export CSV"))
        composeTestRule.onNodeWithText("Export CSV").performClick()
        composeTestRule.onNodeWithText("All time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Last month").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scrollsThroughSettingsToPrivacySection() {
        composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasText("Touch feedback"))
        composeTestRule.onNodeWithText("Touch feedback")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Standard").assertIsDisplayed()
        repeat(4) {
            composeTestRule.onRoot().performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithText("Privacy & data").performScrollTo().assertIsDisplayed()
    }
}
