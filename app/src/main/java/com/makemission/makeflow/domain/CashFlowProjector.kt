package com.makemission.makeflow.domain

import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CashFlowProjection(
    val currentBalance: Double,
    val projectedEndOfMonthBalance: Double,
    val upcomingRecurringTotal: Double,
    val projectedDiscretionarySpend: Double,
    val daysRemainingInMonth: Int
)

object CashFlowProjector {
    fun project(
        currentBalance: Double,
        allTransactionsThisMonth: List<TransactionEntity>,
        recurringPatterns: List<RecurringPattern>,
        today: LocalDate
    ): CashFlowProjection {
        val daysElapsed = today.dayOfMonth
        val daysInMonth = today.lengthOfMonth()
        val daysRemaining = (daysInMonth - daysElapsed).coerceAtLeast(0)

        val recurringDescriptions = recurringPatterns.map { it.normalizedDescription }.toSet()

        val discretionaryTxns = allTransactionsThisMonth.filter {
            it.type == "EXPENSE" &&
            normalize(it.note) !in recurringDescriptions
        }
        val discretionarySpendSoFar = discretionaryTxns.sumOf { it.amount }
        val avgDailyDiscretionary = if (daysElapsed > 0) discretionarySpendSoFar / daysElapsed else 0.0
        val projectedDiscretionaryRemaining = avgDailyDiscretionary * daysRemaining

        val upcomingRecurring = recurringPatterns
            .filter { pattern ->
                val nextDate = Instant.ofEpochMilli(pattern.nextExpectedDate)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                !nextDate.isBefore(today) && !nextDate.isAfter(today.withDayOfMonth(daysInMonth))
            }
            .sumOf { pattern ->
                if (pattern.transactionType == "INCOME") pattern.avgAmount else -pattern.avgAmount
            }

        val projected = currentBalance + upcomingRecurring - projectedDiscretionaryRemaining

        return CashFlowProjection(
            currentBalance = currentBalance,
            projectedEndOfMonthBalance = projected,
            upcomingRecurringTotal = upcomingRecurring,
            projectedDiscretionarySpend = projectedDiscretionaryRemaining,
            daysRemainingInMonth = daysRemaining
        )
    }

    private fun normalize(description: String): String =
        description.trim().lowercase().replace(Regex("\\s+"), " ")
}
