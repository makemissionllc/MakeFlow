package com.makemission.makeflow.domain

import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CashFlowProjectorTest {

    private fun localDateToEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `projection with upcoming recurring income and expense nets correctly`() {
        val today = LocalDate.of(2026, 7, 15)
        val patterns = listOf(
            RecurringPattern(
                normalizedDescription = "salary deposit",
                avgAmount = 5000.0,
                intervalDays = 30,
                lastSeenDate = 0L,
                nextExpectedDate = localDateToEpochMillis(LocalDate.of(2026, 7, 20)),
                confidence = 0.9f,
                transactionType = "INCOME"
            ),
            RecurringPattern(
                normalizedDescription = "netflix subscription",
                avgAmount = 15.0,
                intervalDays = 30,
                lastSeenDate = 0L,
                nextExpectedDate = localDateToEpochMillis(LocalDate.of(2026, 7, 25)),
                confidence = 0.9f,
                transactionType = "EXPENSE"
            )
        )
        val txns = listOf(
            TransactionEntity(amount = 100.0, type = "EXPENSE", note = "groceries", timestamp = 0L),
            TransactionEntity(amount = 50.0, type = "EXPENSE", note = "dinner out", timestamp = 0L)
        )
        val result = CashFlowProjector.project(
            currentBalance = 1000.0,
            allTransactionsThisMonth = txns,
            recurringPatterns = patterns,
            today = today
        )
        assertEquals(1000.0, result.currentBalance, 0.01)
        assertEquals(16, result.daysRemainingInMonth)
        val expectedProjected = 1000.0 + 5000.0 - 15.0 - ((150.0 / 15) * 16)
        assertEquals(expectedProjected, result.projectedEndOfMonthBalance, 0.01)
    }

    @Test
    fun `discretionary-only projection with no recurring patterns`() {
        val today = LocalDate.of(2026, 7, 10)
        val txns = listOf(
            TransactionEntity(amount = 200.0, type = "EXPENSE", note = "groceries", timestamp = 0L),
            TransactionEntity(amount = 100.0, type = "EXPENSE", note = "gas", timestamp = 0L)
        )
        val result = CashFlowProjector.project(
            currentBalance = 500.0,
            allTransactionsThisMonth = txns,
            recurringPatterns = emptyList(),
            today = today
        )
        val avgDaily = 300.0 / 10
        val projectedDisc = avgDaily * 21
        assertEquals(500.0 - projectedDisc, result.projectedEndOfMonthBalance, 0.01)
        assertEquals(0.0, result.upcomingRecurringTotal, 0.01)
    }

    @Test
    fun `divide-by-zero guard on day 1`() {
        val today = LocalDate.of(2026, 7, 1)
        val result = CashFlowProjector.project(
            currentBalance = 1000.0,
            allTransactionsThisMonth = emptyList(),
            recurringPatterns = emptyList(),
            today = today
        )
        assertEquals(1000.0, result.projectedEndOfMonthBalance, 0.01)
        assertEquals(0.0, result.projectedDiscretionarySpend, 0.01)
        assertEquals(30, result.daysRemainingInMonth)
    }

    @Test
    fun `negative projected balance case`() {
        val today = LocalDate.of(2026, 7, 20)
        // Both transaction notes differ from the recurring pattern so they're discretionary
        val txns = listOf(
            TransactionEntity(amount = 5000.0, type = "EXPENSE", note = "one-time shopping", timestamp = 0L),
            TransactionEntity(amount = 3000.0, type = "EXPENSE", note = "emergency repair", timestamp = 0L)
        )
        val patterns = listOf(
            RecurringPattern(
                normalizedDescription = "rent",
                avgAmount = 3000.0,
                intervalDays = 30,
                lastSeenDate = 0L,
                nextExpectedDate = localDateToEpochMillis(LocalDate.of(2026, 7, 25)),
                confidence = 0.9f,
                transactionType = "EXPENSE"
            )
        )
        val result = CashFlowProjector.project(
            currentBalance = 200.0,
            allTransactionsThisMonth = txns,
            recurringPatterns = patterns,
            today = today
        )
        val avgDaily = 8000.0 / 20
        val projectedDisc = avgDaily * 11
        val expectedProjected = 200.0 - 3000.0 - projectedDisc
        assertEquals(expectedProjected, result.projectedEndOfMonthBalance, 0.01)
        assertEquals(11, result.daysRemainingInMonth)
    }
}
