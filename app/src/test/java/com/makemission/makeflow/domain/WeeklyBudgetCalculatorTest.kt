package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyBudgetCalculatorTest {
    @Test
    fun `counts this weeks expenses under the matching parent budget and focused account`() {
        val budget = BudgetEntity(
            id = "weekly-food",
            categoryId = "food",
            categoryName = "Food",
            amount = 200.0,
            period = "Weekly"
        )
        val transactions = listOf(
            TransactionEntity(amount = 12.5, type = "EXPENSE", categoryId = "groceries", accountId = "wallet", timestamp = 100),
            TransactionEntity(amount = 4.0, type = "EXPENSE", categoryId = "food", accountId = "wallet", timestamp = 199),
            TransactionEntity(amount = 80.0, type = "EXPENSE", categoryId = "food", accountId = "savings", timestamp = 150),
            TransactionEntity(amount = 100.0, type = "INCOME", categoryId = "food", accountId = "wallet", timestamp = 150),
            TransactionEntity(amount = 50.0, type = "EXPENSE", categoryId = "food", accountId = "wallet", timestamp = 200)
        )

        val result = WeeklyBudgetCalculator.calculate(
            budgets = listOf(budget),
            transactions = transactions,
            categories = listOf(CategoryEntity(id = "groceries", name = "Groceries", type = "EXPENSE", parentCategoryId = "food")),
            focusedAccountId = "wallet",
            weekStartMillis = 100,
            weekEndMillis = 200
        )

        assertTrue(result.hasBudget)
        assertEquals(200.0, result.limit, 0.0)
        assertEquals(16.5, result.spent, 0.0)
    }

    @Test
    fun `returns no weekly status when only nonweekly budgets exist`() {
        val result = WeeklyBudgetCalculator.calculate(
            budgets = listOf(
                BudgetEntity(
                    id = "monthly-food",
                    categoryId = "food",
                    categoryName = "Food",
                    amount = 500.0,
                    period = "Monthly"
                )
            ),
            transactions = emptyList(),
            categories = emptyList(),
            focusedAccountId = null,
            weekStartMillis = 0,
            weekEndMillis = 1000
        )

        assertFalse(result.hasBudget)
        assertEquals(0.0, result.limit, 0.0)
        assertEquals(0.0, result.spent, 0.0)
    }
}
