package com.makemission.makeflow.ui.state

import com.makemission.makeflow.data.local.entity.CategoryEntity

data class CategoryUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val spendingMap: Map<String, Double> = emptyMap(),
    val errorMessage: String? = null
)
