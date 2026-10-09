package com.makemission.makeflow.domain

import java.time.LocalDate
import java.time.ZoneId

enum class HistoryPeriod {
    ALL_TIME,
    TODAY,
    LAST_SEVEN_DAYS,
    THIS_MONTH,
    LAST_MONTH;

    fun timestampRange(today: LocalDate, zone: ZoneId): LongRange? {
        val start = when (this) {
            ALL_TIME -> return null
            TODAY -> today
            LAST_SEVEN_DAYS -> today.minusDays(6)
            THIS_MONTH -> today.withDayOfMonth(1)
            LAST_MONTH -> today.withDayOfMonth(1).minusMonths(1)
        }
        val endExclusive = when (this) {
            THIS_MONTH -> start.plusMonths(1)
            LAST_MONTH -> today.withDayOfMonth(1)
            else -> today.plusDays(1)
        }
        // Calendar boundaries, rather than fixed 24-hour durations, also handle DST.
        return start.atStartOfDay(zone).toInstant().toEpochMilli() until
            endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
