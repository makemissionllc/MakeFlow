package com.makemission.makeflow.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FrankfurterExchangeRateApi {
    suspend fun fetchLatest(baseCurrencyCode: String): ExchangeRateSnapshot = withContext(Dispatchers.IO) {
        require(baseCurrencyCode.matches(Regex("^[A-Z]{3}$"))) { "Invalid base currency" }
        val connection = (URL("https://api.frankfurter.dev/v2/rates?base=$baseCurrencyCode")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val responseCode = connection.responseCode
            val response = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (responseCode !in 200..299) {
                throw IOException("Exchange-rate service returned HTTP $responseCode")
            }
            FrankfurterRatesParser.parse(response, baseCurrencyCode)
        } finally {
            connection.disconnect()
        }
    }
}
