package com.makemission.makeflow.data

import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

object RecurringDetector {
    private const val MIN_OCCURRENCES = 3
    private const val INTERVAL_VARIANCE_THRESHOLD = 0.25
    private const val AMOUNT_VARIANCE_THRESHOLD = 0.10

    fun detect(transactions: List<TransactionEntity>): List<RecurringPattern> {
        if (transactions.size < MIN_OCCURRENCES) return emptyList()

        return transactions
            .groupBy { normalize(it.note) }
            .filter { it.value.size >= MIN_OCCURRENCES }
            .mapNotNull { (normalizedDesc, txns) ->
                analyzeGroup(normalizedDesc, txns.sortedBy { it.timestamp })
            }
    }

    private fun normalize(description: String): String =
        description.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun analyzeGroup(desc: String, sorted: List<TransactionEntity>): RecurringPattern? {
        val amounts = sorted.map { it.amount }
        val avgAmount = amounts.average()
        if (avgAmount == 0.0) return null
        val amountStdDev = stdDev(amounts, avgAmount)
        if (amountStdDev / avgAmount > AMOUNT_VARIANCE_THRESHOLD) return null

        val uniqueDays = sorted.distinctBy {
            Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
        if (uniqueDays.size < MIN_OCCURRENCES) return null

        val intervals = uniqueDays.zipWithNext { a, b ->
            ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(a.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                Instant.ofEpochMilli(b.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            ).toDouble()
        }
        val avgInterval = intervals.average()
        if (avgInterval == 0.0) return null
        val intervalStdDev = stdDev(intervals, avgInterval)
        if (intervalStdDev / avgInterval > INTERVAL_VARIANCE_THRESHOLD) return null

        val confidence = (1.0 - (intervalStdDev / avgInterval)).coerceIn(0.0, 1.0).toFloat()
        val lastDate = uniqueDays.last().timestamp
        val nextExpected = Instant.ofEpochMilli(lastDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(avgInterval.roundToLong())

        return RecurringPattern(
            normalizedDescription = desc,
            avgAmount = avgAmount,
            intervalDays = avgInterval.roundToInt(),
            lastSeenDate = lastDate,
            nextExpectedDate = nextExpected.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            confidence = confidence,
            categoryId = sorted.last().categoryId,
            transactionType = sorted.last().type
        )
    }

    private fun stdDev(values: List<Double>, mean: Double): Double =
        sqrt(values.sumOf { (it - mean).let { d -> d * d } } / values.size)
}
