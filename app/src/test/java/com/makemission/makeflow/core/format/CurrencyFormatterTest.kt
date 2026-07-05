package com.makemission.makeflow.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun ugx_formatsWithZeroDecimals() {
        val result = CurrencyFormatter.format(15000.0, "UGX")
        assertTrue("UGX should have 0 decimal places", !result.contains("."))
        assertTrue(result.contains("15"))
        assertTrue(result.contains(","))
    }

    @Test
    fun usd_formatsWithTwoDecimals() {
        val result = CurrencyFormatter.format(1234.5, "USD")
        assertTrue("USD should have 2 decimal places", result.contains(".50"))
        assertTrue(result.contains("$") || result.contains("USD"))
    }

    @Test
    fun unknownCurrency_doesNotCrash() {
        val result = CurrencyFormatter.format(100.0, "XYZ")
        assertTrue(result.contains("KES"))
        assertTrue(result.contains("100"))
    }

    @Test
    fun largeNumber_groupsCorrectly() {
        val result = CurrencyFormatter.format(1_234_567.89, "USD")
        assertTrue(result.contains("1,234,567"))
    }

    @Test
    fun negativeAmount_formatsCorrectly() {
        val result = CurrencyFormatter.format(-500.0, "USD")
        assertTrue(result.contains("500"))
        assertTrue(result.contains("($500.00)") || result.contains("-$500.00") || result.contains("-500") || result.contains("(500"))
    }

    @Test
    fun formatNumber_groupsAndFormatsCorrectly() {
        val result = CurrencyFormatter.formatNumber(1234567.89, 2)
        assertTrue(result.contains("1234567") || result.contains("1,234,567") || result.contains("1 234 567") || result.contains("1.234.567"))
        assertTrue(result.contains("89"))
    }
}
