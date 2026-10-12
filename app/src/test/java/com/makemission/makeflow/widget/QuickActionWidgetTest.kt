package com.makemission.makeflow.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionWidgetTest {

    @Test
    fun zeroDataState_noAccounts_showsZeroBalance() {
        val data = QuickActionData(
            balance = 0.0,
            currencyCode = "KES",
            hasAccounts = false
        )
        assertFalse(data.hasAccounts)
        assertEquals(0.0, data.balance, 0.001)
        assertEquals("KES", data.currencyCode)
    }

    @Test
    fun withAccounts_showsCorrectBalance() {
        val data = QuickActionData(
            balance = 5432.10,
            currencyCode = "USD",
            hasAccounts = true
        )
        assertTrue(data.hasAccounts)
        assertEquals(5432.10, data.balance, 0.001)
        assertEquals("USD", data.currencyCode)
    }

    @Test
    fun negativeBalance_displayedCorrectly() {
        val data = QuickActionData(
            balance = -200.0,
            currencyCode = "EUR",
            hasAccounts = true
        )
        assertTrue(data.hasAccounts)
        assertEquals(-200.0, data.balance, 0.001)
    }

    @Test
    fun widgetQuickAddAndRepeatPreviewKeepTransactionContext() {
        val data = QuickActionData(
            balance = 100.0,
            currencyCode = "KES",
            hasAccounts = true,
            quickAddAccountName = "Wallet",
            latestRepeat = QuickRepeatTransaction(
                type = "EXPENSE",
                amount = 250.0,
                note = "Lunch",
                categoryName = "Food",
                accountName = "Wallet",
                accountCurrencyCode = "KES"
            )
        )

        assertEquals("Wallet", data.quickAddAccountName)
        assertEquals("Lunch", data.latestRepeat?.note)
        assertEquals("Food", data.latestRepeat?.categoryName)
        assertEquals("Wallet", data.latestRepeat?.accountName)
        assertEquals(250.0, data.latestRepeat?.amount ?: 0.0, 0.001)
        assertEquals("KES", data.latestRepeat?.accountCurrencyCode)
    }
}
