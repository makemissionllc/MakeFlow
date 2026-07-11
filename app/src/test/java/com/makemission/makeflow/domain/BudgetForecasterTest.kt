package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.BudgetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetForecasterTest {

    private val budget = BudgetEntity(
        id = "b1",
        categoryId = "cat1",
        categoryName = "Groceries",
        amount = 1000.0,
        spentAmount = 0.0,
        period = "Monthly"
    )

    @Test
    fun `first day of month returns ON_TRACK`() {
        val start = 1704067200000L // Jan 1 2024 00:00 UTC
        val end = 1706745600000L   // Feb 1 2024 00:00 UTC
        val now = start            // first moment of the month
        val result = BudgetForecaster.forecast(budget.copy(spentAmount = 200.0), start, end, now)
        assertEquals(BudgetForecastStatus.ON_TRACK, result.status)
        assertEquals(0f, result.percentElapsed, 0.001f)
    }

    @Test
    fun `zero elapsed returns ON_TRACK`() {
        val start = 1704067200000L
        val end = 1706745600000L
        val now = start
        val result = BudgetForecaster.forecast(budget.copy(spentAmount = 500.0), start, end, now)
        assertEquals(BudgetForecastStatus.ON_TRACK, result.status)
        assertEquals(0f, result.percentElapsed, 0.001f)
    }

    @Test
    fun `overspent returns OVERSPENT`() {
        val start = 1704067200000L
        val end = 1706745600000L
        val now = start + 15 * 24 * 60 * 60 * 1000L // mid-month
        val result = BudgetForecaster.forecast(budget.copy(spentAmount = 1200.0), start, end, now)
        assertEquals(BudgetForecastStatus.OVERSPENT, result.status)
        assertEquals(1200.0 - 1000.0, result.projectedOverspend!!, 0.001)
    }

    @Test
    fun `zero budget amount with no spend returns ON_TRACK`() {
        val start = 1704067200000L
        val end = 1706745600000L
        val now = start + 10 * 24 * 60 * 60 * 1000L
        val result = BudgetForecaster.forecast(budget.copy(amount = 0.0, spentAmount = 0.0), start, end, now)
        assertEquals(BudgetForecastStatus.ON_TRACK, result.status)
        assertEquals(0f, result.percentSpent, 0.001f)
        assertNull(result.projectedOverspend)
    }

    @Test
    fun `zero budget amount with spend returns OVERSPENT`() {
        val start = 1704067200000L
        val end = 1706745600000L
        val now = start + 10 * 24 * 60 * 60 * 1000L
        val result = BudgetForecaster.forecast(budget.copy(amount = 0.0, spentAmount = 50.0), start, end, now)
        assertEquals(BudgetForecastStatus.OVERSPENT, result.status)
        assertEquals(Float.MAX_VALUE, result.percentSpent, 0.001f)
    }

    @Test
    fun `off track when spent exceeds elapsed times threshold`() {
        val start = 1704067200000L
        val end = 1706745600000L
        val now = start + 15 * 24 * 60 * 60 * 1000L // ~50% elapsed
        // spent = 60% of budget, threshold is 1.2x elapsed = 1.2 * 0.5 = 0.6, so 0.6 > 0.6 not >, need just above
        val spent = 0.61 * 1000.0
        val result = BudgetForecaster.forecast(budget.copy(spentAmount = spent), start, end, now)
        assertEquals(BudgetForecastStatus.OFF_TRACK, result.status)
        assertNotNull(result.projectedOverspend)
    }

    @Test
    fun `on track when spent is within threshold`() {
        val start = 1704067200000L
        val end = 1706745600000L
        val now = start + 15 * 24 * 60 * 60 * 1000L // ~50% elapsed
        val result = BudgetForecaster.forecast(budget.copy(spentAmount = 400.0), start, end, now)
        assertEquals(BudgetForecastStatus.ON_TRACK, result.status)
        assertNull(result.projectedOverspend)
    }

    @Test
    fun `remainingDays computed correctly`() {
        val start = 1704067200000L
        val end = 1706745600000L // 31 days later
        val now = start + 10 * 24 * 60 * 60 * 1000L
        val result = BudgetForecaster.forecast(budget.copy(spentAmount = 300.0), start, end, now)
        assertEquals(20, result.remainingDays)
    }
}
