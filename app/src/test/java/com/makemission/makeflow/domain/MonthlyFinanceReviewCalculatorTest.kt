package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId

class MonthlyFinanceReviewCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val month = YearMonth.of(2026, 5)
    private val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    private val accounts = listOf(
        AccountEntity(id = "kes", name = "KES wallet", balance = 0.0, initialBalance = 0.0, currencyCode = "KES"),
        AccountEntity(id = "usd", name = "USD account", balance = 0.0, initialBalance = 0.0, currencyCode = "USD"),
        AccountEntity(id = "eur", name = "EUR account", balance = 0.0, initialBalance = 0.0, currencyCode = "EUR")
    )
    private val categories = listOf(
        CategoryEntity(id = "food", name = "Food", type = "EXPENSE"),
        CategoryEntity(id = "lunch", name = "Lunch", type = "EXPENSE", parentCategoryId = "food")
    )

    @Test
    fun convertsMixedCurrencyAndSummarizesCurrentAndPriorMonth() {
        val transactions = listOf(
            tx(1000.0, "INCOME", "kes", monthStart),
            tx(10.0, "EXPENSE", "usd", monthStart + 1_000, "lunch", "Groceries"),
            tx(5.0, "EXPENSE", "kes", monthStart + 2_000, "food"),
            tx(20.0, "EXPENSE", "usd", monthStart - 1, "food")
        )
        val result = MonthlyFinanceReviewCalculator.calculate(
            transactions, accounts, categories, month, "KES",
            listOf(CurrencyRate("USD", "KES", 130.0)), zone
        )

        assertEquals(1000.0, result.income, 0.001)
        assertEquals(1305.0, result.expenses, 0.001)
        assertEquals(-305.0, result.net, 0.001)
        assertEquals(2600.0, result.previousExpenses, 0.001)
        assertEquals(3, result.transactionCount)
        assertEquals("Food", result.topCategoryName)
        assertEquals(100.0, result.topCategorySharePercent, 0.001)
        assertEquals("Groceries", result.largestExpenseDescription)
        assertEquals(-49.807692, result.expenseChangePercent!!, 0.001)
    }

    @Test
    fun missingRatesAreExcludedAndCountedInsteadOfAddedAsRawAmounts() {
        val transactions = listOf(
            tx(100.0, "EXPENSE", "eur", monthStart),
            tx(50.0, "EXPENSE", "kes", monthStart + 1_000)
        )
        val result = MonthlyFinanceReviewCalculator.calculate(
            transactions, accounts, categories, month, "KES", emptyList(), zone
        )

        assertEquals(50.0, result.expenses, 0.001)
        assertEquals(2, result.transactionCount)
        assertEquals(1, result.unconvertedTransactionCount)
        assertNull(result.expenseChangePercent)
    }

    private fun tx(
        amount: Double,
        type: String,
        accountId: String,
        timestamp: Long,
        categoryId: String? = null,
        note: String = ""
    ) = TransactionEntity(
        amount = amount,
        type = type,
        accountId = accountId,
        timestamp = timestamp,
        categoryId = categoryId,
        note = note
    )
}
