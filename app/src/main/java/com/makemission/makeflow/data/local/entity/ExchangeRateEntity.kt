package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String,
    val rateToDisplayCurrency: Double,
    val lastUpdated: Long = System.currentTimeMillis(),
    val baseCurrencyCode: String? = null,
    val rateDate: String? = null
)
