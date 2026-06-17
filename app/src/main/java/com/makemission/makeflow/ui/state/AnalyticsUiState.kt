package com.makemission.makeflow.ui.state

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val incomeChartPoints: List<Pair<String, Double>> = emptyList(),
    val expenseChartPoints: List<Pair<String, Double>> = emptyList(),
    val selectedFilter: Int = 0,
    val errorMessage: String? = null
)
