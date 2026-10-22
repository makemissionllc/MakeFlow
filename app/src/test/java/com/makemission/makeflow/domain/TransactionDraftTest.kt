package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDraftTest {
    @Test
    fun encodeDecode_preservesUnicodeAndPendingCalculatorOperation() {
        val engine = CalculatorEngine().apply {
            onDigit(2)
            onDigit(5)
            onOperator(CalculatorEngine.Operator.ADD)
            onDigit(7)
        }
        val expected = TransactionDraft(
            type = "TRANSFER",
            accountId = "wallet-1",
            categoryId = null,
            note = "Lunch • café / 🥑",
            timestamp = 1_792_663_200_000,
            fromAccountId = "wallet-1",
            toAccountId = "bank-2",
            calculator = engine.snapshot(),
            confirmedAmount = null
        )

        assertEquals(expected, TransactionDraft.decode(expected.encode()))
    }

    @Test
    fun restoredCalculator_continuesTheSavedOperation() {
        val original = CalculatorEngine().apply {
            onDigit(2)
            onDigit(5)
            onOperator(CalculatorEngine.Operator.ADD)
            onDigit(7)
        }
        val restored = CalculatorEngine().apply {
            restore(TransactionDraft(
                type = "EXPENSE",
                accountId = "cash",
                categoryId = "food",
                note = "Lunch",
                timestamp = 1_792_663_200_000,
                fromAccountId = "cash",
                toAccountId = "cash",
                calculator = original.snapshot(),
                confirmedAmount = null
            ).let { TransactionDraft.decode(it.encode())!!.calculator })
        }

        assertTrue(restored.hasUnconfirmedOperation)
        assertEquals(32.0, restored.onConfirm()!!, 0.001)
    }

    @Test
    fun decode_rejectsInvalidOrUnsupportedData() {
        assertNull(TransactionDraft.decode(null))
        assertNull(TransactionDraft.decode("not-a-draft"))
        assertNull(TransactionDraft.decode("v2"))
    }
}
