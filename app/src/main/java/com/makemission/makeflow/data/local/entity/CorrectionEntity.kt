package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "corrections")
data class CorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val correctedLabelName: String,
    val correctedLabelIndex: Int,
    val originalSuggestedLabelName: String?,
    val isTrained: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
