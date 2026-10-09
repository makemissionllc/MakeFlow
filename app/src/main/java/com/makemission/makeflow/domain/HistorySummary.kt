package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

object HistorySummary {
    data class CurrencyTotals(
        val currencyCode: String,
        val income: BigDecimal = BigDecimal.ZERO,
        val spending: BigDecimal = BigDecimal.ZERO
    ) {
        val net: BigDecimal get() = income - spending
    }

    data class Result(
        val transactionCount: Int = 0,
        val transferCount: Int = 0,
        val excludedCount: Int = 0,
        val currencies: List<CurrencyTotals> = emptyList()
    )

    fun summarize(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        defaultCurrency: String
    ): Result {
        val accountsById = accounts.associateBy { it.id }
        val totals = mutableMapOf<String, CurrencyTotals>()
        val validCurrencies = mutableMapOf<String, Boolean>()
        var transfers = 0
        var excluded = 0
        transactions.forEach { transaction ->
            if (transaction.type == "TRANSFER") {
                transfers++
                return@forEach
            }
            val account = accountsById[transaction.accountId]
            val currency = (account?.currencyCode ?: defaultCurrency).trim().uppercase(Locale.ROOT)
            if (account == null || (transaction.type != "INCOME" && transaction.type != "EXPENSE") ||
                !transaction.amount.isFinite() ||
                !validCurrencies.getOrPut(currency) { runCatching { Currency.getInstance(currency) }.isSuccess }
            ) {
                excluded++
                return@forEach
            }
            val amount = BigDecimal.valueOf(transaction.amount)
            val current = totals.getOrPut(currency) { CurrencyTotals(currency) }
            totals[currency] = if (transaction.type == "INCOME") {
                current.copy(income = current.income + amount)
            } else {
                current.copy(spending = current.spending + amount)
            }
        }
        return Result(transactions.size, transfers, excluded, totals.toSortedMap().values.toList())
    }
}
