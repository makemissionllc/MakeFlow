package com.makemission.makeflow.data.repository

import com.google.gson.JsonParser
import java.time.LocalDate

data class ExchangeRateSnapshot(
    val baseCurrencyCode: String,
    val rateDate: String,
    val quoteRates: Map<String, Double>
)

object FrankfurterRatesParser {
    fun parse(json: String, expectedBaseCurrency: String): ExchangeRateSnapshot {
        val rows = JsonParser.parseString(json).asJsonArray
        require(rows.size() > 0) { "The exchange-rate response was empty" }

        val quoteRates = linkedMapOf<String, Double>()
        val rateDates = mutableListOf<LocalDate>()
        rows.forEach { element ->
            val row = element.asJsonObject
            val base = row.get("base").asString.uppercase()
            require(base == expectedBaseCurrency) { "The response base currency did not match" }
            val quote = row.get("quote").asString.uppercase()
            require(quote.matches(Regex("^[A-Z]{3}$"))) { "The response contained an invalid currency code" }
            val rate = row.get("rate").asDouble
            require(rate.isFinite() && rate > 0.0) { "The response contained an invalid exchange rate" }
            quoteRates[quote] = rate
            rateDates += LocalDate.parse(row.get("date").asString)
        }

        return ExchangeRateSnapshot(
            baseCurrencyCode = expectedBaseCurrency,
            rateDate = rateDates.maxOrNull()?.toString() ?: error("The response did not contain a rate date"),
            quoteRates = quoteRates
        )
    }
}
