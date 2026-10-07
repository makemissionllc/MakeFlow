package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.entity.ExchangeRateEntity

class ExchangeRateSyncer(
    private val repository: ExchangeRateRepository,
    private val api: FrankfurterExchangeRateApi = FrankfurterExchangeRateApi()
) {
    suspend fun refresh(baseCurrencyCode: String, force: Boolean = false): ExchangeRateRefreshResult {
        val existing = repository.getAllRatesOnce()
            .filter { it.baseCurrencyCode == baseCurrencyCode }
        val now = System.currentTimeMillis()
        val mostRecent = existing.maxOfOrNull { it.lastUpdated } ?: 0L
        if (!force && existing.isNotEmpty() && now - mostRecent < REFRESH_INTERVAL_MILLIS) {
            return ExchangeRateRefreshResult(
                updated = false,
                rateDate = existing.mapNotNull { it.rateDate }.maxOrNull(),
                rateCount = existing.size
            )
        }

        val snapshot = api.fetchLatest(baseCurrencyCode)
        val updatedAt = System.currentTimeMillis()
        val rates = buildList {
            add(
                ExchangeRateEntity(
                    currencyCode = baseCurrencyCode,
                    rateToDisplayCurrency = 1.0,
                    lastUpdated = updatedAt,
                    baseCurrencyCode = baseCurrencyCode,
                    rateDate = snapshot.rateDate
                )
            )
            snapshot.quoteRates.forEach { (quote, quoteUnitsPerBaseUnit) ->
                add(
                    ExchangeRateEntity(
                        currencyCode = quote,
                        rateToDisplayCurrency = 1.0 / quoteUnitsPerBaseUnit,
                        lastUpdated = updatedAt,
                        baseCurrencyCode = baseCurrencyCode,
                        rateDate = snapshot.rateDate
                    )
                )
            }
        }
        repository.replaceRates(rates)
        return ExchangeRateRefreshResult(updated = true, rateDate = snapshot.rateDate, rateCount = rates.size)
    }

    companion object {
        const val REFRESH_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
    }
}

data class ExchangeRateRefreshResult(
    val updated: Boolean,
    val rateDate: String?,
    val rateCount: Int
)
