package com.makemission.makeflow.core.format

import java.math.BigDecimal
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

    fun format(amount: Double, currencyCode: String): String = formatAmount(amount, currencyCode)

    fun format(amount: BigDecimal, currencyCode: String): String = formatAmount(amount, currencyCode)

    private fun formatAmount(amount: Number, currencyCode: String): String {
        val locale = currencyLocaleMap[currencyCode] ?: Locale.getDefault()
        return try {
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = Currency.getInstance(currencyCode)
            format.maximumFractionDigits = fractionDigitsFor(currencyCode)
            format.minimumFractionDigits = fractionDigitsFor(currencyCode)
            format.format(amount)
        } catch (e: Exception) {
            fallbackFormat(amount, currencyCode)
        }
    }

    fun formatCompact(amount: Double, currencyCode: String): String {
        val symbol = if (currencyCode == "KES") "KSh" else symbolFor(currencyCode)
        val decimalPlaces = if (fractionDigitsFor(currencyCode) == 0 || amount % 1.0 == 0.0) 0
        else fractionDigitsFor(currencyCode).coerceAtMost(2)
        val separator = if (symbol.length == 1) "" else " "
        val sign = if (amount < 0.0) "-" else ""
        return sign + symbol + separator + formatNumber(kotlin.math.abs(amount), decimalPlaces)
    }

    fun symbolFor(currencyCode: String): String {
        return try {
            val locale = currencyLocaleMap[currencyCode] ?: Locale.getDefault()
            Currency.getInstance(currencyCode).getSymbol(locale)
        } catch (e: Exception) {
            legacySymbolFor(currencyCode)
        }
    }

    private fun fallbackFormat(amount: Number, currencyCode: String): String {
        val symbol = legacySymbolFor(currencyCode)
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = fractionDigitsFor(currencyCode)
            minimumFractionDigits = fractionDigitsFor(currencyCode)
        }
        return "$symbol${numberFormat.format(amount)}"
    }

    private fun fractionDigitsFor(currencyCode: String): Int = try {
        Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    } catch (_: Exception) {
        2
    }

    fun fractionDigits(currencyCode: String): Int = fractionDigitsFor(currencyCode)

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
