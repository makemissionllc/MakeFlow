package com.makemission.makeflow.ui.viewmodel

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        categoryMap: Map<String, CategoryEntity> = emptyMap(),
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
            val accountName = accountMap[tx.accountId]?.name ?: "Unlinked account"
            val date = dateFmt(tx.timestamp)
            val note = tx.note?.trim().orEmpty()

            if (tx.type == "TRANSFER") {
                val fromName = tx.fromAccountId?.let { accountMap[it]?.name } ?: "Unlinked account"
                val toName = tx.toAccountId?.let { accountMap[it]?.name } ?: "Unlinked account"
                val fromAccountId = tx.fromAccountId ?: tx.accountId
                val fromCurrency = accountMap[fromAccountId]?.currencyCode ?: displayCurrency
                val toCurrency = tx.toAccountId?.let { accountMap[it]?.currencyCode } ?: displayCurrency
                val converted = if (tx.destinationAmount != null) {
                    "${amountLabel(tx.amount, fromCurrency, displayCurrency, conversionRates)} to " +
                        "${amountLabel(tx.destinationAmount, toCurrency, displayCurrency, conversionRates)}"
                } else {
                    amountLabel(tx.amount, fromCurrency, displayCurrency, conversionRates)
                }
                PdfRow(
                    date = date,
                    description = note.ifBlank { "Account transfer" },
                    category = "Transfer",
                    account = accountName,
                    amount = converted,
                    isTransfer = true,
                    transferDetail = "From $fromName to $toName"
                )
            } else {
                val accountCurrency = accountMap[tx.accountId]?.currencyCode ?: displayCurrency
                val converted = amountLabel(tx.amount, accountCurrency, displayCurrency, conversionRates)
                val categoryName = tx.categoryId?.let { categoryMap[it]?.name } ?: "Uncategorized"

                val summaryAmount = CurrencyConverter.convert(tx.amount, accountCurrency, displayCurrency, conversionRates)
                if (tx.type == "INCOME" && summaryAmount != null) totalIncome += summaryAmount
                else if (tx.type == "EXPENSE" && summaryAmount != null) totalExpense += summaryAmount

                PdfRow(
                    date = date,
                    description = note.ifBlank {
                        if (tx.type == "INCOME") "Income" else "Expense"
                    },
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
        return if (converted != null) {
            formatAmount(converted, displayCurrency)
        } else {
            "${formatAmount(amount, accountCurrency)} (unconverted)"
        }
    }

    private fun formatAmount(amount: Double, currencyCode: String): String {
        val digits = try {
            java.util.Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
        } catch (_: Exception) {
            2
        }
        val number = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = digits.coerceAtMost(2)
            minimumFractionDigits = 0
        }.format(amount)
        return "$currencyCode $number"
    }
}

object PdfStatementFileName {
    fun forRange(
        startMillis: Long,
        endMillis: Long,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        if (startMillis <= 0L && endMillis == Long.MAX_VALUE) {
            val generatedDate = dateFormat.format(Date(generatedAtMillis))
            return "MakeFlow_Statement_All_Time_$generatedDate.pdf"
        }

        val inclusiveEnd = if (endMillis > startMillis) endMillis - 1 else endMillis
        return "MakeFlow_Statement_${dateFormat.format(Date(startMillis))}_to_" +
            "${dateFormat.format(Date(inclusiveEnd))}.pdf"
    }
}
