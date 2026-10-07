package com.makemission.makeflow.ui.viewmodel

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate

data class PdfRow(
    val date: String,
    val description: String,
    val category: String,
    val account: String,
    val amount: String,
    val isTransfer: Boolean,
    val transferDetail: String
)

data class PdfSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val net: Double
)

data class PdfExportData(
    val rows: List<PdfRow>,
    val summary: PdfSummary
)

object PdfExportDataBuilder {

    fun build(
        transactions: List<TransactionEntity>,
        accountMap: Map<String, AccountEntity>,
        exchangeRates: Map<String, ExchangeRateEntity>,
        displayCurrency: String,
        dateFmt: (Long) -> String
    ): PdfExportData {
        var totalIncome = 0.0
        var totalExpense = 0.0
        val conversionRates = exchangeRates.values.mapNotNull { rate ->
            rate.baseCurrencyCode?.let { base ->
                CurrencyRate(rate.currencyCode, base, rate.rateToDisplayCurrency, rate.rateDate)
            }
        }

        val rows = transactions.map { tx ->
            val accountName = accountMap[tx.accountId]?.name ?: tx.accountId
            val date = dateFmt(tx.timestamp)
            val note = tx.note ?: ""

            if (tx.type == "TRANSFER") {
                val fromName = tx.fromAccountId?.let { accountMap[it]?.name } ?: tx.fromAccountId ?: ""
                val toName = tx.toAccountId?.let { accountMap[it]?.name } ?: tx.toAccountId ?: ""
                val fromAccountId = tx.fromAccountId ?: tx.accountId
                val fromCurrency = accountMap[fromAccountId]?.currencyCode ?: displayCurrency
                val toCurrency = tx.toAccountId?.let { accountMap[it]?.currencyCode } ?: displayCurrency
                val converted = if (tx.destinationAmount != null) {
                    "${amountLabel(tx.amount, fromCurrency, displayCurrency, conversionRates)} → " +
                        "${amountLabel(tx.destinationAmount, toCurrency, displayCurrency, conversionRates)}"
                } else {
                    amountLabel(tx.amount, fromCurrency, displayCurrency, conversionRates)
                }
                PdfRow(
                    date = date,
                    description = note,
                    category = "Transfer",
                    account = accountName,
                    amount = converted,
                    isTransfer = true,
                    transferDetail = "$fromName \u2192 $toName"
                )
            } else {
                val accountCurrency = accountMap[tx.accountId]?.currencyCode ?: displayCurrency
                val converted = amountLabel(tx.amount, accountCurrency, displayCurrency, conversionRates)
                val categoryName = tx.categoryId ?: ""

                val summaryAmount = CurrencyConverter.convert(tx.amount, accountCurrency, displayCurrency, conversionRates)
                if (tx.type == "INCOME" && summaryAmount != null) totalIncome += summaryAmount
                else if (tx.type == "EXPENSE" && summaryAmount != null) totalExpense += summaryAmount

                PdfRow(
                    date = date,
                    description = note,
                    category = categoryName,
                    account = accountName,
                    amount = converted,
                    isTransfer = false,
                    transferDetail = ""
                )
            }
        }

        return PdfExportData(
            rows = rows,
            summary = PdfSummary(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                net = totalIncome - totalExpense
            )
        )
    }

    private fun amountLabel(
        amount: Double,
        accountCurrency: String,
        displayCurrency: String,
        rates: List<CurrencyRate>
    ): String {
        val converted = CurrencyConverter.convert(amount, accountCurrency, displayCurrency, rates)
        return converted?.toString() ?: "$amount $accountCurrency (unconverted)"
    }
}
