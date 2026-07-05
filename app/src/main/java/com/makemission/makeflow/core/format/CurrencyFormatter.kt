package com.makemission.makeflow.core.format

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    private val currencyLocaleMap = mapOf(
        "KES" to Locale.forLanguageTag("en-KE"),
        "USD" to Locale.US,
        "EUR" to Locale.GERMANY,
        "GBP" to Locale.UK,
        "UGX" to Locale.forLanguageTag("en-UG"),
        "TZS" to Locale.forLanguageTag("en-TZ")
    )

    private val zeroDecimalCurrencies = setOf("UGX", "TZS")

    fun format(amount: Double, currencyCode: String): String {
        val locale = currencyLocaleMap[currencyCode] ?: Locale.getDefault()
        return try {
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = Currency.getInstance(currencyCode)
            if (currencyCode in zeroDecimalCurrencies) {
                format.maximumFractionDigits = 0
                format.minimumFractionDigits = 0
            } else {
                format.maximumFractionDigits = 2
                format.minimumFractionDigits = 2
            }
            format.format(amount)
        } catch (e: Exception) {
            fallbackFormat(amount, currencyCode)
        }
    }

    fun symbolFor(currencyCode: String): String {
        return try {
            val locale = currencyLocaleMap[currencyCode] ?: Locale.getDefault()
            Currency.getInstance(currencyCode).getSymbol(locale)
        } catch (e: Exception) {
            legacySymbolFor(currencyCode)
        }
    }

    private fun fallbackFormat(amount: Double, currencyCode: String): String {
        val symbol = legacySymbolFor(currencyCode)
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = if (currencyCode in zeroDecimalCurrencies) 0 else 2
            minimumFractionDigits = if (currencyCode in zeroDecimalCurrencies) 0 else 2
        }
        return "$symbol${numberFormat.format(amount)}"
    }

    private fun legacySymbolFor(currencyCode: String): String = when (currencyCode) {
        "USD" -> "$"
        "EUR" -> "\u20AC"
        "GBP" -> "\u00A3"
        "UGX" -> "UGX"
        "TZS" -> "TZS"
        else -> "KES"
    }

    fun formatNumber(value: Double, decimalPlaces: Int = 0): String {
        return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = decimalPlaces
            minimumFractionDigits = decimalPlaces
        }.format(value)
    }
}
