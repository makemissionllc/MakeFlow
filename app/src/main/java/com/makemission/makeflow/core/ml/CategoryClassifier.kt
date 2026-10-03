package com.makemission.makeflow.core.ml

data class CategoryPrediction(
    val categoryName: String,
    val confidence: Float
)

interface CategoryClassifier {
    fun classify(description: String): CategoryPrediction?
}
