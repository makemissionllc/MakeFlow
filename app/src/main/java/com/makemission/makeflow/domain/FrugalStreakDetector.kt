package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class FrugalStreak(
    val categoryId: String,
    val categoryName: String,
    val streakLengthMonths: Int,
    val totalSavedVsAverage: Double,
    val projectedAnnualSavings: Double
)

object FrugalStreakDetector {
    private const val EVALUATION_MONTHS = 2
    private const val MAX_TRAILING_MONTHS = 6
    private const val MIN_HISTORY_MONTHS = 3
    private const val FRUGAL_THRESHOLD = 0.70

    /**
     * Detects a frugal streak for a category: the 2 most recent completed
     * months are each < 70% of the trailing average (up to 6 months before
     * those 2 months, including zeros).
     *
     * Future: extend to N-month streaks by walking backward as long as each
     * month qualifies.
     */
    fun detect(
        categoryId: String,
        categoryName: String,
        categoryTransactions: List<TransactionEntity>,
        now: Long = System.currentTimeMillis()
    ): FrugalStreak? {
        val currentMonth = YearMonth.from(
            Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        )

        val month1 = currentMonth.minusMonths(1)
        val month2 = currentMonth.minusMonths(2)

        val trailingMonths = (3..(2 + MAX_TRAILING_MONTHS)).map { currentMonth.minusMonths(it.toLong()) }
        val evaluatedMonths = listOf(month1, month2)

        fun monthSpend(month: YearMonth): Double {
            val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val end = month.atEndOfMonth().atTime(23, 59, 59, 999999999)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return categoryTransactions.filter { it.timestamp in start..end }.sumOf { it.amount }
        }

        val evaluatedSpends = evaluatedMonths.map(::monthSpend)
        val trailingSpends = trailingMonths.map(::monthSpend)

        val trailingMonthsWithHistory = trailingSpends.count { it > 0.0 }
        if (trailingMonthsWithHistory < 1) return null

        val totalMonthsWithHistory = trailingMonthsWithHistory + evaluatedSpends.count { it > 0.0 }
        if (totalMonthsWithHistory < MIN_HISTORY_MONTHS) return null

        val trailingAverage = trailingSpends.average()
        if (trailingAverage == 0.0) return null

        if (evaluatedSpends[0] >= trailingAverage * FRUGAL_THRESHOLD) return null
        if (evaluatedSpends[1] >= trailingAverage * FRUGAL_THRESHOLD) return null

        val totalSaved = (trailingAverage * EVALUATION_MONTHS) - evaluatedSpends.sum()

        return FrugalStreak(
            categoryId = categoryId,
            categoryName = categoryName,
            streakLengthMonths = EVALUATION_MONTHS,
            totalSavedVsAverage = totalSaved,
            projectedAnnualSavings = totalSaved / EVALUATION_MONTHS * 12
        )
    }
}
