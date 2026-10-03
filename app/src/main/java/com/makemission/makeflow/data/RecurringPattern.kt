package com.makemission.makeflow.data

data class RecurringPattern(
    val normalizedDescription: String,
    val avgAmount: Double,
    val intervalDays: Int,
    val lastSeenDate: Long,
    val nextExpectedDate: Long,
    val confidence: Float,
    val categoryId: String? = null,
    val transactionType: String
)
