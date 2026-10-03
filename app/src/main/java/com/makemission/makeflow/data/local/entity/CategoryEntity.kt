package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val parentCategoryId: String? = null, // Nullable self-reference link for nested subcategories
    val iconName: String = "Check",
    val colorHex: String = "#9E9E9E",
    val monthlyBudget: Double = 0.0
)
