package com.makemission.makeflow.ui.viewmodel

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfExportDataBuilderTest {

    private val accountA = AccountEntity(id = "acc1", name = "Main Checking", balance = 5000.0, initialBalance = 5000.0, currencyCode = "KES")
    private val accountB = AccountEntity(id = "acc2", name = "Savings USD", balance = 1000.0, initialBalance = 1000.0, currencyCode = "USD")
    private val accountC = AccountEntity(id = "acc3", name = "Euro Wallet", balance = 500.0, initialBalance = 500.0, currencyCode = "EUR")

    private val rateUSD = ExchangeRateEntity(currencyCode = "USD", rateToDisplayCurrency = 150.0, baseCurrencyCode = "KES")
    private val rateEUR = ExchangeRateEntity(currencyCode = "EUR", rateToDisplayCurrency = 160.0, baseCurrencyCode = "KES")

    private val accountMap = listOf(accountA, accountB, accountC).associateBy { it.id }
    private val exchangeRates = listOf(rateUSD, rateEUR).associateBy { it.currencyCode }

    private fun dateFmt(ts: Long): String = "2025-07-01 10:00"

    @Test
    fun `empty transactions produces empty rows and zero summary`() {
        val result = PdfExportDataBuilder.build(
            transactions = emptyList(),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        assertTrue(result.rows.isEmpty())
        assertEquals(0.0, result.summary.totalIncome, 0.001)
        assertEquals(0.0, result.summary.totalExpense, 0.001)
        assertEquals(0.0, result.summary.net, 0.001)
    }

    @Test
    fun `income transaction in same currency is included in totalIncome`() {
        val tx = TransactionEntity(
            id = "tx1", amount = 500.0, type = "INCOME",
            categoryId = "salary", accountId = "acc1",
            note = "Monthly salary", timestamp = 1000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            categoryMap = mapOf("salary" to CategoryEntity(id = "salary", name = "Salary", type = "INCOME")),
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        assertEquals(1, result.rows.size)
        val row = result.rows[0]
        assertEquals("Salary", row.category)
        assertEquals("Main Checking", row.account)
        assertTrue(row.amount.contains("KES"))
        assertTrue(row.amount.filter(Char::isDigit).contains("500"))
        assertFalse(row.isTransfer)
        assertEquals(500.0, result.summary.totalIncome, 0.001)
        assertEquals(0.0, result.summary.totalExpense, 0.001)
    }

    @Test
    fun `expense transaction is included in totalExpense`() {
        val tx = TransactionEntity(
            id = "tx2", amount = 120.0, type = "EXPENSE",
            categoryId = "food", accountId = "acc1",
            note = "Lunch", timestamp = 2000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        assertEquals(0.0, result.summary.totalIncome, 0.001)
        assertEquals(120.0, result.summary.totalExpense, 0.001)
        assertEquals(-120.0, result.summary.net, 0.001)
    }

    @Test
    fun `transfer is excluded from income and expense totals`() {
        val tx = TransactionEntity(
            id = "tx3", amount = 5000.0, type = "TRANSFER",
            categoryId = null, accountId = "acc1",
            fromAccountId = "acc1", toAccountId = "acc2",
            note = "Move to savings", timestamp = 3000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        assertEquals(1, result.rows.size)
        val row = result.rows[0]
        assertTrue(row.isTransfer)
        assertEquals("Transfer", row.category)
        assertEquals("Move to savings", row.description)
        assertEquals("From Main Checking to Savings USD", row.transferDetail)
        assertEquals(0.0, result.summary.totalIncome, 0.001)
        assertEquals(0.0, result.summary.totalExpense, 0.001)
    }

    @Test
    fun `multi-currency amount is converted via exchange rate`() {
        val tx = TransactionEntity(
            id = "tx4", amount = 100.0, type = "INCOME",
            categoryId = "freelance", accountId = "acc2",
            note = "USD payment", timestamp = 4000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        val row = result.rows[0]
        // 100 USD * 150 = 15000 KES
        assertTrue(row.amount.filter(Char::isDigit).contains("15000"))
        assertEquals(15000.0, result.summary.totalIncome, 0.001)
    }

    @Test
    fun `EUR expense converted via exchange rate`() {
        val tx = TransactionEntity(
            id = "tx5", amount = 50.0, type = "EXPENSE",
            categoryId = "travel", accountId = "acc3",
            note = "Train ticket", timestamp = 5000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        val row = result.rows[0]
        // 50 EUR * 160 = 8000 KES
        assertTrue(row.amount.filter(Char::isDigit).contains("8000"))
        assertEquals(8000.0, result.summary.totalExpense, 0.001)
    }

    @Test
    fun `missing exchange rate falls back to raw amount`() {
        val accountGBP = AccountEntity(id = "acc4", name = "UK Account", balance = 200.0, initialBalance = 200.0, currencyCode = "GBP")
        val tx = TransactionEntity(
            id = "tx6", amount = 200.0, type = "INCOME",
            categoryId = "refund", accountId = "acc4",
            note = "Refund", timestamp = 6000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap + ("acc4" to accountGBP),
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        assertTrue(result.rows[0].amount.contains("GBP"))
        assertTrue(result.rows[0].amount.contains("(unconverted)"))
        assertEquals(0.0, result.summary.totalIncome, 0.001)
    }

    @Test
    fun `mixed transactions produce correct summary`() {
        val income = TransactionEntity(
            id = "txI", amount = 1000.0, type = "INCOME",
            categoryId = "salary", accountId = "acc1",
            note = "Pay", timestamp = 100L
        )
        val expense = TransactionEntity(
            id = "txE", amount = 300.0, type = "EXPENSE",
            categoryId = "rent", accountId = "acc1",
            note = "Rent", timestamp = 200L
        )
        val transfer = TransactionEntity(
            id = "txT", amount = 200.0, type = "TRANSFER",
            accountId = "acc1", fromAccountId = "acc1", toAccountId = "acc2",
            note = "Savings", timestamp = 300L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(income, expense, transfer),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        assertEquals(3, result.rows.size)
        assertTrue(result.rows[0].isTransfer.not())
        assertTrue(result.rows[1].isTransfer.not())
        assertTrue(result.rows[2].isTransfer)
        assertEquals(1000.0, result.summary.totalIncome, 0.001)
        assertEquals(300.0, result.summary.totalExpense, 0.001)
        assertEquals(700.0, result.summary.net, 0.001)
    }

    @Test
    fun `transfer with multi-currency is converted`() {
        val tx = TransactionEntity(
            id = "txT2", amount = 100.0, type = "TRANSFER",
            accountId = "acc2", fromAccountId = "acc2", toAccountId = "acc1",
            note = "USD to KES", timestamp = 7000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        val row = result.rows[0]
        assertTrue(row.isTransfer)
        // 100 USD * 150 = 15000 KES
        assertTrue(row.amount.filter(Char::isDigit).contains("15000"))
        assertEquals("From Savings USD to Main Checking", row.transferDetail)
        assertEquals(0.0, result.summary.totalIncome, 0.001)
        assertEquals(0.0, result.summary.totalExpense, 0.001)
    }

    @Test
    fun `transaction with missing account uses a readable fallback instead of an id`() {
        val tx = TransactionEntity(
            id = "txMissing", amount = 50.0, type = "EXPENSE",
            categoryId = "misc", accountId = "unknown_acc",
            note = "Unknown", timestamp = 8000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        val row = result.rows[0]
        assertEquals("Unlinked account", row.account)
        assertEquals("Uncategorized", row.category)
        // Same display currency (default), so no conversion
        assertTrue(row.amount.contains("KES"))
        assertTrue(row.amount.filter(Char::isDigit).contains("50"))
    }

    @Test
    fun `transfer with missing source and destination uses readable fallbacks`() {
        val tx = TransactionEntity(
            id = "txNull", amount = 100.0, type = "TRANSFER",
            accountId = "acc1", fromAccountId = null, toAccountId = null,
            note = "Internal", timestamp = 9000L
        )

        val result = PdfExportDataBuilder.build(
            transactions = listOf(tx),
            accountMap = accountMap,
            exchangeRates = exchangeRates,
            displayCurrency = "KES",
            dateFmt = ::dateFmt
        )

        val row = result.rows[0]
        assertTrue(row.isTransfer)
        assertEquals("From Unlinked account to Unlinked account", row.transferDetail)
    }
}
