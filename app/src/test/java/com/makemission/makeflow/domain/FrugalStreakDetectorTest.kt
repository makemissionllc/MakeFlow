package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId

class FrugalStreakDetectorTest {

    private val catId = "cat_food"
    private val catName = "Food"

    private fun utcMillis(year: Int, month: Int, day: Int): Long =
        YearMonth.of(year, month).atDay(day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

    private fun tx(amount: Double, year: Int, month: Int, day: Int): TransactionEntity =
        TransactionEntity(
            amount = amount,
            type = "EXPENSE",
            categoryId = catId,
            timestamp = utcMillis(year, month, day)
        )

    private val now = utcMillis(2026, 4, 15)

    // Trailing months: Aug 2025 through Jan 2026 (6 months)
    // Evaluated months: Feb 2026, Mar 2026
    private fun trailingHistory(amount: Double = 1000.0) = listOf(
        tx(amount, 2025, 8, 15),
        tx(amount, 2025, 9, 15),
        tx(amount, 2025, 10, 15),
        tx(amount, 2025, 11, 15),
        tx(amount, 2025, 12, 15),
        tx(amount, 2026, 1, 15)
    )

    private fun evaluatedSpend(mar: Double, feb: Double) = listOf(
        tx(mar, 2026, 3, 15),
        tx(feb, 2026, 2, 15)
    )

    @Test
    fun `both months under 70 percent triggers streak`() {
        // trailing avg = 1000, threshold = 700
        // March = 500 (< 700), February = 400 (< 700)
        val result = FrugalStreakDetector.detect(
            catId, catName, trailingHistory() + evaluatedSpend(500.0, 400.0), now
        )
        assertNotNull(result)
        assertEquals(catId, result!!.categoryId)
        assertEquals(catName, result.categoryName)
        assertEquals(2, result.streakLengthMonths)
        assertEquals(1100.0, result.totalSavedVsAverage, 0.01) // (1000*2) - (500+400)
        assertEquals(6600.0, result.projectedAnnualSavings, 0.01) // 1100/2*12
    }

    @Test
    fun `one qualifying month one non qualifying returns null`() {
        // March = 500 (< 700), February = 800 (>= 700)
        val result = FrugalStreakDetector.detect(
            catId, catName, trailingHistory() + evaluatedSpend(500.0, 800.0), now
        )
        assertNull(result)
    }

    @Test
    fun `neither month qualifies returns null`() {
        // Both >= 700
        val result = FrugalStreakDetector.detect(
            catId, catName, trailingHistory() + evaluatedSpend(800.0, 900.0), now
        )
        assertNull(result)
    }

    @Test
    fun `insufficient history returns null`() {
        // Only 2 months with spend total (1 trailing + 1 evaluated)
        val sparse = listOf(
            tx(1000.0, 2026, 1, 15),   // 1 trailing month
            tx(400.0, 2026, 3, 15)      // 1 evaluated month
        )
        val result = FrugalStreakDetector.detect(
            catId, catName, sparse, now
        )
        assertNull(result)
    }

    @Test
    fun `zero trailing average returns null`() {
        // All trailing months have 0 spend
        val zeroTrail = listOf(
            tx(0.0, 2025, 8, 15), tx(0.0, 2025, 9, 15), tx(0.0, 2025, 10, 15),
            tx(0.0, 2025, 11, 15), tx(0.0, 2025, 12, 15), tx(0.0, 2026, 1, 15)
        )
        val result = FrugalStreakDetector.detect(
            catId, catName, zeroTrail + evaluatedSpend(400.0, 300.0), now
        )
        assertNull(result)
    }

    @Test
    fun `exact threshold does not trigger`() {
        // threshold = 700, both exactly 700 -> not < 700
        val result = FrugalStreakDetector.detect(
            catId, catName, trailingHistory() + evaluatedSpend(700.0, 700.0), now
        )
        assertNull(result)
    }

    @Test
    fun `saved amount is zero when spending matches threshold exactly`() {
        // both = 699 (< 700), just under
        // totalSaved = 2000 - (699+699) = 602
        val result = FrugalStreakDetector.detect(
            catId, catName, trailingHistory() + evaluatedSpend(699.0, 699.0), now
        )
        assertNotNull(result)
        assertEquals(602.0, result!!.totalSavedVsAverage, 0.01)
    }

    @Test
    fun `no trailing months with history returns null`() {
        // 0 trailing months with spend > 0
        val onlyEvaluated = evaluatedSpend(400.0, 300.0)
        val result = FrugalStreakDetector.detect(
            catId, catName, onlyEvaluated, now
        )
        assertNull(result)
    }

    @Test
    fun `empty transactions returns null`() {
        val result = FrugalStreakDetector.detect(
            catId, catName, emptyList(), now
        )
        assertNull(result)
    }
}
