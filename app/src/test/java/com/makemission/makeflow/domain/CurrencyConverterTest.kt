package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyConverterTest {
    private val rates = listOf(
        CurrencyRate("USD", "KES", 130.0),
        CurrencyRate("EUR", "USD", 1.1)
    )

    @Test
    fun convertsOnlyAgainstTheRequestedBase() {
        assertEquals(260.0, CurrencyConverter.convert(2.0, "USD", "KES", rates)!!, 0.0001)
        assertNull(CurrencyConverter.convert(2.0, "EUR", "KES", rates))
    }

    @Test
    fun sameCurrencyDoesNotNeedAnExchangeRate() {
        assertEquals(42.0, CurrencyConverter.convert(42.0, "KES", "KES", emptyList())!!, 0.0)
    }

    @Test
    fun convertsBetweenTwoQuotesUsingTheirSharedBase() {
        val rates = listOf(
            CurrencyRate("USD", "KES", 130.0),
            CurrencyRate("EUR", "KES", 150.0)
        )

        assertEquals(8.6666667, CurrencyConverter.convert(10.0, "USD", "EUR", rates)!!, 0.0001)
    }

    @Test
    fun invalidRatesAreRejected() {
        assertNull(CurrencyConverter.convert(10.0, "USD", "KES", listOf(CurrencyRate("USD", "KES", Double.NaN))))
        assertNull(CurrencyConverter.convert(10.0, "USD", "KES", listOf(CurrencyRate("USD", "KES", 0.0))))
    }
}
