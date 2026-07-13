package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId

class ExpenseSeasonalityDetectorTest {

    private val catId = "cat_gifts"
    private val catName = "Gifts"

    private fun utcMillis(year: Int, month: Int, day: Int): Long =
        YearMonth.of(year, month).atDay(day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

    private fun tx(amount: Double, year: Int, month: Int, day: Int): TransactionEntity =
        TransactionEntity(amount = amount, type = "EXPENSE", categoryId = catId, timestamp = utcMillis(year, month, day))

    @Test
    fun `december spike over 2 years detected as SEASONAL_SPIKE`() {
        val txs = mutableListOf<TransactionEntity>()
        for (year in 2024..2025) {
            for (m in 1..11) {
                txs.add(tx(100.0, year, m, 15))
            }
            txs.add(tx(500.0, year, 12, 20))
        }
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        assertTrue("Expected at least 1 pattern", results.isNotEmpty())
        val spike = results.find { it.patternType == PatternType.SEASONAL_SPIKE }
        assertTrue("Expected SEASONAL_SPIKE pattern", spike != null)
        assertTrue(spike!!.peakMonths.any { it == 12 })
        assertEquals(catId, spike.categoryId)
        assertEquals(catName, spike.categoryName)
    }

    @Test
    fun `annual bill over 3 years detected as ANNUAL_BILL`() {
        val txs = listOf(
            tx(1000.0, 2023, 3, 10),
            tx(1050.0, 2024, 3, 12),
            tx(1100.0, 2025, 3, 8)
        )
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        assertEquals(1, results.size)
        val pattern = results[0]
        assertEquals(PatternType.ANNUAL_BILL, pattern.patternType)
        assertEquals(listOf(3), pattern.peakMonths)
        assertEquals(1050.0, pattern.avgPeakAmount, 1.0)
    }

    @Test
    fun `less than 12 months span returns empty list`() {
        val txs = (1..11).map { m -> tx(100.0, 2025, m, 15) }
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `flat consistent spending returns no false positive`() {
        val txs = (1..12).map { m -> tx(100.0, 2025, m, 15) }
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `confidence increases with more years of data`() {
        val twoYearTxs = mutableListOf<TransactionEntity>()
        for (y in 2024..2025) {
            for (m in 1..11) { twoYearTxs.add(tx(100.0, y, m, 15)) }
            twoYearTxs.add(tx(500.0, y, 12, 20))
        }
        val twoYearResults = ExpenseSeasonalityDetector.detect(catId, catName, twoYearTxs)
        val twoYearSpike = twoYearResults.find { it.patternType == PatternType.SEASONAL_SPIKE }
        assertTrue("Expected SEASONAL_SPIKE in 2-year data", twoYearSpike != null)

        val fourYearTxs = mutableListOf<TransactionEntity>()
        for (y in 2022..2025) {
            for (m in 1..11) { fourYearTxs.add(tx(100.0, y, m, 15)) }
            fourYearTxs.add(tx(500.0, y, 12, 20))
        }
        val fourYearResults = ExpenseSeasonalityDetector.detect(catId, catName, fourYearTxs)
        val fourYearSpike = fourYearResults.find { it.patternType == PatternType.SEASONAL_SPIKE }
        assertTrue("Expected SEASONAL_SPIKE in 4-year data", fourYearSpike != null)

        assertTrue(
            "Expected 4-year confidence ${fourYearSpike!!.confidence} > 2-year confidence ${twoYearSpike!!.confidence}",
            fourYearSpike.confidence > twoYearSpike.confidence
        )
    }

    @Test
    fun `annual bill with month shift plus minus 1 still detected`() {
        val txs = listOf(
            tx(1000.0, 2023, 3, 10),
            tx(1050.0, 2024, 2, 12),
            tx(1100.0, 2025, 3, 8)
        )
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        val bill = results.find { it.patternType == PatternType.ANNUAL_BILL }
        assertTrue("Expected ANNUAL_BILL pattern", bill != null)
    }

    @Test
    fun `annual bill with dec jan wrap detected`() {
        val txs = listOf(
            tx(1000.0, 2023, 12, 10),
            tx(1050.0, 2025, 1, 12)
        )
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        val bill = results.find { it.patternType == PatternType.ANNUAL_BILL }
        assertTrue("Expected ANNUAL_BILL pattern", bill != null)
    }

    @Test
    fun `inconsistent annual bill months returns empty`() {
        val txs = listOf(
            tx(1000.0, 2023, 3, 10),
            tx(1050.0, 2024, 7, 12),
            tx(1100.0, 2025, 11, 8)
        )
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `empty transactions returns empty`() {
        val results = ExpenseSeasonalityDetector.detect(catId, catName, emptyList())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `annual bill minimum 2 years passes`() {
        val txs = listOf(
            tx(1000.0, 2024, 6, 15),
            tx(1050.0, 2025, 6, 10)
        )
        val results = ExpenseSeasonalityDetector.detect(catId, catName, txs)
        val bill = results.find { it.patternType == PatternType.ANNUAL_BILL }
        assertTrue("Expected ANNUAL_BILL pattern", bill != null)
    }

    @Test
    fun `both spike and bill can coexist for separate categories`() {
        val spikeCatId = "cat_gifts"
        val billCatId = "cat_insurance"

        val spikeTxs = mutableListOf<TransactionEntity>()
        for (y in 2024..2025) {
            for (m in 1..11) { spikeTxs.add(tx(100.0, y, m, 15).copy(categoryId = spikeCatId)) }
            spikeTxs.add(tx(500.0, y, 12, 20).copy(categoryId = spikeCatId))
        }

        val billTxs = listOf(
            tx(1200.0, 2023, 4, 10).copy(categoryId = billCatId),
            tx(1250.0, 2024, 4, 12).copy(categoryId = billCatId),
            tx(1300.0, 2025, 4, 8).copy(categoryId = billCatId)
        )

        val spikeResults = ExpenseSeasonalityDetector.detect(spikeCatId, "Gifts", spikeTxs)
        val billResults = ExpenseSeasonalityDetector.detect(billCatId, "Insurance", billTxs)

        assertTrue("Expected SEASONAL_SPIKE for gifts", spikeResults.any { it.patternType == PatternType.SEASONAL_SPIKE })
        assertTrue("Expected ANNUAL_BILL for insurance", billResults.any { it.patternType == PatternType.ANNUAL_BILL })
    }
}
