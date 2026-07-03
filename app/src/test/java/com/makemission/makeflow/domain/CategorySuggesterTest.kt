package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySuggesterTest {

    private val history = listOf(
        PastTransactionForSuggestion("boda boda", "transport"),
        PastTransactionForSuggestion("BODA BODA", "transport"),
        PastTransactionForSuggestion("boda boda", "transport"),
        PastTransactionForSuggestion("uber", "transport"),
        PastTransactionForSuggestion("matatu", "transport"),
        PastTransactionForSuggestion("lunch", "food"),
        PastTransactionForSuggestion("groceries", "food"),
        PastTransactionForSuggestion("dinner out", "food"),
    )

    @Test
    fun exactMatch_returnsConfidenceOne() {
        val result = CategorySuggester.suggest("boda boda", history)
        assertNotNull(result)
        assertEquals("transport", result!!.categoryId)
        assertEquals(1.0f, result.confidence, 0.001f)
        assertEquals(true, result.matchedOnExactPastDescription)
    }

    @Test
    fun exactMatch_normalizesCase() {
        val result = CategorySuggester.suggest("  BODA BODA  ", history)
        assertNotNull(result)
        assertEquals("transport", result!!.categoryId)
        assertEquals(1.0f, result.confidence, 0.001f)
    }

    @Test
    fun partialMatch_consistentCategory_returnsHighConfidence() {
        val localHistory = listOf(
            PastTransactionForSuggestion("coffee at cafe", "food"),
            PastTransactionForSuggestion("cafe lunch", "food"),
            PastTransactionForSuggestion("cafe pastry", "food"),
        )
        val result = CategorySuggester.suggest("cafe", localHistory)
        assertNotNull(result)
        assertEquals("food", result!!.categoryId)
        assertEquals(0.95f, result.confidence, 0.001f)
        assertEquals(false, result.matchedOnExactPastDescription)
    }

    @Test
    fun partialMatch_splitCategories_picksMajority() {
        val splitHistory = listOf(
            PastTransactionForSuggestion("quick bite", "food"),
            PastTransactionForSuggestion("bite snack", "food"),
            PastTransactionForSuggestion("bite repair", "transport"),
        )
        val result = CategorySuggester.suggest("bite", splitHistory)
        assertNotNull(result)
        assertEquals("food", result!!.categoryId)
        assertEquals((2f / 3f).coerceIn(0f, 0.95f), result.confidence, 0.001f)
    }

    @Test
    fun tooShortInput_returnsNull() {
        val result = CategorySuggester.suggest("a", history)
        assertNull(result)
    }

    @Test
    fun insufficientPartialMatches_returnsNull() {
        val thinHistory = listOf(
            PastTransactionForSuggestion("rare word xyz", "misc"),
        )
        val result = CategorySuggester.suggest("rare", thinHistory)
        assertNull(result)
    }

    @Test
    fun emptyHistory_returnsNull() {
        val result = CategorySuggester.suggest("boda boda", emptyList())
        assertNull(result)
    }

    @Test
    fun emptyInput_returnsNull() {
        val result = CategorySuggester.suggest("", history)
        assertNull(result)
    }
}
