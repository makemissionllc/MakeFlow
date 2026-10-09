package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionExitGuardTest {
    private val original = TransactionEntity(
        id = "original", amount = 250.0, type = "EXPENSE", categoryId = "food",
        accountId = "cash", note = "Lunch", timestamp = 1000L
    )
    private val unchanged = TransactionExitGuard.FormState(
        amountInput = "250", type = "EXPENSE", categoryId = "food",
        accountId = "cash", note = "Lunch", timestamp = 1000L
    )

    @Test
    fun emptyNewFormAndAutomaticDefaultsDoNotRequireConfirmation() {
        val blank = unchanged.copy(amountInput = "", note = "")
        listOf("", "0", "0.00", "0.").forEach { amount ->
            assertFalse(TransactionExitGuard.hasUnsavedChanges(blank.copy(amountInput = amount)))
        }
        assertFalse(TransactionExitGuard.hasUnsavedChanges(blank.copy(
            type = "INCOME", accountId = "bank", categoryId = "salary", timestamp = 2000L
        )))
    }

    @Test
    fun newAmountOrNoteRequiresConfirmationIncludingPrefilledImports() {
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = "", note = "Receipt")))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = "15", note = "")))
        assertFalse(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = "0", note = "  ")))
    }

    @Test
    fun unfinishedOrInvalidCalculatorInputIsProtected() {
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = "0", note = "", hasPendingCalculation = true)))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = "Error", note = "")))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = ".", note = "")))
    }

    @Test
    fun unchangedEditAndEquivalentNumericFormattingCloseImmediately() {
        listOf("250", "250.0", "250.00", "2.5E+2").forEach {
            assertFalse(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = it), original))
        }
    }

    @Test
    fun eachRelevantEditedFieldIsProtected() {
        listOf(
            unchanged.copy(amountInput = "251"),
            unchanged.copy(type = "INCOME"),
            unchanged.copy(categoryId = "transport"),
            unchanged.copy(accountId = "bank"),
            unchanged.copy(note = "Dinner"),
            unchanged.copy(timestamp = 2000L),
            unchanged.copy(hasPendingCalculation = true)
        ).forEach { assertTrue(TransactionExitGuard.hasUnsavedChanges(it, original)) }
    }

    @Test
    fun clearingOriginalAmountOrNoteIsStillAnEdit() {
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(amountInput = ""), original))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(note = ""), original))
    }

    @Test
    fun revertingEditsRemovesConfirmation() {
        val edited = unchanged.copy(note = "Dinner")
        assertTrue(TransactionExitGuard.hasUnsavedChanges(edited, original))
        assertFalse(TransactionExitGuard.hasUnsavedChanges(edited.copy(note = original.note), original))
    }

    @Test
    fun nonTransferFormsIgnoreHiddenTransferSelections() {
        assertFalse(TransactionExitGuard.hasUnsavedChanges(unchanged.copy(fromAccountId = "bank", toAccountId = "wallet"), original))
    }

    @Test
    fun transferIgnoresLegacyAccountAndCategoryButProtectsEndpoints() {
        val transfer = original.copy(type = "TRANSFER", fromAccountId = "bank", toAccountId = "cash", destinationAmount = 30000.0)
        val form = unchanged.copy(type = "TRANSFER", accountId = "bank", categoryId = null, fromAccountId = "bank", toAccountId = "cash")
        assertFalse(TransactionExitGuard.hasUnsavedChanges(form, transfer))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(form.copy(fromAccountId = "savings"), transfer))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(form.copy(toAccountId = "savings"), transfer))
    }

    @Test
    fun legacyTransferWithoutFromIdUsesItsAccount() {
        val transfer = original.copy(type = "TRANSFER", fromAccountId = null, toAccountId = "bank")
        val form = unchanged.copy(type = "TRANSFER", categoryId = null, fromAccountId = "cash", toAccountId = "bank")
        assertFalse(TransactionExitGuard.hasUnsavedChanges(form, transfer))
    }

    @Test
    fun logAgainIsNewUnsavedWorkEvenWithUnchangedDetails() {
        assertFalse(TransactionExitGuard.hasUnsavedChanges(unchanged, original))
        assertTrue(TransactionExitGuard.hasUnsavedChanges(unchanged, original = null))
    }
}
