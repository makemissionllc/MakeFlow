package com.makemission.makeflow.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Date
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Locale

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

data class FinancialAccount(
    val id: String,
    val name: String,
    val balance: Double,
    val iconColor: Color
)

fun getIconByName(iconName: String?): ImageVector {
    return when (iconName) {
        "Home" -> Icons.Filled.Home
        "ShoppingCart" -> Icons.Filled.ShoppingCart
        "LocationOn" -> Icons.Filled.LocationOn
        "Star" -> Icons.Filled.Star
        "Check" -> Icons.Filled.Check
        else -> Icons.Filled.Check
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    val isToday = todayFormat.format(date) == todayFormat.format(now)
    val prefix = if (isToday) "Today" else "Yesterday"
    return "$prefix, ${timeFormat.format(date)} • ${dateFormat.format(date)}"
}

fun formatAmount(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    fmt.minimumFractionDigits = 2
    fmt.maximumFractionDigits = 2
    return fmt.format(amount)
}
