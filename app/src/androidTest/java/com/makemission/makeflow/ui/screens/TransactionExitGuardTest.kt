package com.makemission.makeflow.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.PastTransactionForSuggestion
import com.makemission.makeflow.domain.TransactionCommitHandler
import com.makemission.makeflow.domain.TransactionDraft
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransactionExitGuardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var dismissals = 0
    private var saves = 0
    private val accounts = listOf(AccountEntity("cash", "Cash", 0.0, 0.0))
    private val categories = listOf(
        CategoryEntity("food", "Food", "EXPENSE"),
        CategoryEntity("other", "Other", "EXPENSE")
    )
    private val original = TransactionEntity(
        id = "original", amount = 250.0, type = "EXPENSE", categoryId = "food",
        accountId = "cash", note = "Original purchase", timestamp = 1000L
    )

    @Test
    fun emptyEditorClosesWithoutConfirmation() {
        showForm(amount = "", note = "")
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeTestRule.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun systemBackOffersKeepEditingAndPreservesPrefilledEntry() {
        showForm()
        composeTestRule.onNodeWithText("New").assertIsDisplayed()
        Espresso.pressBack()
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep editing").performClick()
        composeTestRule.onNodeWithText("Unfinished purchase").performScrollTo().assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(0, dismissals)
            assertEquals(0, saves)
        }
    }

    @Test
    fun discardClosesEditorWithoutSaving() {
        showForm()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Discard").performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, saves)
        }
    }

    @Test
    fun unchangedEditClosesWithoutConfirmation() {
        showForm(transaction = original)
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeTestRule.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun successfulSaveDoesNotAskToDiscard() {
        showForm()
        composeTestRule.onNodeWithText("Save").performClick()
        Espresso.pressBack()
        composeTestRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(1, saves)
        }
    }

    @Test
    fun fullScreenDraftAutosavesAndRestoresWhenFormReopens() {
        var savedDraft: TransactionDraft? = null
        val formOpen = mutableStateOf(true)
        composeTestRule.setContent {
            val context = LocalContext.current
            val haptics = remember(context) { HapticManager(context).apply { strength = HapticStrength.OFF } }
            MakeFlowTheme {
                if (formOpen.value) {
                    AddTransactionScreen(
                        onDismiss = { formOpen.value = false },
                        onSave = { _, _, _, _, _, _, _, _, _ -> },
                        accounts = accounts,
                        categories = categories,
                        currencyCode = "KES",
                        hapticManager = haptics,
                        initialType = "EXPENSE",
                        onLoadDraft = { savedDraft },
                        onSaveDraft = { savedDraft = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("+ Record Name (Optional)")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("transaction_draft_note")
            .performScrollTo()
            .performTextInput("Keep this entry")
        composeTestRule.waitUntil(timeoutMillis = 4_000) {
            savedDraft?.note == "Keep this entry"
        }

        composeTestRule.runOnIdle { formOpen.value = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { formOpen.value = true }
        composeTestRule.waitUntil(timeoutMillis = 4_000) {
            composeTestRule.onAllNodesWithText("Draft restored from this device").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("transaction_draft_note")
            .performScrollTo()
            .assertTextEquals("Keep this entry")
    }

    @Test
    fun emptyQuickAddClosesImmediately() {
        showForm(quickAdd = true, amount = "", note = "")
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeTestRule.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun quickAddCloseCanBeCancelledWithoutLosingTheForm() {
        showForm(quickAdd = true)
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Keep editing").performClick()
        composeTestRule.onNodeWithText("Unfinished purchase").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Discard").performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, saves)
        }
    }

    @Test
    fun sheetDismissalReadsCurrentInputAndKeepsSheetVisibleWhenVetoed() {
        showForm(quickAdd = true, amount = "", note = "")
        composeTestRule.onNodeWithText("Add a note (optional)").performScrollTo().performTextInput("Keep this note")
        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss) { it() }
        composeTestRule.onNodeWithText("Keep editing").performClick()
        composeTestRule.onNodeWithText("Keep this note").performScrollTo().assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(0, dismissals) }
    }

    private fun showForm(
        quickAdd: Boolean = false,
        amount: String = "250",
        note: String = "Unfinished purchase",
        transaction: TransactionEntity? = null
    ) {
        composeTestRule.setContent {
            val context = LocalContext.current
            val haptics = remember(context) { HapticManager(context).apply { strength = HapticStrength.OFF } }
            MakeFlowTheme {
                if (quickAdd) {
                    val handler = remember { TransactionCommitHandler(null, null, haptics) }
                    AddTransactionBottomSheet(
                        onDismiss = { dismissals++ },
                        onSave = { _, _, _, _, _, _, _, _, _ -> saves++ },
                        accounts = accounts,
                        categories = categories,
                        currencyCode = "KES",
                        hapticManager = haptics,
                        initialType = "EXPENSE",
                        initialAmount = amount,
                        initialNote = note,
                        editTransaction = transaction,
                        commitHandler = handler,
                        onGetPastTransactions = { listOf(PastTransactionForSuggestion("Original purchase", "other")) }
                    )
                } else {
                    AddTransactionScreen(
                        onDismiss = { dismissals++ },
                        onSave = { _, _, _, _, _, _, _, _, _ -> saves++ },
                        accounts = accounts,
                        categories = categories,
                        currencyCode = "KES",
                        hapticManager = haptics,
                        initialAmount = amount,
                        initialNote = note,
                        editTransaction = transaction,
                        onGetPastTransactions = { listOf(PastTransactionForSuggestion("Original purchase", "other")) }
                    )
                }
            }
        }
    }
}
