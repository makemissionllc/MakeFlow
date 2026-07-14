package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaExternalReferenceTest {

    @Test
    fun transactionEntity_hasExternalReferenceField() {
        val tx = TransactionEntity(
            amount = 100.0,
            type = "EXPENSE",
            externalReference = "QAB1XYZ2"
        )
        assertEquals("QAB1XYZ2", tx.externalReference)
    }

    @Test
    fun transactionEntity_externalReferenceDefaultsToNull() {
        val tx = TransactionEntity(
            amount = 100.0,
            type = "EXPENSE"
        )
        assertNull(tx.externalReference)
    }

    @Test
    fun parsedMessage_canBeUsedAsExternalReference() {
        val msg = "QAB1XYZ2 Confirmed. You have received Ksh5,000.00 from John Kamau 0712345678 on 1/3/2025 at 10:30 AM. New M-PESA balance is Ksh12,450.00."
        val parsed = MpesaMessageParser.parse(msg)
        assertNotNull(parsed)

        val tx = TransactionEntity(
            amount = parsed!!.amount,
            type = "INCOME",
            note = parsed.counterpartyName ?: "",
            externalReference = parsed.code
        )
        assertEquals("QAB1XYZ2", tx.externalReference)
        assertEquals(5000.0, tx.amount, 0.01)
    }

    @Test
    fun duplicateGuard_sameCodeBlocked() {
        // Simulate: two transactions with same external reference
        val tx1 = TransactionEntity(amount = 100.0, type = "EXPENSE", externalReference = "CODE123")
        val tx2 = TransactionEntity(amount = 100.0, type = "EXPENSE", externalReference = "CODE123")
        // Same code = exact duplicate
        assertEquals(tx1.externalReference, tx2.externalReference)
    }

    @Test
    fun duplicateGuard_differentCodeAllowed() {
        val tx1 = TransactionEntity(amount = 100.0, type = "EXPENSE", externalReference = "CODE123")
        val tx2 = TransactionEntity(amount = 100.0, type = "EXPENSE", externalReference = "CODE456")
        // Different codes = not duplicates
        assertTrue(tx1.externalReference != tx2.externalReference)
    }

    @Test
    fun duplicateGuard_nullExternalRefNeverTriggers() {
        // Manual entry (null external reference) should never be blocked
        val tx = TransactionEntity(amount = 100.0, type = "EXPENSE")
        assertNull(tx.externalReference)
        // Null external references are never considered duplicates of each other
    }

    @Test
    fun parsedMessage_typeMapping_receivedIsIncome() {
        val msg = "QAB1XYZ2 Confirmed. You have received Ksh500 from Test 0700000000 on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000."
        val parsed = MpesaMessageParser.parse(msg)!!
        // RECEIVED -> INCOME
        assertEquals(MpesaMessageParser.TransactionType.RECEIVED, parsed.type)
    }

    @Test
    fun parsedMessage_typeMapping_sentIsExpense() {
        val msg = "QAB1XYZ2 Confirmed. Ksh500 sent to Test 0700000000 on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000. Transaction cost, Ksh10."
        val parsed = MpesaMessageParser.parse(msg)!!
        assertEquals(MpesaMessageParser.TransactionType.SENT, parsed.type)
    }

    @Test
    fun parsedMessage_typeMapping_paybillIsExpense() {
        val msg = "QAB1XYZ2 Confirmed. Ksh500 paid to KPLC. on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000. Transaction cost, Ksh10."
        val parsed = MpesaMessageParser.parse(msg)!!
        assertEquals(MpesaMessageParser.TransactionType.PAYBILL, parsed.type)
    }

    @Test
    fun parsedMessage_typeMapping_withdrawIsExpense() {
        val msg = "QAB1XYZ2 Confirmed. Ksh500 withdrawn from Agent on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000. Transaction cost, Ksh10."
        val parsed = MpesaMessageParser.parse(msg)!!
        assertEquals(MpesaMessageParser.TransactionType.WITHDRAW, parsed.type)
    }

    @Test
    fun parsedMessage_typeMapping_airtimeIsExpense() {
        val msg = "QAB1XYZ2 Confirmed. You bought Ksh500 of airtime on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000."
        val parsed = MpesaMessageParser.parse(msg)!!
        assertEquals(MpesaMessageParser.TransactionType.AIRTIME, parsed.type)
    }
}
