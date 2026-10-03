package com.makemission.makeflow.core.ml

internal fun charWbNgrams(description: String): List<String> {
    val lower = description.lowercase().trim()
    val words = lower.split(Regex("\\s+")).filter { it.isNotBlank() }
    val result = mutableListOf<String>()
    for (word in words) {
        val padded = " $word "
        for (n in 2..4) {
            for (i in 0..padded.length - n) {
                result.add(padded.substring(i, i + n))
            }
        }
    }
    return result
}

internal fun vectorize(
    description: String,
    vocab: Map<String, Int>,
    maxFeatures: Int
): FloatArray {
    val ngrams = charWbNgrams(description).toSet()
    val vec = FloatArray(maxFeatures)
    for (ngram in ngrams) {
        val idx = vocab[ngram]
        if (idx != null && idx < maxFeatures) {
            vec[idx] = 1.0f
        }
    }
    return vec
}
