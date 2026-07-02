package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.dao.CardHourCount

enum class DashboardCardType { QUICK_ADD, INSIGHTS_SUMMARY, RECURRING, BUDGET_PROGRESS, ACCOUNTS }

object DashboardCardRanker {
    private const val MIN_INTERACTIONS_FOR_PERSONALIZATION = 15
    private const val TIME_WINDOW_HOURS = 3

    fun rank(
        currentHour: Int,
        interactionCounts: List<CardHourCount>,
        defaultOrder: List<DashboardCardType>
    ): List<DashboardCardType> {
        val totalInteractions = interactionCounts.sumOf { it.count }
        if (totalInteractions < MIN_INTERACTIONS_FOR_PERSONALIZATION) {
            return defaultOrder
        }

        val relevantWindow = (currentHour - TIME_WINDOW_HOURS..currentHour + TIME_WINDOW_HOURS)
            .map { ((it % 24) + 24) % 24 }
            .toSet()

        val scoreByCard = defaultOrder.associateWith { cardType ->
            interactionCounts
                .filter { it.cardType == cardType.name && it.hourOfDay in relevantWindow }
                .sumOf { it.count }
        }

        return defaultOrder.sortedByDescending { scoreByCard[it] ?: 0 }
    }
}
