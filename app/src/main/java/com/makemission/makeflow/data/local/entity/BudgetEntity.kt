package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryName: String,
    val name: String? = null,
    val amount: Double,
    val spentAmount: Double = 0.0,
    val period: String, // "Daily", "Weekly", "Monthly"
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String get() = name ?: categoryName
}
