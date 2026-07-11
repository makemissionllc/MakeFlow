package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amount: Double,
    val type: String,
    val categoryId: String? = null,
    val accountId: String,
    val frequency: String,
    val intervalDays: Int,
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val autoConfirm: Boolean = false,
    val isActive: Boolean = true
)
