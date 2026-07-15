package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.SavingsGoalEntity

data class CategorySpendSummary(
    val categoryName: String,
    val monthlySpend: Double
)

data class CategoryReductionSuggestion(
    val categoryName: String,
    val currentMonthlySpend: Double,
    val reductionPercent: Float,
    val reductionAmount: Double
)

data class TradeOffSuggestion(
    val suggestions: List<CategoryReductionSuggestion>,
    val totalReductionPercent: Float
)

enum class FeasibilityStatus {
    FEASIBLE,
    NOT_FEASIBLE,
    INSUFFICIENT_DATA,
    PROJECTION_ONLY
}

data class GoalFeasibilityResult(
    val status: FeasibilityStatus,
    val confidence: Float,
    val monthlySurplus: Double,
    val safeFloor: Double,
    val requiredMonthlySave: Double?,
    val monthsRemaining: Int?,
    val revisedTimelineMonths: Int?,
    val tradeOffSuggestion: TradeOffSuggestion?
)

object GoalFeasibilityCalculator {
    private const val MAX_REDUCTION_CAP = 0.5
    private const val TOP_CATEGORIES_COUNT = 2
    private const val MONTH_MILLIS = 30L * 24 * 60 * 60 * 1000

    fun assess(
        goal: SavingsGoalEntity,
        currentProgress: Double,
        monthlySurpluses: List<MonthlySurplus>,
        categorySpending: List<CategorySpendSummary>,
        nowMillis: Long = System.currentTimeMillis()
    ): GoalFeasibilityResult {
        val capacity = SavingsCapacityDetector.compute(monthlySurpluses)
        if (capacity == null) {
            return GoalFeasibilityResult(
                status = FeasibilityStatus.INSUFFICIENT_DATA,
                confidence = 0f,
                monthlySurplus = 0.0,
                safeFloor = 0.0,
                requiredMonthlySave = null,
                monthsRemaining = null,
                revisedTimelineMonths = null,
                tradeOffSuggestion = null
            )
        }

        val surplus = capacity.safeFloor
        val remainingAmount = goal.targetAmount - currentProgress

        if (remainingAmount <= 0.0) {
            return GoalFeasibilityResult(
                status = FeasibilityStatus.FEASIBLE,
                confidence = capacity.confidence,
                monthlySurplus = surplus,
                safeFloor = capacity.safeFloor,
                requiredMonthlySave = 0.0,
                monthsRemaining = null,
                revisedTimelineMonths = 0,
                tradeOffSuggestion = null
            )
        }

        val projectedMonths = kotlin.math.ceil(remainingAmount / surplus).toInt()

        if (goal.targetDate == null) {
            return GoalFeasibilityResult(
                status = FeasibilityStatus.PROJECTION_ONLY,
                confidence = capacity.confidence,
                monthlySurplus = surplus,
                safeFloor = capacity.safeFloor,
                requiredMonthlySave = null,
                monthsRemaining = null,
                revisedTimelineMonths = projectedMonths,
                tradeOffSuggestion = null
            )
        }

        val monthsRemaining = ((goal.targetDate - nowMillis) / MONTH_MILLIS).toInt()
            .coerceAtLeast(0)

        if (monthsRemaining <= 0) {
            return GoalFeasibilityResult(
                status = FeasibilityStatus.NOT_FEASIBLE,
                confidence = capacity.confidence,
                monthlySurplus = surplus,
                safeFloor = capacity.safeFloor,
                requiredMonthlySave = remainingAmount,
                monthsRemaining = 0,
                revisedTimelineMonths = projectedMonths,
                tradeOffSuggestion = null
            )
        }

        val requiredPerMonth = remainingAmount / monthsRemaining

        if (surplus >= requiredPerMonth) {
            return GoalFeasibilityResult(
                status = FeasibilityStatus.FEASIBLE,
                confidence = capacity.confidence,
                monthlySurplus = surplus,
                safeFloor = capacity.safeFloor,
                requiredMonthlySave = requiredPerMonth,
                monthsRemaining = monthsRemaining,
                revisedTimelineMonths = monthsRemaining,
                tradeOffSuggestion = null
            )
        }

        val monthlyShortfall = requiredPerMonth - surplus
        val tradeOff = computeTradeOffSuggestion(
            categorySpending = categorySpending,
            requiredMonthlyReduction = monthlyShortfall
        )

        return GoalFeasibilityResult(
            status = FeasibilityStatus.NOT_FEASIBLE,
            confidence = capacity.confidence,
            monthlySurplus = surplus,
            safeFloor = capacity.safeFloor,
            requiredMonthlySave = requiredPerMonth,
            monthsRemaining = monthsRemaining,
            revisedTimelineMonths = projectedMonths,
            tradeOffSuggestion = tradeOff
        )
    }

    private fun computeTradeOffSuggestion(
        categorySpending: List<CategorySpendSummary>,
        requiredMonthlyReduction: Double
    ): TradeOffSuggestion? {
        val topCategories = categorySpending
            .sortedByDescending { it.monthlySpend }
            .take(TOP_CATEGORIES_COUNT)

        if (topCategories.isEmpty()) return null

        var remaining = requiredMonthlyReduction
        val suggestions = mutableListOf<CategoryReductionSuggestion>()

        for (cat in topCategories) {
            if (remaining <= 0.0) break
            val idealPercent = remaining / cat.monthlySpend
            val cappedPercent = idealPercent.coerceAtMost(MAX_REDUCTION_CAP)
            if (cappedPercent <= 0.0) continue
            val reductionAmount = cat.monthlySpend * cappedPercent
            suggestions.add(
                CategoryReductionSuggestion(
                    categoryName = cat.categoryName,
                    currentMonthlySpend = cat.monthlySpend,
                    reductionPercent = cappedPercent.toFloat(),
                    reductionAmount = reductionAmount
                )
            )
            remaining -= reductionAmount
        }

        if (remaining > 0.01) return null

        val totalPct = suggestions.sumOf { it.reductionPercent.toDouble() }.toFloat()
        return TradeOffSuggestion(
            suggestions = suggestions,
            totalReductionPercent = totalPct
        )
    }
}
