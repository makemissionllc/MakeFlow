package com.makemission.makeflow.core.format

import java.util.Currency
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val name: String
)

object CurrencyCatalog {
    private val preferredCodes = listOf("KES", "USD", "EUR", "GBP", "UGX", "TZS")

    val currencies: List<CurrencyOption> by lazy {
        val available = Currency.getAvailableCurrencies().map { currency ->
            CurrencyOption(currency.currencyCode, currency.getDisplayName(Locale.getDefault()))
        }
        val byCode = available.associateBy { it.code }
        preferredCodes.mapNotNull { byCode[it] } + available
            .filterNot { it.code in preferredCodes }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    fun filter(query: String): List<CurrencyOption> {
        if (query.isBlank()) return currencies
        val normalized = query.trim().lowercase(Locale.getDefault())
        return currencies.filter {
            it.code.lowercase(Locale.getDefault()).contains(normalized) ||
                it.name.lowercase(Locale.getDefault()).contains(normalized)
        }
    }
}
