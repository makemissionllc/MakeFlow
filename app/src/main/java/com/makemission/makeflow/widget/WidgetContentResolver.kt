package com.makemission.makeflow.widget

import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.domain.CashFlowProjection
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed class WidgetContent {
    data class UpcomingBill(
        val description: String,
        val amount: Double,
        val daysUntilDue: Int
    ) : WidgetContent()

    data class BudgetWarning(
        val categoryName: String,
        val spentAmount: Double,
        val budgetAmount: Double,
        val isExceeded: Boolean
    ) : WidgetContent()

    data class BalanceSummary(
        val currentBalance: Double,
        val projectedEndOfMonth: Double?
    ) : WidgetContent()

    data object NoData : WidgetContent()
}

object WidgetContentResolver {

    fun resolve(
        upcomingBills: List<RecurringPattern>,
        budgets: List<BudgetEntity>,
        currentBalance: Double,
        projection: CashFlowProjection?,
        hasAnyAccounts: Boolean,
        hasAnyTransactions: Boolean
    ): WidgetContent {
        if (!hasAnyAccounts && currentBalance == 0.0 && !hasAnyTransactions) {
            return WidgetContent.NoData
        }

        val now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()

        val urgentBill = upcomingBills
            .filter { daysUntil(it.nextExpectedDate, now) <= 2 }
            .minByOrNull { daysUntil(it.nextExpectedDate, now) }
        if (urgentBill != null) {
            return WidgetContent.UpcomingBill(
                description = urgentBill.normalizedDescription,
                amount = urgentBill.avgAmount,
                daysUntilDue = daysUntil(urgentBill.nextExpectedDate, now)
            )
        }

        val offTrackBudget = budgets.firstOrNull { it.spentAmount > it.amount }
        if (offTrackBudget != null) {
            return WidgetContent.BudgetWarning(
                categoryName = offTrackBudget.categoryName,
                spentAmount = offTrackBudget.spentAmount,
                budgetAmount = offTrackBudget.amount,
                isExceeded = offTrackBudget.spentAmount > offTrackBudget.amount
            )
        }

        return WidgetContent.BalanceSummary(
            currentBalance = currentBalance,
            projectedEndOfMonth = projection?.projectedEndOfMonthBalance
        )
    }

    private fun daysUntil(epochMillis: Long, today: java.time.LocalDate): Int {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return ChronoUnit.DAYS.between(today, date).toInt().coerceAtLeast(0)
    }
}
