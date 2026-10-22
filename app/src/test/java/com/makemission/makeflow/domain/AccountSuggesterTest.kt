package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountSuggesterTest {
    private val history = listOf(
        PastTransactionForSuggestion("Coffee", "food", "wallet"),
        PastTransactionForSuggestion(" coffee ", "food", "wallet"),
        PastTransactionForSuggestion("Coffee", "food", "bank"),
        PastTransactionForSuggestion("Taxi", "transport", "bank")
    )

    @Test
    fun exactDescriptionSuggestsMostUsedAvailableAccount() {
        val result = AccountSuggester.suggest("  COFFEE  ", history, setOf("wallet", "bank"))

        assertEquals(AccountSuggestion("wallet", 2), result)
    }

    @Test
    fun tiedAccountsDoNotProduceAnAmbiguousSuggestion() {
        val result = AccountSuggester.suggest("Coffee", history.drop(1).take(2), setOf("wallet", "bank"))

        assertNull(result)
    }

    @Test
    fun removedAccountsAreIgnored() {
        val result = AccountSuggester.suggest("Coffee", history, setOf("bank"))

        assertEquals(AccountSuggestion("bank", 1), result)
    }

    @Test
    fun partialOrShortDescriptionsDoNotSuggestAnAccount() {
        assertNull(AccountSuggester.suggest("cof", history, setOf("wallet", "bank")))
        assertNull(AccountSuggester.suggest("c", history, setOf("wallet", "bank")))
    }

    @Test
    fun descriptionsWithoutAvailableAccountHistoryDoNotSuggest() {
        val uncategorizedHistory = listOf(PastTransactionForSuggestion("Coffee", "food"))

        assertNull(AccountSuggester.suggest("Coffee", uncategorizedHistory, setOf("wallet", "bank")))
        assertNull(AccountSuggester.suggest("Coffee", history, emptySet()))
    }
}
