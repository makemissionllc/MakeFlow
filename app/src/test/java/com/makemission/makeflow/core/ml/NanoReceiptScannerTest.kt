package com.makemission.makeflow.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NanoReceiptScannerTest {
    @Test
    fun parsesAmountAndMerchantFromJsonResponse() {
        val parsed = NanoReceiptScanner.parseSuggestion(
            """{"total": 1250.50, "merchant": "Market Square"}"""
        )

        assertEquals(1250.50, parsed?.amount ?: 0.0, 0.001)
        assertEquals("Market Square", parsed?.merchant)
    }

    @Test
    fun acceptsFormattedAmountAndOptionalMerchant() {
        val parsed = NanoReceiptScanner.parseSuggestion(
            """{"total": "1,250.00", "merchant": null}"""
        )

        assertEquals(1250.0, parsed?.amount ?: 0.0, 0.001)
        assertNull(parsed?.merchant)
    }

    @Test
    fun rejectsMissingNonFiniteAndOutOfRangeTotals() {
        assertNull(NanoReceiptScanner.parseSuggestion("""{"total": null}"""))
        assertNull(NanoReceiptScanner.parseSuggestion("""{"total": "NaN"}"""))
        assertNull(NanoReceiptScanner.parseSuggestion("""{"total": -25}"""))
        assertNull(NanoReceiptScanner.parseSuggestion("""{"total": 1000000001}"""))
        assertNull(NanoReceiptScanner.parseSuggestion("not JSON"))
    }
}
