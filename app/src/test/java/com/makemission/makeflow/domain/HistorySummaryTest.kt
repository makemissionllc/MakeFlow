package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySummaryTest {
    private val accounts = listOf(
        AccountEntity("cash", "Cash", 0.0, 0.0, currencyCode = "KES"),
        AccountEntity("bank", "Bank", 0.0, 0.0, currencyCode = "USD"),
        AccountEntity("legacy", "Legacy", 0.0, 0.0)
    )

    private fun transaction(amount: Double, type: String = "EXPENSE", accountId: String = "cash") =
        TransactionEntity(amount = amount, type = type, accountId = accountId)

    @Test
    fun emptyResultsHaveNoTotals() {
        assertEquals(HistorySummary.Result(), HistorySummary.summarize(emptyList(), accounts, "KES"))
    }

    @Test
    fun incomeSpendingAndNetStaySeparateForEachCurrency() {
        val result = HistorySummary.summarize(listOf(
            transaction(1200.0, "INCOME"), transaction(250.0),
            transaction(40.0, "INCOME", "bank"), transaction(10.0, accountId = "bank")
        ), accounts, "KES")
        assertEquals(4, result.transactionCount)
        assertEquals(0, result.excludedCount)
        assertEquals(listOf("KES", "USD"), result.currencies.map { it.currencyCode })
        assertMoney("1200", result.currencies[0].income)
        assertMoney("250", result.currencies[0].spending)
        assertMoney("950", result.currencies[0].net)
        assertMoney("30", result.currencies[1].net)
    }

    @Test
    fun transfersNeverBecomeIncomeOrSpendingEvenForDestinationAccount() {
        val transfer = transaction(500.0, "TRANSFER").copy(
            fromAccountId = "cash", toAccountId = "bank", destinationAmount = 4.0
        )
        val result = HistorySummary.summarize(listOf(transfer), accounts, "USD")
        assertEquals(1, result.transactionCount)
        assertEquals(1, result.transferCount)
        assertEquals(0, result.excludedCount)
        assertTrue(result.currencies.isEmpty())
    }

    @Test
    fun legacyAccountsUseDefaultCurrencyButExplicitCurrenciesDoNotChange() {
        val rows = listOf(transaction(20.0, accountId = "legacy"), transaction(10.0))
        val result = HistorySummary.summarize(rows, accounts, "EUR")
        assertEquals(listOf("EUR", "KES"), result.currencies.map { it.currencyCode })
        assertMoney("20", result.currencies[0].spending)
        assertEquals(listOf("KES", "USD"), HistorySummary.summarize(rows, accounts, "USD").currencies.map { it.currencyCode })
    }

    @Test
    fun missingAccountsInvalidAmountsTypesAndCurrenciesAreDisclosedNotMixed() {
        val invalidAccounts = accounts + listOf(
            AccountEntity("invalid", "Invalid", 0.0, 0.0, currencyCode = "INVALID"),
            AccountEntity("blank", "Blank", 0.0, 0.0, currencyCode = "")
        )
        val result = HistorySummary.summarize(listOf(
            transaction(12.0), transaction(100.0, accountId = "missing"),
            transaction(Double.NaN), transaction(Double.POSITIVE_INFINITY), transaction(Double.NEGATIVE_INFINITY),
            transaction(100.0, "UNKNOWN"), transaction(100.0, accountId = "invalid"),
            transaction(100.0, accountId = "blank")
        ), invalidAccounts, "KES")
        assertEquals(8, result.transactionCount)
        assertEquals(7, result.excludedCount)
        assertMoney("12", result.currencies.single().spending)
    }

    @Test
    fun decimalSumsAreExactAndLargeTotalsDoNotOverflow() {
        val result = HistorySummary.summarize(listOf(transaction(0.1), transaction(0.2)), accounts, "KES")
        assertMoney("0.3", result.currencies.single().spending)
        val large = HistorySummary.summarize(listOf(transaction(Double.MAX_VALUE), transaction(Double.MAX_VALUE)), accounts, "KES")
        assertMoney((BigDecimal.valueOf(Double.MAX_VALUE) * BigDecimal("2")).toPlainString(), large.currencies.single().spending)
    }

    @Test
    fun signedLegacyAmountsKeepTheirRecordedMeaning() {
        val result = HistorySummary.summarize(listOf(transaction(20.0), transaction(-5.0)), accounts, "KES")
        assertMoney("15", result.currencies.single().spending)
        assertMoney("-15", result.currencies.single().net)
    }

    @Test
    fun currencyCodesAreNormalizedAndResultsAreOrderIndependent() {
        val normalized = accounts + AccountEntity("eur", "Euro", 0.0, 0.0, currencyCode = " eur ")
        val rows = listOf(transaction(20.0, accountId = "bank"), transaction(2.0, accountId = "eur"), transaction(4.0))
        val result = HistorySummary.summarize(rows, normalized, "KES")
        assertEquals(listOf("EUR", "KES", "USD"), result.currencies.map { it.currencyCode })
        assertEquals(result, HistorySummary.summarize(rows.reversed(), normalized, "KES"))
    }

    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
