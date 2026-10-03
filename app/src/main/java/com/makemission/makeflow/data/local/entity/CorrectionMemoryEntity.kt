package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "correction_memory")
data class CorrectionMemoryEntity(
    @PrimaryKey val normalizedDescription: String,
    val categoryId: String,
    val updatedAt: Long = System.currentTimeMillis()
)
