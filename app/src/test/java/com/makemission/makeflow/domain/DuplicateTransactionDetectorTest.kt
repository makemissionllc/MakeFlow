package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DuplicateTransactionDetectorTest {

    private val baseTimestamp = 1_000_000_000_000L

    private fun tx(
        id: String = "tx1",
        amount: Double = 50.0,
        type: String = "EXPENSE",
        accountId: String = "acct1",
        fromAccountId: String? = null,
        toAccountId: String? = null,
        note: String = "",
        timestamp: Long = baseTimestamp
    ) = TransactionEntity(
        id = id, amount = amount, type = type,
        accountId = accountId, fromAccountId = fromAccountId,
        toAccountId = toAccountId, note = note, timestamp = timestamp
    )

    @Test
    fun `exact duplicate within window is flagged`() {
        val candidate = tx(id = "candidate", timestamp = baseTimestamp)
        val existing = tx(id = "existing", amount = 50.0, type = "EXPENSE", accountId = "acct1", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNotNull(result)
        assertEquals("existing", result!!.matchingTransaction.id)
        assertEquals(1, result.timeDeltaMinutes)
    }

    @Test
    fun `same details but outside time window is not flagged`() {
        val candidate = tx(id = "candidate", timestamp = baseTimestamp)
        val existing = tx(id = "existing", amount = 50.0, type = "EXPENSE", accountId = "acct1", timestamp = baseTimestamp - 6 * 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }

    @Test
    fun `different amount is not flagged`() {
        val candidate = tx(id = "candidate", amount = 100.0, timestamp = baseTimestamp)
        val existing = tx(id = "existing", amount = 50.0, accountId = "acct1", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }

    @Test
    fun `different account is not flagged`() {
        val candidate = tx(id = "candidate", accountId = "acct1", timestamp = baseTimestamp)
        val existing = tx(id = "existing", accountId = "acct2", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }

    @Test
    fun `transfer duplicate detection uses from-to pair`() {
        val candidate = tx(id = "candidate", amount = 200.0, type = "TRANSFER", fromAccountId = "acct1", toAccountId = "acct2", timestamp = baseTimestamp)
        val existing = tx(id = "existing", amount = 200.0, type = "TRANSFER", fromAccountId = "acct1", toAccountId = "acct2", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNotNull(result)
        assertEquals("existing", result!!.matchingTransaction.id)
    }

    @Test
    fun `transfer with different from account is not flagged`() {
        val candidate = tx(id = "candidate", amount = 200.0, type = "TRANSFER", fromAccountId = "acct1", toAccountId = "acct2", timestamp = baseTimestamp)
        val existing = tx(id = "existing", amount = 200.0, type = "TRANSFER", fromAccountId = "acct3", toAccountId = "acct2", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }

    @Test
    fun `transfer with different to account is not flagged`() {
        val candidate = tx(id = "candidate", amount = 200.0, type = "TRANSFER", fromAccountId = "acct1", toAccountId = "acct2", timestamp = baseTimestamp)
        val existing = tx(id = "existing", amount = 200.0, type = "TRANSFER", fromAccountId = "acct1", toAccountId = "acct3", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }

    @Test
    fun `different types are not flagged`() {
        val candidate = tx(id = "candidate", type = "EXPENSE", amount = 50.0, accountId = "acct1", timestamp = baseTimestamp)
        val existing = tx(id = "existing", type = "INCOME", amount = 50.0, accountId = "acct1", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }

    @Test
    fun `empty transaction history never flags`() {
        val candidate = tx(timestamp = baseTimestamp)

        val result = DuplicateTransactionDetector.check(candidate, emptyList())

        assertNull(result)
    }

    @Test
    fun `flags against the candidate itself is skipped`() {
        val candidate = tx(id = "same", timestamp = baseTimestamp)

        val result = DuplicateTransactionDetector.check(candidate, listOf(candidate))

        assertNull(result)
    }

    @Test
    fun `duplicate note is not required`() {
        val candidate = tx(id = "candidate", note = "lunch today", timestamp = baseTimestamp)
        val existing = tx(id = "existing", note = "lunch yesterday", accountId = "acct1", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNotNull(result)
    }

    @Test
    fun `reason includes note when present`() {
        val candidate = tx(id = "candidate", amount = 25.0, note = "coffee", timestamp = baseTimestamp)
        val existing = tx(id = "existing", amount = 25.0, note = "morning coffee", accountId = "acct1", timestamp = baseTimestamp - 120_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNotNull(result)
        assertEquals("existing", result!!.matchingTransaction.id)
        assertEquals(2, result.timeDeltaMinutes)
        val mentionsNote = result.reason.contains("morning coffee")
        assert(mentionsNote) { "reason should include the note text: ${result.reason}" }
    }

    @Test
    fun `expense does not match transfer`() {
        val candidate = tx(id = "candidate", type = "EXPENSE", amount = 100.0, accountId = "acct1", timestamp = baseTimestamp)
        val existing = tx(id = "existing", type = "TRANSFER", amount = 100.0, accountId = "acct1", fromAccountId = "acct1", toAccountId = "acct2", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }

    @Test
    fun `transfer does not match expense`() {
        val candidate = tx(id = "candidate", type = "TRANSFER", amount = 100.0, fromAccountId = "acct1", toAccountId = "acct2", timestamp = baseTimestamp)
        val existing = tx(id = "existing", type = "EXPENSE", amount = 100.0, accountId = "acct1", timestamp = baseTimestamp - 60_000)

        val result = DuplicateTransactionDetector.check(candidate, listOf(existing))

        assertNull(result)
    }
}
