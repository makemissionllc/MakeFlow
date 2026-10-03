package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.BudgetEntity

enum class BudgetForecastStatus {
    ON_TRACK,
    OFF_TRACK,
    OVERSPENT
}

data class BudgetForecast(
    val budgetId: String,
    val categoryName: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val status: BudgetForecastStatus,
    val percentSpent: Float,
    val percentElapsed: Float,
    val projectedOverspend: Double?
) {
    val remainingDays: Int
        get() = ((1f - percentElapsed) * 30).toInt().coerceAtLeast(0)
}

object BudgetForecaster {

    private const val OFF_TRACK_THRESHOLD = 1.2f

    fun forecast(
        budget: BudgetEntity,
        periodStartMillis: Long,
        periodEndMillis: Long,
        nowMillis: Long
    ): BudgetForecast {
        val totalDuration = periodEndMillis - periodStartMillis
        val elapsed = (nowMillis - periodStartMillis).coerceAtLeast(0L)
        val percentElapsed = if (totalDuration > 0L) {
            (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
        } else {
            1f
        }

        val percentSpent = if (budget.amount > 0.0) {
            (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, Float.MAX_VALUE)
        } else if (budget.spentAmount > 0.0) {
            Float.MAX_VALUE
        } else {
            0f
        }

        val status = when {
            percentSpent >= 1f -> BudgetForecastStatus.OVERSPENT
            percentElapsed <= 0f -> BudgetForecastStatus.ON_TRACK
            percentSpent > percentElapsed * OFF_TRACK_THRESHOLD -> BudgetForecastStatus.OFF_TRACK
            else -> BudgetForecastStatus.ON_TRACK
        }

        val projectedOverspend = if (status == BudgetForecastStatus.OFF_TRACK && percentElapsed > 0f) {
            val projectedTotal = budget.spentAmount / percentElapsed.toDouble()
            if (projectedTotal > budget.amount) projectedTotal - budget.amount else null
        } else if (status == BudgetForecastStatus.OVERSPENT) {
            budget.spentAmount - budget.amount
        } else {
            null
        }

        return BudgetForecast(
            budgetId = budget.id,
            categoryName = budget.categoryName,
            budgetAmount = budget.amount,
            spentAmount = budget.spentAmount,
            status = status,
            percentSpent = percentSpent,
            percentElapsed = percentElapsed,
            projectedOverspend = projectedOverspend
        )
    }
}
