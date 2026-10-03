package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class SpendingVelocityTrackerTest {

    private val catId = "cat_food"
    private val catName = "Food"

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        YearMonth.of(year, month).atDay(day)
            .atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

    private fun tx(
        amount: Double,
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        id: String = "tx_${year}_${month}_${day}_$hour"
    ) = TransactionEntity(
        id = id,
        amount = amount,
        type = "EXPENSE",
        categoryId = catId,
        timestamp = utcMillis(year, month, day, hour)
    )

    private val now = utcMillis(2026, 4, 10) // April 10, 2026, daysElapsed = 10

    // Build 3 trailing months of history at 1000 each
    private fun trailingHistory(
        janAmount: Double = 1000.0,
        febAmount: Double = 1000.0,
        marAmount: Double = 1000.0
    ) = listOf(
        tx(janAmount, 2026, 1, 31),
        tx(febAmount, 2026, 2, 28),
        tx(marAmount, 2026, 3, 31)
    )

    @Test
    fun `steady pace produces no alert`() {
        val daily = 33.34
        val currentMonth = (1..10).map { day -> tx(daily, 2026, 4, day) }
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory() + currentMonth, now
        )
        assertNull(result)
    }

    @Test
    fun `burst over exactly 3 days produces ELEVATED`() {
        val currentMonth = listOf(
            tx(28.57, 2026, 4, 1), tx(28.57, 2026, 4, 2), tx(28.57, 2026, 4, 3),
            tx(28.57, 2026, 4, 4), tx(28.57, 2026, 4, 5), tx(28.57, 2026, 4, 6),
            tx(28.58, 2026, 4, 7),
            tx(200.0, 2026, 4, 8),
            tx(150.0, 2026, 4, 9),
            tx(150.0, 2026, 4, 10)
        )
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory() + currentMonth, now
        )
        assertNotNull(result)
        assertTrue(result!!.severity == VelocitySeverity.CRITICAL || result.severity == VelocitySeverity.ELEVATED || result.severity == VelocitySeverity.HIGH)
        assertEquals(catId, result.categoryId)
        assertEquals(catName, result.categoryName)
    }

    @Test
    fun `single one-day spike does not trigger`() {
        val currentMonth = (1..9).flatMap { day ->
            listOf(tx(30.0, 2026, 4, day, hour = 10), tx(0.0, 2026, 4, day, hour = 14))
        } + tx(500.0, 2026, 4, 10)
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory() + currentMonth, now
        )
        assertNull(result)
    }

    @Test
    fun `zero history produces no alert`() {
        val result = SpendingVelocityTracker.compute(
            catId, catName, emptyList(), now
        )
        assertNull(result)
    }

    @Test
    fun `category with zero expected spend handled gracefully`() {
        val currentMonth = (1..10).map { day -> tx(50.0, 2026, 4, day) }
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory(0.0, 0.0, 0.0) + currentMonth, now
        )
        assertNull(result)
    }

    @Test
    fun `less than 3 months history returns null`() {
        val history = listOf(tx(1000.0, 2026, 3, 31))
        val currentMonth = (1..10).map { day -> tx(50.0, 2026, 4, day) }
        val result = SpendingVelocityTracker.compute(
            catId, catName, history + currentMonth, now
        )
        assertNull(result)
    }

    @Test
    fun `high severity spending returns HIGH`() {
        // Days 1-6 at 30/day, Days 7-10 at 100/day
        // Day 8 cumulative = 380, expected 266.67, pct=42.5% >=30% ✓
        // Day 9 cumulative = 480, expected 300, pct=60% >=30% ✓
        // Day 10 cumulative = 580, expected 333.33, pct=74% >=30% ✓
        // 3 consecutive -> passes gate
        // final percentAhead = 74% >= 50% -> HIGH
        val currentMonth = (1..6).map { day -> tx(30.0, 2026, 4, day) } + listOf(
            tx(100.0, 2026, 4, 7),
            tx(100.0, 2026, 4, 8),
            tx(100.0, 2026, 4, 9),
            tx(100.0, 2026, 4, 10)
        )
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory() + currentMonth, now
        )
        assertNotNull(result)
        assertTrue(result!!.severity == VelocitySeverity.HIGH)
    }

    @Test
    fun `percentAhead correctly computed`() {
        val currentMonth = (1..6).map { day -> tx(30.0, 2026, 4, day) } + listOf(
            tx(100.0, 2026, 4, 7),
            tx(100.0, 2026, 4, 8),
            tx(100.0, 2026, 4, 9),
            tx(100.0, 2026, 4, 10)
        )
        // trailing avg = 1000
        // expected = 1000 * 10/30 = 333.33
        // actual = 6*30 + 4*100 = 580
        // percentAhead = (580 - 333.33) / 333.33 = 0.74
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory() + currentMonth, now
        )
        assertNotNull(result)
        val expectedPct = (580.0 - 1000.0 * 10.0 / 30.0) / (1000.0 * 10.0 / 30.0)
        assertEquals(expectedPct, result!!.percentAhead, 0.01)
    }

    @Test
    fun `projected month end amount is correct`() {
        val currentMonth = (1..6).map { day -> tx(30.0, 2026, 4, day) } + listOf(
            tx(100.0, 2026, 4, 7),
            tx(100.0, 2026, 4, 8),
            tx(100.0, 2026, 4, 9),
            tx(100.0, 2026, 4, 10)
        )
        // actualCumulative = 580, daysElapsed = 10, daysInMonth = 30
        // projected = 580 / (10/30) = 1740
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory() + currentMonth, now
        )
        assertNotNull(result)
        assertEquals(1740.0, result!!.projectedMonthEndAmount, 0.01)
    }

    @Test
    fun `first few days of month returns null`() {
        val earlyNow = utcMillis(2026, 4, 2)
        val currentMonth = (1..2).map { day -> tx(100.0, 2026, 4, day) }
        val result = SpendingVelocityTracker.compute(
            catId, catName, trailingHistory() + currentMonth, earlyNow
        )
        assertNull(result)
    }
}
