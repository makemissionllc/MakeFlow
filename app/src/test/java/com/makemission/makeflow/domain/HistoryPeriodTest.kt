package com.makemission.makeflow.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPeriodTest {
    private val nairobi = ZoneId.of("Africa/Nairobi")

    @Test
    fun allTimeDoesNotRestrictTimestamps() {
        assertNull(HistoryPeriod.ALL_TIME.timestampRange(LocalDate.of(2026, 10, 9), nairobi))
    }

    @Test
    fun todayIncludesLocalMidnightAndLastMillisecondButNotNextDay() {
        val today = LocalDate.of(2026, 10, 9)
        val range = HistoryPeriod.TODAY.timestampRange(today, nairobi)!!
        val start = millis(today)
        val nextDay = millis(today.plusDays(1))
        assertTrue(start in range)
        assertTrue(nextDay - 1 in range)
        assertFalse(start - 1 in range)
        assertFalse(nextDay in range)
        assertEquals(start, range.first)
        assertEquals(nextDay - 1, range.last)
    }

    @Test
    fun lastSevenDaysIncludesTodayAndSixPreviousCalendarDays() {
        val today = LocalDate.of(2026, 1, 3)
        val range = HistoryPeriod.LAST_SEVEN_DAYS.timestampRange(today, nairobi)!!
        assertEquals(millis(LocalDate.of(2025, 12, 28)), range.first)
        assertEquals(millis(LocalDate.of(2026, 1, 4)) - 1, range.last)
    }

    @Test
    fun thisMonthIncludesEntireCalendarMonth() {
        val range = HistoryPeriod.THIS_MONTH.timestampRange(LocalDate.of(2026, 10, 9), nairobi)!!
        assertEquals(millis(LocalDate.of(2026, 10, 1)), range.first)
        assertEquals(millis(LocalDate.of(2026, 11, 1)) - 1, range.last)
    }

    @Test
    fun lastMonthHandlesNewYear() {
        val range = HistoryPeriod.LAST_MONTH.timestampRange(LocalDate.of(2026, 1, 9), nairobi)!!
        assertEquals(millis(LocalDate.of(2025, 12, 1)), range.first)
        assertEquals(millis(LocalDate.of(2026, 1, 1)) - 1, range.last)
    }

    @Test
    fun lastMonthIncludesLeapDay() {
        val range = HistoryPeriod.LAST_MONTH.timestampRange(LocalDate.of(2024, 3, 9), nairobi)!!
        assertEquals(millis(LocalDate.of(2024, 2, 1)), range.first)
        assertEquals(millis(LocalDate.of(2024, 3, 1)) - 1, range.last)
        assertTrue(millis(LocalDate.of(2024, 2, 29)) in range)
    }

    @Test
    fun todayUsesShortDayDuringSpringDaylightSavingChange() {
        val range = HistoryPeriod.TODAY.timestampRange(LocalDate.of(2026, 3, 8), ZoneId.of("America/New_York"))!!
        assertEquals(23 * 60 * 60 * 1000L, range.last - range.first + 1)
    }

    @Test
    fun todayUsesLongDayDuringAutumnDaylightSavingChange() {
        val range = HistoryPeriod.TODAY.timestampRange(LocalDate.of(2026, 11, 1), ZoneId.of("America/New_York"))!!
        assertEquals(25 * 60 * 60 * 1000L, range.last - range.first + 1)
    }

    private fun millis(date: LocalDate) = date.atStartOfDay(nairobi).toInstant().toEpochMilli()
}
