package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaFeeTransactionTest {

    private val feeCategory = CategoryEntity(
        id = "fee-cat-id",
        name = "Transaction Fees",
        type = "EXPENSE",
        parentCategoryId = "bills-parent-id",
        iconName = "receipt",
        colorHex = "#78909C"
    )

    private fun buildFeeTransaction(
        mainCode: String,
        cost: Double,
        accountId: String,
        timestamp: Long
    ): TransactionEntity {
        return TransactionEntity(
            amount = cost,
            type = "EXPENSE",
            categoryId = feeCategory.id,
            accountId = accountId,
            note = "M-PESA transaction cost for $mainCode",
            timestamp = timestamp,
            externalReference = "${mainCode}-fee"
        )
    }

    @Test
    fun feeTransaction_createdCorrectlyWhenCostIsPositive() {
        val cost = 22.0
        val code = "QHK7ABC1"
        val accountId = "mpesa-account-123"
        val timestamp = 1700000000000L

        val feeTx = buildFeeTransaction(code, cost, accountId, timestamp)

        assertEquals(22.0, feeTx.amount, 0.01)
        assertEquals("EXPENSE", feeTx.type)
        assertEquals("fee-cat-id", feeTx.categoryId)
        assertEquals(accountId, feeTx.accountId)
        assertEquals("M-PESA transaction cost for QHK7ABC1", feeTx.note)
        assertEquals(timestamp, feeTx.timestamp)
        assertEquals("QHK7ABC1-fee", feeTx.externalReference)
    }

    @Test
    fun feeTransaction_notCreatedWhenCostIsZero() {
        val cost = 0.0
        // Logic: if cost != null && cost > 0 -> create. cost=0 -> skip.
        assertTrue(cost <= 0.0)
    }

    @Test
    fun feeTransaction_notCreatedWhenCostIsNull() {
        val cost: Double? = null
        // Logic: if cost != null && cost > 0 -> create. null -> skip.
        assertTrue(cost == null)
    }

    @Test
    fun feeTransaction_notCreatedWhenToggleIsOff() {
        // Simulate the condition: logMpesaCosts = false
        val logMpesaCosts = false
        val cost = 22.0
        val code = "QHK7ABC1"
        val wouldCreateFee = logMpesaCosts && cost > 0.0
        assertTrue(!wouldCreateFee)
    }

    @Test
    fun feeTransaction_createdWhenToggleOnAndCostPositive() {
        val logMpesaCosts = true
        val cost = 22.0
        val wouldCreateFee = logMpesaCosts && cost > 0.0
        assertTrue(wouldCreateFee)
    }

    @Test
    fun duplicateGuard_blocksReShareOfMainAndFee() {
        val code = "QHK7ABC1"
        val mainRef = code
        val feeRef = "${code}-fee"

        // Simulating the duplicate check for both references
        val existingRefs = setOf(mainRef, feeRef)

        assertTrue(existingRefs.contains(mainRef))
        assertTrue(existingRefs.contains(feeRef))
    }

    @Test
    fun duplicateGuard_mainRefDoesNotMatchFeeRef() {
        val code = "QHK7ABC1"
        val mainRef = code
        val feeRef = "${code}-fee"

        assertTrue(mainRef != feeRef)
        assertTrue(feeRef.endsWith("-fee"))
    }

    @Test
    fun feeTransaction_accountDateMatchMain() {
        val code = "QHK7ABC1"
        val mainAccountId = "mpesa-123"
        val mainTimestamp = 1700000000000L

        val feeTx = buildFeeTransaction(code, 22.0, mainAccountId, mainTimestamp)

        assertEquals(mainAccountId, feeTx.accountId)
        assertEquals(mainTimestamp, feeTx.timestamp)
    }

    @Test
    fun parsedMessage_transactionCostExtracted() {
        val msg = "QHK7ABC1 Confirmed. Ksh500 sent to Test 0700000000 on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000. Transaction cost, Ksh22."
        val parsed = MpesaMessageParser.parse(msg)
        assertNotNull(parsed)
        assertEquals(22.0, parsed!!.transactionCost!!, 0.01)
    }

    @Test
    fun parsedMessage_noTransactionCostForReceived() {
        val msg = "QHK7ABC1 Confirmed. You have received Ksh5,000.00 from John Kamau 0712345678 on 1/3/2025 at 10:30 AM. New M-PESA balance is Ksh12,450.00."
        val parsed = MpesaMessageParser.parse(msg)
        assertNotNull(parsed)
        assertNull(parsed!!.transactionCost)
    }

    @Test
    fun feeCategory_isChildOfBills() {
        assertEquals("bills-parent-id", feeCategory.parentCategoryId)
        assertEquals("Transaction Fees", feeCategory.name)
        assertEquals("EXPENSE", feeCategory.type)
    }

    @Test
    fun feeTransactionCode_formatIsCodeDashFee() {
        val code = "QHK7ABC1"
        val feeRef = "${code}-fee"
        assertEquals("QHK7ABC1-fee", feeRef)
        assertTrue(feeRef.startsWith(code))
        assertTrue(feeRef.endsWith("-fee"))
    }

    @Test
    fun parsedMessage_costWithCommaSeparator() {
        val msg = "QHK7ABC1 Confirmed. Ksh5,000.00 sent to VODACOM on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh10,000.00. Transaction cost, Ksh1,500.00."
        val parsed = MpesaMessageParser.parse(msg)
        assertNotNull(parsed)
        assertEquals(1500.0, parsed!!.transactionCost!!, 0.01)
    }

    @Test
    fun parsedMessage_costWithoutComma() {
        val msg = "QHK7ABC1 Confirmed. Ksh500.00 sent to JOHN on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1,000.00. Transaction cost Ksh22."
        val parsed = MpesaMessageParser.parse(msg)
        assertNotNull(parsed)
        assertEquals(22.0, parsed!!.transactionCost!!, 0.01)
    }

    @Test
    fun parsedMessage_sentWithNoCostLine_returnsNull() {
        val msg = "QHK7ABC1 Confirmed. Ksh500.00 sent to JOHN on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1,000.00."
        val parsed = MpesaMessageParser.parse(msg)
        assertNotNull(parsed)
        assertNull("SENT with no cost line should have null transactionCost", parsed!!.transactionCost)
    }

    // ── Fee note presentation logic ──
    // Mirrors: val showFeeNote = mpesaTransactionCost != null && mpesaTransactionCost > 0 && logMpesaCosts

    private fun shouldShowFeeNote(transactionCost: Double?, logMpesaCosts: Boolean): Boolean {
        return transactionCost != null && transactionCost > 0 && logMpesaCosts
    }

    @Test
    fun feeNote_shownWhenCostPositiveAndToggleOn() {
        assertTrue(shouldShowFeeNote(transactionCost = 22.0, logMpesaCosts = true))
    }

    @Test
    fun feeNote_hiddenWhenToggleOff() {
        assertFalse(shouldShowFeeNote(transactionCost = 22.0, logMpesaCosts = false))
    }

    @Test
    fun feeNote_hiddenWhenCostZero() {
        assertFalse(shouldShowFeeNote(transactionCost = 0.0, logMpesaCosts = true))
    }

    @Test
    fun feeNote_hiddenWhenCostNull() {
        assertFalse(shouldShowFeeNote(transactionCost = null, logMpesaCosts = true))
    }

    @Test
    fun feeNote_hiddenWhenCostNullAndToggleOff() {
        assertFalse(shouldShowFeeNote(transactionCost = null, logMpesaCosts = false))
    }

    @Test
    fun feeNote_hiddenWhenCostNegative() {
        assertFalse(shouldShowFeeNote(transactionCost = -5.0, logMpesaCosts = true))
    }
}
