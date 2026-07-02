package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_patterns")
data class RecurringPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedDescription: String,
    val avgAmount: Double,
    val intervalDays: Int,
    val lastSeenDate: Long,
    val nextExpectedDate: Long,
    val confidence: Float,
    val categoryId: String? = null,
    val transactionType: String
)
