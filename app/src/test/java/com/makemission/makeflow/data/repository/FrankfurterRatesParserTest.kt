package com.makemission.makeflow.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FrankfurterRatesParserTest {
    @Test
    fun parsesDailyQuotesForTheRequestedBase() {
        val snapshot = FrankfurterRatesParser.parse(
            """[{"date":"2026-10-06","base":"KES","quote":"USD","rate":0.0077},{"date":"2026-10-06","base":"KES","quote":"EUR","rate":0.0066}]""",
            "KES"
        )

        assertEquals("2026-10-06", snapshot.rateDate)
        assertEquals(0.0077, snapshot.quoteRates["USD"]!!, 0.000001)
        assertEquals(2, snapshot.quoteRates.size)
    }

    @Test
    fun rejectsWrongBaseAndInvalidRates() {
        assertThrows(IllegalArgumentException::class.java) {
            FrankfurterRatesParser.parse(
                """[{"date":"2026-10-06","base":"USD","quote":"KES","rate":130.0}]""",
                "KES"
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            FrankfurterRatesParser.parse(
                """[{"date":"2026-10-06","base":"KES","quote":"USD","rate":0.0}]""",
                "KES"
            )
        }
    }
}
