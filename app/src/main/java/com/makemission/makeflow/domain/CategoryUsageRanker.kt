package com.makemission.makeflow.domain

data class CategoryUsage(
    val categoryId: String,
    val type: String,
    val timestamp: Long
)

data class RankedCategory(
    val categoryId: String,
    val count: Int
)

object CategoryUsageRanker {

    private const val TIME_WINDOW_DAYS = 90
    private const val MIN_USAGE_THRESHOLD = 3
    private const val MAX_RESULTS = 8

    fun rank(
        categoryUsages: List<CategoryUsage>,
        nowMillis: Long = System.currentTimeMillis(),
        maxResults: Int = MAX_RESULTS,
        minUsageThreshold: Int = MIN_USAGE_THRESHOLD,
        timeWindowDays: Int = TIME_WINDOW_DAYS
    ): Map<String, List<RankedCategory>> {
        if (categoryUsages.isEmpty()) return emptyMap()

        val cutoffMillis = nowMillis - (timeWindowDays.toLong() * 24 * 60 * 60 * 1000)

        val windowed = categoryUsages.filter { it.timestamp >= cutoffMillis }

        val grouped = windowed.groupBy { it.type }

        return grouped.mapValues { (_, usages) ->
            usages.groupingBy { it.categoryId }
                .eachCount()
                .filter { it.value >= minUsageThreshold }
                .map { (categoryId, count) -> RankedCategory(categoryId, count) }
                .sortedByDescending { it.count }
                .take(maxResults)
        }.filter { it.value.isNotEmpty() }
    }
}
