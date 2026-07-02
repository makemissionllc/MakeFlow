package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.dao.MonthlyCategorySpend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetSuggestionEngineTest {

    @Test
    fun `no budget set returns NO_BUDGET_SET suggestion`() {
        val monthly = listOf(
            MonthlyCategorySpend("2026-01", 500.0),
            MonthlyCategorySpend("2026-02", 600.0),
            MonthlyCategorySpend("2026-03", 550.0)
        )
        val result = BudgetSuggestionEngine.suggest(
            categoryId = "cat1",
            categoryName = "Groceries",
            monthlySpend = monthly,
            currentBudget = null
        )
        assertNotNull(result)
        assertEquals(SuggestionReason.NO_BUDGET_SET, result!!.reason)
        assertEquals("cat1", result.categoryId)
        assertEquals("Groceries", result.categoryName)
        // avg = (500+600+550)/3 = 550, buffer 1.10 = 605
        assertEquals(605.0, result.suggestedAmount, 0.001)
        assertNull(result.currentBudgetAmount)
    }

    @Test
    fun `consistently over budget returns CONSISTENTLY_OVER suggestion`() {
        val monthly = listOf(
            MonthlyCategorySpend("2026-01", 500.0),
            MonthlyCategorySpend("2026-02", 600.0),
            MonthlyCategorySpend("2026-03", 550.0)
        )
        val result = BudgetSuggestionEngine.suggest(
            categoryId = "cat1",
            categoryName = "Groceries",
            monthlySpend = monthly,
            currentBudget = 400.0
        )
        assertNotNull(result)
        assertEquals(SuggestionReason.CONSISTENTLY_OVER, result!!.reason)
        assertEquals(605.0, result.suggestedAmount, 0.001)
        assertEquals(400.0, result.currentBudgetAmount!!, 0.001)
    }

    @Test
    fun `consistently under budget returns CONSISTENTLY_UNDER suggestion`() {
        val monthly = listOf(
            MonthlyCategorySpend("2026-01", 200.0),
            MonthlyCategorySpend("2026-02", 210.0),
            MonthlyCategorySpend("2026-03", 190.0)
        )
        val result = BudgetSuggestionEngine.suggest(
            categoryId = "cat1",
            categoryName = "Groceries",
            monthlySpend = monthly,
            currentBudget = 500.0
        )
        assertNotNull(result)
        assertEquals(SuggestionReason.CONSISTENTLY_UNDER, result!!.reason)
        // avg = 200, buffer 1.10 = 220
        assertEquals(220.0, result.suggestedAmount, 0.001)
    }

    @Test
    fun `budget on track returns null`() {
        val monthly = listOf(
            MonthlyCategorySpend("2026-01", 450.0),
            MonthlyCategorySpend("2026-02", 460.0),
            MonthlyCategorySpend("2026-03", 440.0)
        )
        val result = BudgetSuggestionEngine.suggest(
            categoryId = "cat1",
            categoryName = "Groceries",
            monthlySpend = monthly,
            currentBudget = 500.0
        )
        // 440/500 = 0.88, which is >= 0.70, so not CONSISTENTLY_UNDER
        // 460 < 500, 440 < 500, so not CONSISTENTLY_OVER
        assertNull(result)
    }

    @Test
    fun `fewer than 2 months returns null`() {
        val monthly = listOf(MonthlyCategorySpend("2026-01", 500.0))
        val result = BudgetSuggestionEngine.suggest(
            categoryId = "cat1",
            categoryName = "Groceries",
            monthlySpend = monthly,
            currentBudget = null
        )
        assertNull(result)
    }

    @Test
    fun `zero average spend returns null`() {
        val monthly = listOf(
            MonthlyCategorySpend("2026-01", 0.0),
            MonthlyCategorySpend("2026-02", 0.0)
        )
        val result = BudgetSuggestionEngine.suggest(
            categoryId = "cat1",
            categoryName = "Groceries",
            monthlySpend = monthly,
            currentBudget = null
        )
        assertNull(result)
    }

    @Test
    fun `fillMissingMonths fills gaps with zero`() {
        val raw = listOf(MonthlyCategorySpend("2026-01", 500.0), MonthlyCategorySpend("2026-03", 600.0))
        val filled = BudgetSuggestionEngine.fillMissingMonths(raw, "2026-01", "2026-03")
        assertEquals(3, filled.size)
        assertEquals(500.0, filled[0].totalSpent, 0.001)
        assertEquals(0.0, filled[1].totalSpent, 0.001)
        assertEquals(600.0, filled[2].totalSpent, 0.001)
    }
}
