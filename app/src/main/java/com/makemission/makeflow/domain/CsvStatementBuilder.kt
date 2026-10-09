package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvStatementBuilder {
    fun build(
        transactions: List<TransactionEntity>,
        accounts: Map<String, AccountEntity>,
        categories: Map<String, CategoryEntity>,
        defaultCurrency: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): String = buildString {
        append("Date,Type,Amount,Currency,Account,Category,Note,From account,To account,Destination amount,Destination currency,Reference\r\n")
        transactions.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id }).forEach { tx ->
            val isTransfer = tx.type == "TRANSFER"
            val account = accounts[if (isTransfer) tx.fromAccountId ?: tx.accountId else tx.accountId]
            val destination = tx.toAccountId?.let(accounts::get)
            val category = tx.categoryId?.let(categories::get)
            val parent = category?.parentCategoryId?.let(categories::get)
            val categoryName = when {
                isTransfer -> "Transfer"
                category == null -> "Uncategorized"
                parent != null -> "${parent.name} / ${category.name}"
                else -> category.name
            }
            fun currency(value: AccountEntity?) = value?.let { it.currencyCode ?: defaultCurrency }.orEmpty()
            append(listOf(
                textCell(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(tx.timestamp).atZone(zone))),
                textCell(tx.type),
                numberCell(tx.amount),
                textCell(currency(account)),
                textCell(account?.name ?: "Unlinked account"),
                textCell(categoryName),
                textCell(tx.note),
                textCell(if (isTransfer) account?.name ?: "Unlinked account" else ""),
                textCell(if (isTransfer) destination?.name ?: "Unlinked account" else ""),
                if (isTransfer) tx.destinationAmount?.let(::numberCell).orEmpty() else "",
                textCell(if (isTransfer) currency(destination) else ""),
                textCell(tx.externalReference.orEmpty())
            ).joinToString(","))
            append("\r\n")
        }
    }

    fun fileName(
        startMillis: Long,
        endExclusiveMillis: Long,
        generatedAtMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        require(endExclusiveMillis > startMillis) { "Invalid statement date range" }
        fun date(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toString()
        return if (startMillis == 0L && endExclusiveMillis == Long.MAX_VALUE) {
            "MakeFlow_Transactions_All_Time_${date(generatedAtMillis)}.csv"
        } else {
            "MakeFlow_Transactions_${date(startMillis)}_to_${date(endExclusiveMillis - 1)}.csv"
        }
    }

    private fun numberCell(value: Double): String {
        require(value.isFinite()) { "Invalid transaction amount" }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private fun textCell(value: String): String {
        // Keep formula-like imported text inert when opened directly in a spreadsheet.
        val first = value.trimStart { it.isWhitespace() || it.isISOControl() || it == '\uFEFF' }.firstOrNull()
        val guarded = if (first != null && first in "=+-@\uFF1D\uFF0B\uFF0D\uFF20") "\t$value" else value
        return "\"${guarded.replace("\"", "\"\"")}\""
    }
}
