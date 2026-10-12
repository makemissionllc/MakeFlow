package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountActivityTest {
    private val transfer = TransactionEntity(id = "transfer", type = "TRANSFER", amount = 10.0,
        accountId = "cash", fromAccountId = "cash", toAccountId = "bank", destinationAmount = 1500.0)

    @Test fun transferUsesEachAccountsOwnAmountAndDirection() {
        val source = AccountActivity.recent(listOf(transfer), "cash").single()
        val destination = AccountActivity.recent(listOf(transfer), "bank").single()
        assertEquals(AccountActivity.Direction.TRANSFER_OUT, source.direction)
        assertEquals(10.0, source.amount)
        assertEquals(AccountActivity.Direction.TRANSFER_IN, destination.direction)
        assertEquals(1500.0, destination.amount)
    }

    @Test fun missingDestinationAmountIsNeverReplacedWithSourceAmount() {
        assertNull(AccountActivity.recent(listOf(transfer.copy(destinationAmount = null)), "bank").single().amount)
    }

    @Test fun legacyTransferFallsBackToAccountIdOnlyForSourceIdentity() {
        assertEquals(AccountActivity.Direction.TRANSFER_OUT,
            AccountActivity.recent(listOf(transfer.copy(fromAccountId = null)), "cash").single().direction)
        assertEquals(emptyList<AccountActivity.Entry>(), AccountActivity.recent(listOf(transfer.copy(accountId = "unrelated")), "unrelated"))
    }

    @Test fun filtersUnrelatedRecordsAndOrdersNewestFirstWithStableTiesAndLimit() {
        val transactions = listOf(
            TransactionEntity("old", 2.0, "EXPENSE", accountId = "cash", timestamp = 1),
            TransactionEntity("z", 3.0, "EXPENSE", accountId = "cash", timestamp = 2),
            TransactionEntity("a", 4.0, "INCOME", accountId = "cash", timestamp = 2),
            TransactionEntity("other", 5.0, "INCOME", accountId = "bank", timestamp = 3)
        )
        val entries = AccountActivity.recent(transactions, "cash", 2)
        assertEquals(listOf("a", "z"), entries.map { it.transaction.id })
        assertEquals(listOf(AccountActivity.Direction.INCOME, AccountActivity.Direction.EXPENSE), entries.map { it.direction })
    }

    @Test fun invalidAmountsAndUnknownTypesAreDisclosed() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0).forEach { amount ->
            assertNull(AccountActivity.recent(listOf(transfer.copy(amount = amount)), "cash").single().amount)
            assertNull(AccountActivity.recent(listOf(transfer.copy(destinationAmount = amount)), "bank").single().amount)
        }
        val unknown = AccountActivity.recent(listOf(transfer.copy(type = "UNKNOWN")), "cash").single()
        assertEquals(AccountActivity.Direction.UNAVAILABLE, unknown.direction)
        assertNull(unknown.amount)
    }

    @Test fun selfTransferDoesNotInventANetChange() {
        val entry = AccountActivity.recent(listOf(transfer.copy(toAccountId = "cash")), "cash").single()
        assertEquals(AccountActivity.Direction.UNAVAILABLE, entry.direction)
        assertNull(entry.amount)
    }

    @Test fun defaultLimitIsTwentyAndEmptyAccountHasNoActivity() {
        val records = (1..25).map { transfer.copy(id = "$it", timestamp = it.toLong()) }
        assertEquals(20, AccountActivity.recent(records, "cash").size)
        assertEquals(emptyList<AccountActivity.Entry>(), AccountActivity.recent(records, "missing"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonPositiveLimitIsRejected() { AccountActivity.recent(emptyList(), "cash", 0) }
}
