package com.makemission.makeflow.domain

import kotlin.math.sqrt

data class SavingsCapacitySuggestion(
    val suggestedAmount: Double,
    val safeFloor: Double,
    val confidence: Float,
    val monthCount: Int
)

data class MonthlySurplus(
    val income: Double,
    val expense: Double
)

object SavingsCapacityDetector {
    private const val MIN_MONTHS = 3
    private const val MAX_MONTHS = 6
    private const val SAVINGS_RATIO = 0.8
    private const val MIN_CONFIDENCE = 0.4f
    private const val MAX_CONFIDENCE = 1.0f

    fun compute(monthlySurpluses: List<MonthlySurplus>): SavingsCapacitySuggestion? {
        if (monthlySurpluses.size < MIN_MONTHS) return null

        val surpluses = monthlySurpluses.map { it.income - it.expense }
        val safeFloor = surpluses.minOrNull() ?: return null
        if (safeFloor <= 0.0) return null

        val mean = surpluses.average()
        val stdDev = if (surpluses.size > 1) {
            sqrt(surpluses.sumOf { (it - mean).let { d -> d * d } } / surpluses.size)
        } else 0.0

        val cv = if (mean > 0.0) stdDev / mean else 1.0
        val consistencyConfidence = (1.0 - cv).coerceIn(0.0, 1.0)
        val sampleConfidence =
            (monthlySurpluses.size.toFloat() / MAX_MONTHS).coerceIn(MIN_CONFIDENCE, MAX_CONFIDENCE)

        return SavingsCapacitySuggestion(
            suggestedAmount = safeFloor * SAVINGS_RATIO,
            safeFloor = safeFloor,
            confidence = (consistencyConfidence * sampleConfidence).toFloat(),
            monthCount = monthlySurpluses.size
        )
    }
}
