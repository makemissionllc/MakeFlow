package com.makemission.makeflow.widget

import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.domain.CashFlowProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetContentResolverTest {

    private val now = System.currentTimeMillis()
    private val oneDayMs = 24L * 60 * 60 * 1000

    private fun bill(desc: String, amount: Double, daysAhead: Int) = RecurringPattern(
        normalizedDescription = desc,
        avgAmount = amount,
        intervalDays = 30,
        lastSeenDate = now - oneDayMs,
        nextExpectedDate = now + daysAhead * oneDayMs,
        confidence = 0.9f,
        transactionType = "EXPENSE"
    )

    private fun budget(name: String, spent: Double, limit: Double) = BudgetEntity(
        id = name,
        categoryId = name,
        categoryName = name,
        amount = limit,
        spentAmount = spent,
        period = "Monthly"
    )

    @Test
    fun billDueWithin2Days_takesPriorityOverBudgetWarning() {
        val result = WidgetContentResolver.resolve(
            upcomingBills = listOf(bill("Rent", 500.0, 1)),
            budgets = listOf(budget("Food", 200.0, 100.0)),
            currentBalance = 1000.0,
            projection = null,
            hasAnyAccounts = true,
            hasAnyTransactions = true
        )
        assertTrue("Expected UpcomingBill", result is WidgetContent.UpcomingBill)
        val bill = result as WidgetContent.UpcomingBill
        assertEquals("Rent", bill.description)
        assertEquals(500.0, bill.amount, 0.001)
        assertTrue(bill.daysUntilDue <= 2)
    }

    @Test
    fun budgetWarning_takesPriorityOverBalanceSummary() {
        val result = WidgetContentResolver.resolve(
            upcomingBills = listOf(bill("Rent", 500.0, 10)),
            budgets = listOf(budget("Food", 200.0, 100.0)),
            currentBalance = 1000.0,
            projection = null,
            hasAnyAccounts = true,
            hasAnyTransactions = true
        )
        assertTrue("Expected BudgetWarning", result is WidgetContent.BudgetWarning)
        val warning = result as WidgetContent.BudgetWarning
        assertEquals("Food", warning.categoryName)
        assertEquals(true, warning.isExceeded)
    }

    @Test
    fun balanceSummary_isFallbackWhenNothingUrgent() {
        val result = WidgetContentResolver.resolve(
            upcomingBills = listOf(bill("Rent", 500.0, 10)),
            budgets = listOf(budget("Food", 50.0, 100.0)),
            currentBalance = 1000.0,
            projection = CashFlowProjection(
                currentBalance = 1000.0,
                projectedEndOfMonthBalance = 800.0,
                upcomingRecurringTotal = -200.0,
                projectedDiscretionarySpend = 0.0,
                daysRemainingInMonth = 15
            ),
            hasAnyAccounts = true,
            hasAnyTransactions = true
        )
        assertTrue("Expected BalanceSummary", result is WidgetContent.BalanceSummary)
        val summary = result as WidgetContent.BalanceSummary
        assertEquals(1000.0, summary.currentBalance, 0.001)
        assertEquals(800.0, summary.projectedEndOfMonth!!, 0.001)
    }

    @Test
    fun multipleBills_picksSoonestOne() {
        val result = WidgetContentResolver.resolve(
            upcomingBills = listOf(
                bill("Far Bill", 100.0, 5),
                bill("Near Bill", 200.0, 1),
                bill("Later Bill", 300.0, 2)
            ),
            budgets = emptyList(),
            currentBalance = 1000.0,
            projection = null,
            hasAnyAccounts = true,
            hasAnyTransactions = true
        )
        assertTrue("Expected UpcomingBill", result is WidgetContent.UpcomingBill)
        assertEquals("Near Bill", (result as WidgetContent.UpcomingBill).description)
    }

    @Test
    fun noData_showsNoData() {
        val result = WidgetContentResolver.resolve(
            upcomingBills = emptyList(),
            budgets = emptyList(),
            currentBalance = 0.0,
            projection = null,
            hasAnyAccounts = false,
            hasAnyTransactions = false
        )
        assertTrue("Expected NoData", result is WidgetContent.NoData)
    }
}
