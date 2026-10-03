package com.makemission.makeflow.domain

import kotlin.math.sqrt

data class AnomalyResult(
    val isAnomalous: Boolean,
    val zScore: Double,
    val categoryAverage: Double,
    val categoryStdDev: Double,
    val severity: AnomalySeverity
)

enum class AnomalySeverity { NONE, MODERATE, HIGH }

object AnomalyDetector {
    private const val MIN_HISTORY_SIZE = 5
    private const val MODERATE_Z_THRESHOLD = 2.0
    private const val HIGH_Z_THRESHOLD = 3.0

    fun analyze(
        newTransactionAmount: Double,
        categoryHistory: List<Double>
    ): AnomalyResult {
        if (categoryHistory.size < MIN_HISTORY_SIZE) {
            return AnomalyResult(false, 0.0, 0.0, 0.0, AnomalySeverity.NONE)
        }

        val mean = categoryHistory.average()
        val stdDev = sqrt(categoryHistory.sumOf { (it - mean) * (it - mean) } / categoryHistory.size)

        if (stdDev == 0.0) {
            val isOff = newTransactionAmount != mean
            return AnomalyResult(
                isAnomalous = isOff,
                zScore = if (isOff) HIGH_Z_THRESHOLD else 0.0,
                categoryAverage = mean,
                categoryStdDev = 0.0,
                severity = if (isOff) AnomalySeverity.MODERATE else AnomalySeverity.NONE
            )
        }

        val zScore = (newTransactionAmount - mean) / stdDev
        val severity = when {
            zScore >= HIGH_Z_THRESHOLD -> AnomalySeverity.HIGH
            zScore >= MODERATE_Z_THRESHOLD -> AnomalySeverity.MODERATE
            else -> AnomalySeverity.NONE
        }

        return AnomalyResult(
            isAnomalous = severity != AnomalySeverity.NONE,
            zScore = zScore,
            categoryAverage = mean,
            categoryStdDev = stdDev,
            severity = severity
        )
    }
}
