package com.makemission.makeflow.ui.screens

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

data class FinancialAccount(
    val id: String,
    val name: String,
    val balance: Double,
    val iconColor: androidx.compose.ui.graphics.Color
)
