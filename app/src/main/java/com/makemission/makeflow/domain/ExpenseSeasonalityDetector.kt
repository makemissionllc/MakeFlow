package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.sqrt

data class SeasonalPattern(
    val categoryId: String,
    val categoryName: String,
    val peakMonths: List<Int>,
    val avgPeakAmount: Double,
    val patternType: PatternType,
    val confidence: Float
)

enum class PatternType {
    SEASONAL_SPIKE,
    ANNUAL_BILL
}

object ExpenseSeasonalityDetector {
    private const val MIN_SPAN_MONTHS = 12
    private const val MIN_YEARS_FOR_ANNUAL = 2
    private const val STDDEV_THRESHOLD = 2.0
    private const val BILL_DOMINANCE = 0.80
    private const val MIN_YEARS_FOR_CONFIDENCE = 2

    fun detect(
        categoryId: String,
        categoryName: String,
        categoryTransactions: List<TransactionEntity>,
        now: Long = System.currentTimeMillis()
    ): List<SeasonalPattern> {
        val zone = ZoneId.systemDefault()

        if (categoryTransactions.isEmpty()) return emptyList()

        val allMonths = categoryTransactions.map { tx ->
            YearMonth.from(Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate())
        }
        val minMonth = allMonths.minOrNull() ?: return emptyList()
        val maxMonth = allMonths.maxOrNull() ?: return emptyList()
        val spanMonths = (maxMonth.year - minMonth.year) * 12 +
            (maxMonth.monthValue - minMonth.monthValue) + 1
        if (spanMonths < MIN_SPAN_MONTHS) return emptyList()

        val monthlyTotals = categoryTransactions
            .groupBy { tx ->
                YearMonth.from(Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate())
            }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val activeMonths = monthlyTotals.filter { it.value > 0.0 }

        val results = mutableListOf<SeasonalPattern>()

        detectSeasonalSpikes(categoryId, categoryName, activeMonths, spanMonths)?.let { results.add(it) }

        detectAnnualBill(categoryId, categoryName, categoryTransactions, activeMonths, spanMonths)?.let { results.add(it) }

        return results
    }

    private fun detectSeasonalSpikes(
        categoryId: String,
        categoryName: String,
        activeMonths: Map<YearMonth, Double>,
        spanMonths: Int
    ): SeasonalPattern? {
        val totals = activeMonths.values.toList()
        if (totals.size < MIN_SPAN_MONTHS) return null

        val overallMean = totals.average()
        val overallStdDev = stdDev(totals, overallMean)
        if (overallStdDev == 0.0) return null

        val calendarGroups = activeMonths.entries
            .groupBy({ it.key.monthValue }, { it.value })

        val spikeMonths = calendarGroups.mapValues { (_, values) ->
            values.average()
        }.filter { (_, monthMean) ->
            monthMean > overallMean + STDDEV_THRESHOLD * overallStdDev
        }

        if (spikeMonths.isEmpty()) return null

        val yearsObserved = calendarGroups
            .filterKeys { it in spikeMonths.keys }
            .values.minOfOrNull { it.size } ?: 1

        val avgPeakAmount = spikeMonths.values.sum() / spikeMonths.size
        val confidence = computeConfidence(yearsObserved, spanMonths)

        return SeasonalPattern(
            categoryId = categoryId,
            categoryName = categoryName,
            peakMonths = spikeMonths.keys.sorted(),
            avgPeakAmount = avgPeakAmount,
            patternType = PatternType.SEASONAL_SPIKE,
            confidence = confidence
        )
    }

    private fun detectAnnualBill(
        categoryId: String,
        categoryName: String,
        categoryTransactions: List<TransactionEntity>,
        activeMonths: Map<YearMonth, Double>,
        spanMonths: Int
    ): SeasonalPattern? {
        val zone = ZoneId.systemDefault()

        val yearGroups = categoryTransactions.groupBy { tx ->
            YearMonth.from(Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()).year
        }
        if (yearGroups.size < MIN_YEARS_FOR_ANNUAL) return null

        data class BillCandidate(val month: Int, val amount: Double)

        val annualBills = yearGroups.mapValues { (_, txs) ->
            val monthGroups = txs.groupBy { tx ->
                YearMonth.from(Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()).monthValue
            }
            monthGroups.mapValues { (_, monthTxs) ->
                val total = monthTxs.sumOf { it.amount }
                val maxTx = monthTxs.maxByOrNull { it.amount }
                Pair(total, maxTx)
            }.filter { (_, pair) ->
                val (total, maxTx) = pair
                maxTx != null && maxTx!!.amount >= total * BILL_DOMINANCE
            }.mapValues { (_, pair) ->
                pair.first
            }
        }

        val candidates = mutableListOf<BillCandidate>()
        for (year in yearGroups.keys.sorted()) {
            val yearData = annualBills[year] ?: continue
            if (yearData.isEmpty()) continue
            val topMonth = yearData.maxByOrNull { it.value } ?: continue
            candidates.add(BillCandidate(topMonth.key, topMonth.value))
        }

        if (candidates.size < MIN_YEARS_FOR_ANNUAL) return null

        val months = candidates.map { it.month }
        val medianMonth = months.sorted().let { sorted -> sorted[sorted.size / 2] }
        val consistent = months.all { monthDiff(it, medianMonth) <= 1 }
        if (!consistent) return null

        val avgAmount = candidates.map { it.amount }.average()
        val yearsObserved = candidates.size
        val confidence = computeConfidence(yearsObserved, spanMonths)

        return SeasonalPattern(
            categoryId = categoryId,
            categoryName = categoryName,
            peakMonths = listOf(medianMonth),
            avgPeakAmount = avgAmount,
            patternType = PatternType.ANNUAL_BILL,
            confidence = confidence
        )
    }

    private fun stdDev(values: List<Double>, mean: Double): Double {
        if (values.size <= 1) return 0.0
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    private fun monthDiff(a: Int, b: Int): Int {
        val diff = abs(a - b)
        return minOf(diff, 12 - diff)
    }

    private fun computeConfidence(yearsObserved: Int, spanMonths: Int): Float {
        val years = spanMonths / 12
        val effectiveYears = minOf(yearsObserved, years)
        if (effectiveYears < MIN_YEARS_FOR_CONFIDENCE) return 0.0f
        return ((effectiveYears - 1) * 0.15f + 0.3f).coerceAtMost(1.0f)
    }
}
