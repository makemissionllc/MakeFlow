package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class VelocityAlert(
    val categoryId: String,
    val categoryName: String,
    val severity: VelocitySeverity,
    val percentAhead: Double,
    val projectedMonthEndAmount: Double,
    val actualCumulativeSpend: Double,
    val expectedCumulativeSpend: Double
)

enum class VelocitySeverity { NONE, ELEVATED, HIGH, CRITICAL }

object SpendingVelocityTracker {
    private const val MIN_HISTORY_MONTHS = 3
    private const val TRAILING_MONTHS = 3
    private const val ELEVATED_THRESHOLD = 0.30
    private const val HIGH_THRESHOLD = 0.50
    private const val CRITICAL_THRESHOLD = 1.00
    private const val MIN_CONSECUTIVE_DAYS = 3

    fun compute(
        categoryId: String,
        categoryName: String,
        categoryTransactions: List<TransactionEntity>,
        now: Long = System.currentTimeMillis()
    ): VelocityAlert? {
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val currentMonth = YearMonth.from(today)
        val daysInMonth = currentMonth.lengthOfMonth()
        val daysElapsed = today.dayOfMonth

        if (daysElapsed < MIN_CONSECUTIVE_DAYS) return null

        val trailingMonths = (1..TRAILING_MONTHS).map { currentMonth.minusMonths(it.toLong()) }

        val monthlyTotals = trailingMonths.map { month ->
            val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val end = month.atEndOfMonth().atTime(23, 59, 59, 999999999)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            categoryTransactions.filter { it.timestamp in start..end }.sumOf { it.amount }
        }

        val monthsWithHistory = monthlyTotals.count { it > 0.0 }
        if (monthsWithHistory < MIN_HISTORY_MONTHS) return null

        val avgMonthlySpend = monthlyTotals.average()
        if (avgMonthlySpend == 0.0) return null

        val expectedCumulative = avgMonthlySpend * (daysElapsed.toDouble() / daysInMonth)

        val currentMonthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val actualCumulative = categoryTransactions.filter {
            it.timestamp in currentMonthStart..now
        }.sumOf { it.amount }

        if (expectedCumulative == 0.0) return null

        val percentAhead = (actualCumulative - expectedCumulative) / expectedCumulative

        if (!hasConsecutiveElevatedDays(
                categoryTransactions = categoryTransactions,
                avgMonthlySpend = avgMonthlySpend,
                currentMonth = currentMonth,
                daysInMonth = daysInMonth,
                daysElapsed = daysElapsed,
                currentMonthStart = currentMonthStart,
                now = now
            )
        ) return null

        val severity = when {
            percentAhead >= CRITICAL_THRESHOLD -> VelocitySeverity.CRITICAL
            percentAhead >= HIGH_THRESHOLD -> VelocitySeverity.HIGH
            percentAhead >= ELEVATED_THRESHOLD -> VelocitySeverity.ELEVATED
            else -> VelocitySeverity.NONE
        }

        if (severity == VelocitySeverity.NONE) return null

        val projectedMonthEnd =
            if (daysElapsed > 0) actualCumulative / (daysElapsed.toDouble() / daysInMonth) else 0.0

        return VelocityAlert(
            categoryId = categoryId,
            categoryName = categoryName,
            severity = severity,
            percentAhead = percentAhead,
            projectedMonthEndAmount = projectedMonthEnd,
            actualCumulativeSpend = actualCumulative,
            expectedCumulativeSpend = expectedCumulative
        )
    }

    private fun hasConsecutiveElevatedDays(
        categoryTransactions: List<TransactionEntity>,
        avgMonthlySpend: Double,
        currentMonth: YearMonth,
        daysInMonth: Int,
        daysElapsed: Int,
        currentMonthStart: Long,
        now: Long
    ): Boolean {
        var consecutiveCount = 0
        for (day in 1..daysElapsed) {
            val dayEnd = currentMonth.atDay(day).atTime(23, 59, 59, 999999999)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEndClamped = minOf(dayEnd, now)
            val cumUpToDay = categoryTransactions.filter {
                it.timestamp in currentMonthStart..dayEndClamped
            }.sumOf { it.amount }
            val expectedUpToDay = avgMonthlySpend * (day.toDouble() / daysInMonth)
            val pctAhead =
                if (expectedUpToDay > 0) (cumUpToDay - expectedUpToDay) / expectedUpToDay else 0.0

            if (pctAhead >= ELEVATED_THRESHOLD) {
                consecutiveCount++
                if (consecutiveCount >= MIN_CONSECUTIVE_DAYS) return true
            } else {
                consecutiveCount = 0
            }
        }
        return false
    }
}
