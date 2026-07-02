package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_interactions")
data class CardInteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardType: String,
    val hourOfDay: Int,
    val timestamp: Long
)
