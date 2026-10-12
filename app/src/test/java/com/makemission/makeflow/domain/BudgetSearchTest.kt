package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.BudgetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetSearchTest {
    private val budgets = listOf(
        BudgetEntity("food", "food", "Groceries", name = "Market", amount = 800.0, period = "Monthly"),
        BudgetEntity("travel", "transport", "Transport", amount = 200.0, period = "Weekly"),
        BudgetEntity("rent", "housing", "Housing", amount = 1000.0, period = "Monthly")
    )

    @Test fun blankQueryKeepsEveryBudgetInOriginalOrder() {
        assertEquals(budgets, BudgetSearch.filter(budgets, " \t "))
    }

    @Test fun matchesCustomNameCategoryAndPeriodWithAllTerms() {
        assertEquals(listOf(budgets[0]), BudgetSearch.filter(budgets, " market   FOOD "))
        assertEquals(listOf(budgets[1]), BudgetSearch.filter(budgets, "weekly transport"))
        assertEquals(emptyList<BudgetEntity>(), BudgetSearch.filter(budgets, "market weekly"))
        assertEquals(listOf(budgets[2]), BudgetSearch.filter(budgets, "housing"))
    }
}
