package com.makemission.makeflow.domain

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

    private fun normalize(description: String): String =
        description.trim().lowercase().replace(Regex("\\s+"), " ")
}
