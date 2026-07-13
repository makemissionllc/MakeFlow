package com.makemission.makeflow.ui.viewmodel

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity

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

        val rows = transactions.map { tx ->
            val accountName = accountMap[tx.accountId]?.name ?: tx.accountId
            val date = dateFmt(tx.timestamp)
            val note = tx.note ?: ""

            if (tx.type == "TRANSFER") {
                val fromName = tx.fromAccountId?.let { accountMap[it]?.name } ?: tx.fromAccountId ?: ""
                val toName = tx.toAccountId?.let { accountMap[it]?.name } ?: tx.toAccountId ?: ""
                val converted = convertAmount(tx.amount, tx.accountId, accountMap, exchangeRates, displayCurrency)
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
                val converted = convertAmount(tx.amount, tx.accountId, accountMap, exchangeRates, displayCurrency)
                val categoryName = tx.categoryId ?: ""

                if (tx.type == "INCOME") totalIncome += parseConvertedAmount(tx.amount, tx.accountId, accountMap, exchangeRates, displayCurrency)
                else totalExpense += parseConvertedAmount(tx.amount, tx.accountId, accountMap, exchangeRates, displayCurrency)

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

    private fun convertAmount(
        amount: Double,
        accountId: String,
        accountMap: Map<String, AccountEntity>,
        exchangeRates: Map<String, ExchangeRateEntity>,
        displayCurrency: String
    ): String {
        val accountCurrency = accountMap[accountId]?.currencyCode ?: displayCurrency
        val converted = if (accountCurrency != displayCurrency) {
            val rate = exchangeRates[accountCurrency]?.rateToDisplayCurrency
            if (rate != null && rate > 0) amount * rate else amount
        } else {
            amount
        }
        return "${converted}"
    }

    private fun parseConvertedAmount(
        amount: Double,
        accountId: String,
        accountMap: Map<String, AccountEntity>,
        exchangeRates: Map<String, ExchangeRateEntity>,
        displayCurrency: String
    ): Double {
        val accountCurrency = accountMap[accountId]?.currencyCode ?: displayCurrency
        if (accountCurrency == displayCurrency) return amount
        val rate = exchangeRates[accountCurrency]?.rateToDisplayCurrency
        return if (rate != null && rate > 0) amount * rate else amount
    }
}
