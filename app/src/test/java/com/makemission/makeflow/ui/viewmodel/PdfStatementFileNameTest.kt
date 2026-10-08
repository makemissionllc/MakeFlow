package com.makemission.makeflow.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PdfStatementFileNameTest {

    @Test
    fun `monthly export uses a readable inclusive date range`() {
        val start = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 1)
        }.timeInMillis
        val endExclusive = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 1)
        }.timeInMillis

        assertEquals(
            "MakeFlow_Statement_2026-10-01_to_2026-10-31.pdf",
            PdfStatementFileName.forRange(start, endExclusive)
        )
    }

    @Test
    fun `all time export includes the generation date`() {
        val generatedAt = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 8)
        }.timeInMillis

        assertEquals(
            "MakeFlow_Statement_All_Time_2026-10-08.pdf",
            PdfStatementFileName.forRange(0L, Long.MAX_VALUE, generatedAt)
        )
    }
}
