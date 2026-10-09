package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvStatementBuilderTest {
    private val zone = ZoneId.of("Africa/Nairobi")
    private val timestamp = ZonedDateTime.parse("2026-10-09T12:00:00+03:00[Africa/Nairobi]").toInstant().toEpochMilli()
    private val cash = AccountEntity("cash", "Daily wallet", 0.0, 0.0, currencyCode = "KES")
    private val usd = AccountEntity("usd", "Dollar account", 0.0, 0.0, currencyCode = "USD")
    private val food = CategoryEntity("food", "Food", "EXPENSE")
    private val transaction = TransactionEntity(
        id = "entry", amount = 250.5, type = "EXPENSE", categoryId = food.id,
        accountId = cash.id, note = "Lunch", timestamp = timestamp, externalReference = "ABC123"
    )

    private fun build(
        transactions: List<TransactionEntity> = listOf(transaction),
        accounts: Map<String, AccountEntity> = listOf(cash, usd).associateBy { it.id },
        categories: Map<String, CategoryEntity> = mapOf(food.id to food),
        defaultCurrency: String = "KES"
    ) = CsvStatementBuilder.build(transactions, accounts, categories, defaultCurrency, zone)

    @Test
    fun exportsReadableNamesCurrencyOffsetAndReference() {
        val row = build().substringAfter("\r\n")
        assertEquals(
            "\"2026-10-09T12:00:00+03:00\",\"EXPENSE\",250.5,\"KES\",\"Daily wallet\",\"Food\",\"Lunch\",\"\",\"\",,\"\",\"ABC123\"\r\n",
            row
        )
    }

    @Test
    fun quotesCommasDoubleQuotesAndMultilineUnicodeNotes() {
        val note = "Caf\u00e9, \"lunch\"\r\nwith friends"
        val csv = build(listOf(transaction.copy(note = note)), accounts = mapOf("cash" to cash.copy(name = "Cash, \"daily\"")))
        assertTrue(csv.contains("\"Caf\u00e9, \"\"lunch\"\"\r\nwith friends\""))
        assertTrue(csv.contains("\"Cash, \"\"daily\"\"\""))
    }

    @Test
    fun formulaLikeNotesArePrefixedInsideQuotedCells() {
        listOf("=1+2", "+SUM(1,2)", "-1+2", "@SUM(1,2)", "  =1+2", "\r\n=1+2", "\t=1+2", "\uFF1D1+2").forEach { note ->
            val csv = build(listOf(transaction.copy(note = note)))
            assertTrue("Missing guard for $note", csv.contains("\"\t${note.replace("\"", "\"\"")}\""))
        }
    }

    @Test
    fun guardsAccountCategoryAndReferenceTextAsWellAsNotes() {
        val csv = build(
            listOf(transaction.copy(externalReference = "+reference")),
            accounts = mapOf(cash.id to cash.copy(name = "=account")),
            categories = mapOf(food.id to food.copy(name = "@category"))
        )
        listOf("=account", "@category", "+reference").forEach {
            assertTrue(csv.contains("\"\t$it\""))
        }
    }

    @Test
    fun exportsBothTransferCurrenciesAndOriginalDestinationAmount() {
        val transfer = transaction.copy(
            amount = 10.0, type = "TRANSFER", accountId = "legacy-wrong-id",
            fromAccountId = usd.id, toAccountId = cash.id, destinationAmount = 1280.0
        )
        val row = build(listOf(transfer)).substringAfter("\r\n")
        assertTrue(row.contains("\"TRANSFER\",10,\"USD\",\"Dollar account\",\"Transfer\""))
        assertTrue(row.contains("\"Dollar account\",\"Daily wallet\",1280,\"KES\""))
    }

    @Test
    fun missingDestinationAmountIsNotInvented() {
        val transfer = transaction.copy(type = "TRANSFER", fromAccountId = usd.id, toAccountId = cash.id)
        assertTrue(build(listOf(transfer)).contains("\"Dollar account\",\"Daily wallet\",,\"KES\""))
    }

    @Test
    fun missingAccountsDoNotLeakIdsOrInventCurrency() {
        val csv = build(accounts = emptyMap(), categories = emptyMap())
        assertTrue(csv.contains("250.5,\"\",\"Unlinked account\",\"Uncategorized\""))
        assertFalse(csv.contains("\"cash\""))
        assertFalse(csv.contains("\"food\""))
    }

    @Test
    fun legacyAccountUsesDefaultCurrencyWithoutConvertingAmount() {
        val csv = build(accounts = mapOf(cash.id to cash.copy(currencyCode = null)), defaultCurrency = "USD")
        assertTrue(csv.contains("250.5,\"USD\""))
    }

    @Test
    fun subcategoryIncludesParentName() {
        val child = CategoryEntity("lunch", "Lunch", "EXPENSE", parentCategoryId = food.id)
        val csv = build(listOf(transaction.copy(categoryId = child.id)), categories = listOf(food, child).associateBy { it.id })
        assertTrue(csv.contains("\"Food / Lunch\""))
    }

    @Test
    fun numericAmountsAreLocaleIndependentAndNotScientificNotation() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertTrue(build(listOf(transaction.copy(amount = 1250.5))).contains(",1250.5,"))
            assertTrue(build(listOf(transaction.copy(amount = 1e8))).contains(",100000000,"))
            assertTrue(build(listOf(transaction.copy(amount = -10.0))).contains(",-10,"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun emptyHistoryExportsHeaderOnly() {
        val csv = build(emptyList())
        assertEquals("Date,Type,Amount,Currency,Account,Category,Note,From account,To account,Destination amount,Destination currency,Reference\r\n", csv)
    }

    @Test
    fun rowsAreOrderedByDateThenIdentity() {
        val csv = build(listOf(
            transaction.copy(id = "b", note = "second"),
            transaction.copy(id = "a", note = "first"),
            transaction.copy(id = "c", note = "older", timestamp = timestamp - 1)
        ))
        assertTrue(csv.indexOf("\"older\"") < csv.indexOf("\"first\""))
        assertTrue(csv.indexOf("\"first\"") < csv.indexOf("\"second\""))
    }

    @Test
    fun fileNameShowsInclusiveDatesInLocalZone() {
        val range = HistoryPeriod.THIS_MONTH.timestampRange(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), zone).toLocalDate(), zone)!!
        assertEquals("MakeFlow_Transactions_2026-10-01_to_2026-10-31.csv", CsvStatementBuilder.fileName(range.first, range.last + 1, timestamp, zone))
    }

    @Test
    fun allTimeFileNameIncludesGenerationDate() {
        assertEquals("MakeFlow_Transactions_All_Time_2026-10-09.csv", CsvStatementBuilder.fileName(0L, Long.MAX_VALUE, timestamp, zone))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDateRangeIsRejected() {
        CsvStatementBuilder.fileName(timestamp, timestamp, timestamp, zone)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonFiniteAmountsAreRejected() {
        build(listOf(transaction.copy(amount = Double.NaN)))
    }
}
