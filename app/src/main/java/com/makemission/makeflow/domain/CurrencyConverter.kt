package com.makemission.makeflow.domain

data class CurrencyRate(
    val currencyCode: String,
    val baseCurrencyCode: String,
    val baseUnitsPerCurrencyUnit: Double,
    val rateDate: String? = null
)

object CurrencyConverter {
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        rates: List<CurrencyRate>
    ): Double? {
        if (fromCurrency == toCurrency) return amount
        val directRate = rates.firstOrNull {
            it.currencyCode == fromCurrency && it.baseCurrencyCode == toCurrency
        }?.baseUnitsPerCurrencyUnit
        if (directRate != null) {
            if (!directRate.isFinite() || directRate <= 0.0) return null
            return amount * directRate
        }

        val fromRate = rates.firstOrNull { it.currencyCode == fromCurrency } ?: return null
        val toRate = rates.firstOrNull {
            it.currencyCode == toCurrency && it.baseCurrencyCode == fromRate.baseCurrencyCode
        } ?: return null
        if (!fromRate.baseUnitsPerCurrencyUnit.isFinite() || fromRate.baseUnitsPerCurrencyUnit <= 0.0 ||
            !toRate.baseUnitsPerCurrencyUnit.isFinite() || toRate.baseUnitsPerCurrencyUnit <= 0.0
        ) return null
        return amount * fromRate.baseUnitsPerCurrencyUnit / toRate.baseUnitsPerCurrencyUnit
    }
}
