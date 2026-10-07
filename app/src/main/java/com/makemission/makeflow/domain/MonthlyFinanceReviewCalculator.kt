package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.time.YearMonth
import java.time.ZoneId

data class MonthlyFinanceReview(
    val month: YearMonth,
    val income: Double,
    val expenses: Double,
    val net: Double,
    val previousIncome: Double,
    val previousExpenses: Double,
    val previousNet: Double,
    val savingsRatePercent: Double?,
    val expenseChangePercent: Double?,
    val transactionCount: Int,
    val unconvertedTransactionCount: Int,
    val topCategoryName: String?,
    val topCategoryAmount: Double,
    val topCategorySharePercent: Double,
    val largestExpenseDescription: String?,
    val largestExpenseAmount: Double,
    val rateDate: String?
)

object MonthlyFinanceReviewCalculator {
    fun calculate(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        month: YearMonth,
        defaultCurrency: String,
        rates: List<CurrencyRate>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MonthlyFinanceReview {
        val current = monthRange(month, zoneId)
        val previous = monthRange(month.minusMonths(1), zoneId)
        val accountCurrencies = accounts.associate { it.id to (it.currencyCode ?: defaultCurrency) }
        val parentCategoryById = categories.associate { category ->
            category.id to (category.parentCategoryId ?: category.id)
        }
        val categoryNames = categories
            .filter { it.parentCategoryId == null }
            .associate { it.id to it.name }

        fun convertMonth(range: LongRange): ConvertedMonth {
            val periodTransactions = transactions.filter {
                it.timestamp in range && (it.type == "INCOME" || it.type == "EXPENSE")
            }
            var missing = 0
            val converted = periodTransactions.mapNotNull { transaction ->
                val source = accountCurrencies[transaction.accountId] ?: defaultCurrency
                val amount = CurrencyConverter.convert(transaction.amount, source, defaultCurrency, rates)
                if (amount == null) {
                    missing++
                    null
                } else {
                    ConvertedTransaction(transaction, amount)
                }
            }
            return ConvertedMonth(converted, periodTransactions.size, missing)
        }

        val currentMonth = convertMonth(current)
        val previousMonth = convertMonth(previous)
        val income = currentMonth.transactions.filter { it.transaction.type == "INCOME" }.sumOf { it.amount }
        val expenses = currentMonth.transactions.filter { it.transaction.type == "EXPENSE" }.sumOf { it.amount }
        val previousIncome = previousMonth.transactions.filter { it.transaction.type == "INCOME" }.sumOf { it.amount }
        val previousExpenses = previousMonth.transactions.filter { it.transaction.type == "EXPENSE" }.sumOf { it.amount }

        val categoryTotals = currentMonth.transactions
            .filter { it.transaction.type == "EXPENSE" }
            .groupBy { parentCategoryById[it.transaction.categoryId] ?: "" }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
        val topCategory = categoryTotals.maxByOrNull { it.value }
        val largestExpense = currentMonth.transactions
            .filter { it.transaction.type == "EXPENSE" }
            .maxByOrNull { it.amount }
        val date = rates
            .filter { it.baseCurrencyCode == defaultCurrency }
            .mapNotNull { it.rateDate }
            .maxOrNull()

        return MonthlyFinanceReview(
            month = month,
            income = income,
            expenses = expenses,
            net = income - expenses,
            previousIncome = previousIncome,
            previousExpenses = previousExpenses,
            previousNet = previousIncome - previousExpenses,
            savingsRatePercent = if (income > 0.0) (income - expenses) / income * 100.0 else null,
            expenseChangePercent = if (previousExpenses > 0.0) {
                (expenses - previousExpenses) / previousExpenses * 100.0
            } else null,
            transactionCount = currentMonth.transactionCount,
            unconvertedTransactionCount = currentMonth.unconvertedCount,
            topCategoryName = topCategory?.key?.let { categoryNames[it] ?: "Uncategorized" },
            topCategoryAmount = topCategory?.value ?: 0.0,
            topCategorySharePercent = if (expenses > 0.0 && topCategory != null) topCategory.value / expenses * 100.0 else 0.0,
            largestExpenseDescription = largestExpense?.transaction?.note
                ?.takeIf { it.isNotBlank() } ?: largestExpense?.let { "Unlabeled expense" },
            largestExpenseAmount = largestExpense?.amount ?: 0.0,
            rateDate = date
        )
    }

    private fun monthRange(month: YearMonth, zoneId: ZoneId): LongRange {
        val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        return start..end
    }

    private data class ConvertedTransaction(
        val transaction: TransactionEntity,
        val amount: Double
    )

    private data class ConvertedMonth(
        val transactions: List<ConvertedTransaction>,
        val transactionCount: Int,
        val unconvertedCount: Int
    )
}
