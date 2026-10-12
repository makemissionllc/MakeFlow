package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.BudgetEntity
import java.util.Locale

object BudgetSearch {
    fun filter(budgets: List<BudgetEntity>, query: String): List<BudgetEntity> {
        val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotEmpty)
        if (terms.isEmpty()) return budgets
        return budgets.filter { budget ->
            val text = "${budget.displayName} ${budget.categoryName} ${budget.period}".lowercase(Locale.ROOT)
            terms.all { it in text }
        }
    }
}
