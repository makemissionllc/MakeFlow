package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.SavingsGoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalFeasibilityCalculatorTest {

    private val stableSurpluses = listOf(
        MonthlySurplus(3000.0, 1500.0),
        MonthlySurplus(3000.0, 1500.0),
        MonthlySurplus(3000.0, 1500.0)
    )

    private val categorySpending = listOf(
        CategorySpendSummary("Food & Drinks", 500.0),
        CategorySpendSummary("Entertainment", 200.0),
        CategorySpendSummary("Shopping", 300.0)
    )

    private val now = 1_000_000L

    @Test
    fun `feasible goal with stable surplus`() {
        val goal = SavingsGoalEntity(
            name = "New Laptop",
            targetAmount = 6000.0,
            targetDate = now + 4L * 30 * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 0.0,
            monthlySurpluses = stableSurpluses,
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.FEASIBLE, result.status)
        assertEquals(1500.0, result.safeFloor, 0.001)
        assertEquals(1500.0, result.requiredMonthlySave!!, 0.001)
        assertEquals(4, result.monthsRemaining)
        assertTrue(result.confidence > 0f)
    }

    @Test
    fun `infeasible goal produces correct revised timeline`() {
        val goal = SavingsGoalEntity(
            name = "Dream Car",
            targetAmount = 30000.0,
            targetDate = now + 6L * 30 * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 0.0,
            monthlySurpluses = stableSurpluses,
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.NOT_FEASIBLE, result.status)
        assertEquals(5000.0, result.requiredMonthlySave!!, 0.001)
        assertEquals(6, result.monthsRemaining)
        assertEquals(20, result.revisedTimelineMonths)
    }

    @Test
    fun `trade-off suggestion computes sensible percentage`() {
        val goal = SavingsGoalEntity(
            name = "Vacation",
            targetAmount = 20000.0,
            targetDate = now + 12L * 30 * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 0.0,
            monthlySurpluses = stableSurpluses,
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.NOT_FEASIBLE, result.status)
        assertNotNull(result.tradeOffSuggestion)
        val suggestion = result.tradeOffSuggestion!!
        assertTrue(suggestion.suggestions.isNotEmpty())
        assertTrue(suggestion.suggestions.first().reductionPercent > 0f)
        assertTrue(suggestion.suggestions.first().reductionPercent <= 0.5f)
    }

    @Test
    fun `trade-off caps at reasonable reduction ceiling`() {
        val tinySurplus = listOf(
            MonthlySurplus(1000.0, 900.0),
            MonthlySurplus(1000.0, 900.0),
            MonthlySurplus(1000.0, 900.0)
        )
        val goal = SavingsGoalEntity(
            name = "Mansion",
            targetAmount = 500000.0,
            targetDate = now + 12L * 30 * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 0.0,
            monthlySurpluses = tinySurplus,
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.NOT_FEASIBLE, result.status)
        assertNull(result.tradeOffSuggestion)
    }

    @Test
    fun `no target date just projects timeline`() {
        val goal = SavingsGoalEntity(
            name = "Rainy Day Fund",
            targetAmount = 9000.0,
            targetDate = null,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 0.0,
            monthlySurpluses = stableSurpluses,
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.PROJECTION_ONLY, result.status)
        assertEquals(6, result.revisedTimelineMonths)
        assertNull(result.monthsRemaining)
        assertNull(result.requiredMonthlySave)
    }

    @Test
    fun `insufficient transaction history returns insufficient data`() {
        val goal = SavingsGoalEntity(
            name = "Test",
            targetAmount = 1000.0,
            targetDate = now + 30L * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 0.0,
            monthlySurpluses = listOf(MonthlySurplus(1000.0, 500.0)),
            categorySpending = emptyList(),
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.INSUFFICIENT_DATA, result.status)
    }

    @Test
    fun `goal already achieved returns feasible`() {
        val goal = SavingsGoalEntity(
            name = "Done Goal",
            targetAmount = 5000.0,
            targetDate = now + 30L * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 5000.0,
            monthlySurpluses = stableSurpluses,
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.FEASIBLE, result.status)
    }

    @Test
    fun `past deadline returns not feasible`() {
        val goal = SavingsGoalEntity(
            name = "Late Goal",
            targetAmount = 6000.0,
            targetDate = now - 30L * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 1000.0,
            monthlySurpluses = stableSurpluses,
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.NOT_FEASIBLE, result.status)
        assertEquals(0, result.monthsRemaining)
    }

    @Test
    fun `negative surplus returns insufficient data`() {
        val goal = SavingsGoalEntity(
            name = "Test",
            targetAmount = 1000.0,
            targetDate = now + 30L * 24 * 60 * 60 * 1000,
            createdDate = now
        )
        val result = GoalFeasibilityCalculator.assess(
            goal = goal,
            currentProgress = 0.0,
            monthlySurpluses = listOf(
                MonthlySurplus(1000.0, 500.0),
                MonthlySurplus(500.0, 600.0),
                MonthlySurplus(1000.0, 400.0)
            ),
            categorySpending = categorySpending,
            nowMillis = now
        )
        assertEquals(FeasibilityStatus.INSUFFICIENT_DATA, result.status)
    }
}
