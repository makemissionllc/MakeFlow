package com.makemission.makeflow.domain

import com.makemission.makeflow.core.ml.CategoryClassifier

data class CategorySuggestion(
    val categoryId: String,
    val confidence: Float,
    val matchedOnExactPastDescription: Boolean
)

data class PastTransactionForSuggestion(
    val description: String,
    val categoryId: String
)

object CategorySuggester {
    private const val MIN_DESCRIPTION_LENGTH = 2
    private const val MIN_MATCHES_FOR_SUGGESTION = 2

    fun suggest(
        inputDescription: String,
        pastTransactions: List<PastTransactionForSuggestion>
    ): CategorySuggestion? {
        val normalized = normalize(inputDescription)
        if (normalized.length < MIN_DESCRIPTION_LENGTH) return null

        val exactMatches = pastTransactions.filter { normalize(it.description) == normalized }
        if (exactMatches.isNotEmpty()) {
            val topCategory = exactMatches
                .groupingBy { it.categoryId }
                .eachCount()
                .maxByOrNull { it.value }
            if (topCategory != null) {
                return CategorySuggestion(
                    categoryId = topCategory.key,
                    confidence = 1.0f,
                    matchedOnExactPastDescription = true
                )
            }
        }

        val partialMatches = pastTransactions.filter {
            val pastNorm = normalize(it.description)
            pastNorm.contains(normalized) || normalized.contains(pastNorm)
        }
        if (partialMatches.size < MIN_MATCHES_FOR_SUGGESTION) return null

        val grouped = partialMatches.groupingBy { it.categoryId }.eachCount()
        val total = partialMatches.size
        val topCategory = grouped.maxByOrNull { it.value } ?: return null
        val confidence = (topCategory.value.toFloat() / total).coerceIn(0f, 0.95f)

        return CategorySuggestion(
            categoryId = topCategory.key,
            confidence = confidence,
            matchedOnExactPastDescription = false
        )
    }

    fun suggestWithClassifier(
        inputDescription: String,
        pastTransactions: List<PastTransactionForSuggestion>,
        classifier: CategoryClassifier?,
        categoryNameToId: Map<String, String>,
        confidenceThreshold: Float = 0.6f,
        correctionMemory: Map<String, String> = emptyMap()
    ): CategorySuggestion? {
        val exactOrPartial = suggest(inputDescription, pastTransactions)
        if (exactOrPartial != null && exactOrPartial.matchedOnExactPastDescription) {
            return exactOrPartial
        }

        val normalized = normalize(inputDescription)
        val memoryCategoryId = correctionMemory[normalized]
        if (memoryCategoryId != null) {
            return CategorySuggestion(
                categoryId = memoryCategoryId,
                confidence = 0.95f,
                matchedOnExactPastDescription = false
            )
        }

        if (classifier != null) {
            val prediction = classifier.classify(inputDescription)
            if (prediction != null && prediction.confidence >= confidenceThreshold) {
                val categoryId = categoryNameToId[prediction.categoryName]
                if (categoryId != null) {
                    return CategorySuggestion(
                        categoryId = categoryId,
                        confidence = prediction.confidence,
                        matchedOnExactPastDescription = false
                    )
                }
            }
        }
        return exactOrPartial?.takeIf { it.confidence >= 0.5f }
    }

    fun normalize(description: String): String =
        description.trim().lowercase().replace(Regex("\\s+"), " ")
}
