package com.makemission.makeflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    data object Analytics : Screen("analytics", "Analytics", Icons.Filled.BarChart)
    data object AddTransaction : Screen("add_transaction", "Add", Icons.Filled.Add)
    data object Categories : Screen("categories", "Categories", Icons.Filled.Category)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomNavItems = listOf(Dashboard, Analytics, AddTransaction, Categories, Settings)
    }
}
