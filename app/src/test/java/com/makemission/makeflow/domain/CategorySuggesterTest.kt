package com.makemission.makeflow.domain

import com.makemission.makeflow.core.ml.CategoryClassifier
import com.makemission.makeflow.core.ml.CategoryPrediction
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

    @Test
    fun threeTier_exactMatch_winsOverMl() {
        val classifier = FakeClassifier(CategoryPrediction("food", 0.95f))
        val idMap = mapOf("food" to "food_id", "transport" to "transport_id")
        val result = CategorySuggester.suggestWithClassifier("boda boda", history, classifier, idMap)
        assertNotNull(result)
        assertEquals("transport", result!!.categoryId)
        assertEquals(1.0f, result.confidence, 0.001f)
        assertEquals(true, result.matchedOnExactPastDescription)
    }

    @Test
    fun threeTier_mlHighConfidence_winsOverSubstring() {
        val classifier = FakeClassifier(CategoryPrediction("food", 0.85f))
        val idMap = mapOf("food" to "food_id")
        val substringHistory = listOf(
            PastTransactionForSuggestion("cafe latte", "cafe"),
            PastTransactionForSuggestion("cafe mocha", "cafe"),
        )
        val result = CategorySuggester.suggestWithClassifier("cafe", substringHistory, classifier, idMap)
        assertNotNull(result)
        assertEquals("food_id", result!!.categoryId)
        assertEquals(0.85f, result.confidence, 0.001f)
    }

    @Test
    fun threeTier_mlBelowThreshold_fallsBackToSubstring() {
        val classifier = FakeClassifier(CategoryPrediction("food", 0.4f))
        val idMap = mapOf("food" to "food_id")
        val substringHistory = listOf(
            PastTransactionForSuggestion("cafe latte", "cafe"),
            PastTransactionForSuggestion("cafe mocha", "cafe"),
        )
        val result = CategorySuggester.suggestWithClassifier("cafe", substringHistory, classifier, idMap)
        assertNotNull(result)
        assertEquals("cafe", result!!.categoryId)
    }

    @Test
    fun threeTier_noClassifier_usesSubstring() {
        val substringHistory = listOf(
            PastTransactionForSuggestion("cafe latte", "cafe"),
            PastTransactionForSuggestion("cafe mocha", "cafe"),
        )
        val result = CategorySuggester.suggestWithClassifier("cafe", substringHistory, null, emptyMap())
        assertNotNull(result)
        assertEquals("cafe", result!!.categoryId)
    }

    @Test
    fun threeTier_mlCategoryNotFoundInMap_fallsThrough() {
        val classifier = FakeClassifier(CategoryPrediction("unknown_category", 0.9f))
        val idMap = mapOf("food" to "food_id")
        val substringHistory = listOf(
            PastTransactionForSuggestion("cafe latte", "cafe"),
            PastTransactionForSuggestion("cafe mocha", "cafe"),
        )
        val result = CategorySuggester.suggestWithClassifier("cafe", substringHistory, classifier, idMap)
        assertNotNull(result)
        assertEquals("cafe", result!!.categoryId)
    }

    // ==== Correction memory (tier 2) tests ====

    @Test
    fun correctionMemory_outranksMl() {
        val classifier = FakeClassifier(CategoryPrediction("food", 0.95f))
        val idMap = mapOf("food" to "food_id")
        val memory = mapOf("safari" to "transport_id")
        val result = CategorySuggester.suggestWithClassifier(
            "Safari", emptyList(), classifier, idMap,
            correctionMemory = memory
        )
        assertNotNull(result)
        assertEquals("transport_id", result!!.categoryId)
        assertEquals(0.95f, result.confidence, 0.001f)
    }

    @Test
    fun exactMatch_stillWinsOverCorrectionMemory() {
        val classifier = FakeClassifier(CategoryPrediction("food", 0.95f))
        val idMap = mapOf("food" to "food_id")
        val memory = mapOf("boda boda" to "fuel_id")
        val result = CategorySuggester.suggestWithClassifier(
            "boda boda", history, classifier, idMap,
            correctionMemory = memory
        )
        assertNotNull(result)
        assertEquals("transport", result!!.categoryId)
        assertEquals(1.0f, result.confidence, 0.001f)
        assertEquals(true, result.matchedOnExactPastDescription)
    }

    @Test
    fun correctionMemory_usedWhenNoExactMatchAndNoClassifier() {
        val memory = mapOf("safari" to "transport_id")
        val result = CategorySuggester.suggestWithClassifier(
            "Safari", emptyList(), null, emptyMap(),
            correctionMemory = memory
        )
        assertNotNull(result)
        assertEquals("transport_id", result!!.categoryId)
        assertEquals(0.95f, result.confidence, 0.001f)
    }

    @Test
    fun correctionMemory_notInMemory_doesNotFire() {
        val result = CategorySuggester.suggestWithClassifier(
            "boda boda", history, null, emptyMap(),
            correctionMemory = mapOf("uber" to "fuel_id")
        )
        assertNotNull(result)
        assertEquals("transport", result!!.categoryId)
    }

    @Test
    fun correctionMemory_emptyMap_ignored() {
        val classifier = FakeClassifier(CategoryPrediction("food", 0.85f))
        val idMap = mapOf("food" to "food_id")
        val substringHistory = listOf(
            PastTransactionForSuggestion("cafe latte", "cafe"),
            PastTransactionForSuggestion("cafe mocha", "cafe"),
        )
        val result = CategorySuggester.suggestWithClassifier(
            "cafe", substringHistory, classifier, idMap,
            correctionMemory = emptyMap()
        )
        assertNotNull(result)
        assertEquals("food_id", result!!.categoryId)
    }

    @Test
    fun normalize_removesExtraWhitespaceAndLowercases() {
        val result = CategorySuggester.normalize("  Hello   World  ")
        assertEquals("hello world", result)
    }

    @Test
    fun normalize_emptyString() {
        val result = CategorySuggester.normalize("")
        assertEquals("", result)
    }
}

private class FakeClassifier(private val prediction: CategoryPrediction) : CategoryClassifier {
    override fun classify(description: String): CategoryPrediction = prediction
}
