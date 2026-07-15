package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val targetDate: Long? = null,
    val linkedAccountId: String? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE"
)
