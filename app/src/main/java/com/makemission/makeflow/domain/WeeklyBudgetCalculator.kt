package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity

data class WeeklyBudgetStatus(
    val hasBudget: Boolean,
    val limit: Double,
    val spent: Double
)

object WeeklyBudgetCalculator {
    fun calculate(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        focusedAccountId: String?,
        weekStartMillis: Long,
        weekEndMillis: Long
    ): WeeklyBudgetStatus {
        val weeklyBudgets = budgets.filter { it.period.equals("Weekly", ignoreCase = true) }
        if (weeklyBudgets.isEmpty()) return WeeklyBudgetStatus(false, 0.0, 0.0)

        val budgetCategoryIds = weeklyBudgets.mapTo(mutableSetOf()) { it.categoryId }
        val parentByChild = categories
            .filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }
        val spent = transactions.asSequence()
            .filter { transaction ->
                transaction.type == "EXPENSE" &&
                    transaction.timestamp >= weekStartMillis &&
                    transaction.timestamp < weekEndMillis &&
                    (focusedAccountId == null || transaction.accountId == focusedAccountId) &&
                    transaction.categoryId != null &&
                    (parentByChild[transaction.categoryId] ?: transaction.categoryId) in budgetCategoryIds
            }
            .sumOf { it.amount }

        return WeeklyBudgetStatus(
            hasBudget = true,
            limit = weeklyBudgets.sumOf { it.amount },
            spent = spent
        )
    }
}
