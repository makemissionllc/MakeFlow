package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.dao.CardHourCount
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardCardRankerTest {

    private val defaultOrder = listOf(
        DashboardCardType.INSIGHTS_SUMMARY,
        DashboardCardType.ACCOUNTS,
        DashboardCardType.BUDGET_PROGRESS,
        DashboardCardType.RECURRING
    )

    @Test
    fun `insufficient data returns default order`() {
        val counts = listOf(
            CardHourCount("INSIGHTS_SUMMARY", 10, 3),
            CardHourCount("ACCOUNTS", 10, 2)
        )
        val result = DashboardCardRanker.rank(10, counts, defaultOrder)
        assertEquals(defaultOrder, result)
    }

    @Test
    fun `clear time-of-day preference correctly reorders`() {
        val counts = listOf(
            CardHourCount("BUDGET_PROGRESS", 10, 20),
            CardHourCount("ACCOUNTS", 10, 5),
            CardHourCount("INSIGHTS_SUMMARY", 10, 3),
            CardHourCount("RECURRING", 10, 1)
        )
        val result = DashboardCardRanker.rank(10, counts, defaultOrder)
        assertEquals(
            listOf(
                DashboardCardType.BUDGET_PROGRESS,
                DashboardCardType.ACCOUNTS,
                DashboardCardType.INSIGHTS_SUMMARY,
                DashboardCardType.RECURRING
            ),
            result
        )
    }

    @Test
    fun `tied scores preserve default relative order`() {
        val counts = listOf(
            CardHourCount("INSIGHTS_SUMMARY", 10, 5),
            CardHourCount("ACCOUNTS", 10, 5),
            CardHourCount("BUDGET_PROGRESS", 10, 20),
            CardHourCount("RECURRING", 10, 20)
        )
        val result = DashboardCardRanker.rank(10, counts, defaultOrder)
        assertEquals(
            listOf(
                DashboardCardType.BUDGET_PROGRESS,
                DashboardCardType.RECURRING,
                DashboardCardType.INSIGHTS_SUMMARY,
                DashboardCardType.ACCOUNTS
            ),
            result
        )
    }
}
