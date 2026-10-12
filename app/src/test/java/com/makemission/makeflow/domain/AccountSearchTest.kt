package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AccountSearchTest {
    private val accounts = listOf(
        AccountEntity("cash", "Daily Wallet", 100.0, 0.0, currencyCode = "USD"),
        AccountEntity("bank", "Travel Bank", 1500.0, 0.0, currencyCode = "KES"),
        AccountEntity("legacy", "Savings", 30.0, 0.0)
    )

    @Test fun blankQueryKeepsAllAccountsInOriginalOrder() {
        assertEquals(accounts, AccountSearch.filter(accounts, " \t ", "EUR"))
    }

    @Test fun matchesNameAndCurrencyTermsIgnoringCaseAndWhitespace() {
        assertEquals(listOf(accounts[0]), AccountSearch.filter(accounts, "  WALLET \t usd ", "EUR"))
        assertEquals(listOf(accounts[1]), AccountSearch.filter(accounts, "kes", "EUR"))
        assertEquals(emptyList<AccountEntity>(), AccountSearch.filter(accounts, "wallet kes", "EUR"))
    }

    @Test fun legacyCurrencyUsesDefaultButExplicitCurrencyDoesNot() {
        assertEquals(listOf(accounts[2]), AccountSearch.filter(accounts, "EUR", "EUR"))
        assertEquals(listOf(accounts[1], accounts[2]), AccountSearch.filter(accounts, "KES", "KES"))
    }

    @Test fun matchingIsIndependentOfDeviceLocaleAndDoesNotModifyRecords() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(listOf(accounts[0]), AccountSearch.filter(accounts, "DAILY", "EUR"))
            assertEquals(100.0, accounts[0].balance, 0.0)
        } finally { Locale.setDefault(original) }
    }
}
