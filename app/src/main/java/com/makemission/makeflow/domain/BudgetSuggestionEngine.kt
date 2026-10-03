package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.dao.MonthlyCategorySpend

data class BudgetSuggestion(
    val categoryId: String,
    val categoryName: String,
    val suggestedAmount: Double,
    val currentBudgetAmount: Double?,
    val reason: SuggestionReason,
    val confidence: Float
)

enum class SuggestionReason {
    NO_BUDGET_SET,
    CONSISTENTLY_OVER,
    CONSISTENTLY_UNDER
}

object BudgetSuggestionEngine {
    private const val BUFFER_MULTIPLIER = 1.10
    private const val UNDER_THRESHOLD = 0.70
    private const val MIN_MONTHS_FOR_SUGGESTION = 2

    fun suggest(
        categoryId: String,
        categoryName: String,
        monthlySpend: List<MonthlyCategorySpend>,
        currentBudget: Double?
    ): BudgetSuggestion? {
        if (monthlySpend.size < MIN_MONTHS_FOR_SUGGESTION) return null

        val avgSpend = monthlySpend.map { it.totalSpent }.average()
        if (avgSpend == 0.0) return null

        val suggested = avgSpend * BUFFER_MULTIPLIER

        if (currentBudget == null) {
            return BudgetSuggestion(
                categoryId, categoryName, suggested, null,
                SuggestionReason.NO_BUDGET_SET,
                confidence = confidenceFromSampleSize(monthlySpend.size)
            )
        }

        val recentMonths = monthlySpend.takeLast(MIN_MONTHS_FOR_SUGGESTION)
        val allOver = recentMonths.all { it.totalSpent > currentBudget }
        val allUnder = recentMonths.all { it.totalSpent < currentBudget * UNDER_THRESHOLD }

        return when {
            allOver -> BudgetSuggestion(
                categoryId, categoryName, suggested, currentBudget,
                SuggestionReason.CONSISTENTLY_OVER,
                confidence = confidenceFromSampleSize(monthlySpend.size)
            )
            allUnder -> BudgetSuggestion(
                categoryId, categoryName, suggested, currentBudget,
                SuggestionReason.CONSISTENTLY_UNDER,
                confidence = confidenceFromSampleSize(monthlySpend.size)
            )
            else -> null
        }
    }

    private fun confidenceFromSampleSize(months: Int): Float =
        (months / 3f).coerceIn(0.4f, 1.0f)

    fun fillMissingMonths(
        raw: List<MonthlyCategorySpend>,
        startYearMonth: String,
        endYearMonth: String
    ): List<MonthlyCategorySpend> {
        val dateMap = raw.associateBy { it.yearMonth }
        val result = mutableListOf<MonthlyCategorySpend>()
        var current = startYearMonth
        while (current <= endYearMonth) {
            result.add(dateMap[current] ?: MonthlyCategorySpend(current, 0.0))
            current = nextYearMonth(current)
        }
        return result
    }

    private fun nextYearMonth(ym: String): String {
        val parts = ym.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val m = if (month == 12) 1 else month + 1
        val y = if (month == 12) year + 1 else year
        return "%04d-%02d".format(y, m)
    }

    private operator fun String.compareTo(other: String): Int {
        val a = this.split("-")
        val b = other.split("-")
        val ay = a[0].toInt()
        val am = a[1].toInt()
        val by = b[0].toInt()
        val bm = b[1].toInt()
        return when {
            ay != by -> ay.compareTo(by)
            else -> am.compareTo(bm)
        }
    }
}
