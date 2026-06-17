package com.makemission.makeflow.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makemission.makeflow.ui.navigation.Screen
import com.makemission.makeflow.ui.screens.AddTransactionSheet
import com.makemission.makeflow.ui.screens.AnalyticsScreen
import com.makemission.makeflow.ui.screens.CategoriesScreen
import com.makemission.makeflow.ui.screens.DashboardScreen
import com.makemission.makeflow.ui.screens.SettingsScreen

@Composable
fun MainScreen() {
    Log.d("MakeFlowDebug", "MainScreen initializing")
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isAddRoute = currentDestination?.route == Screen.AddTransaction.route

    Log.d("MakeFlowDebug", "NavHost initializing, startDestination=${Screen.Dashboard.route}")

    Scaffold(
        bottomBar = {
            if (isAddRoute) return@Scaffold
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    screen?.let { s ->
                        val selected = currentDestination?.hierarchy?.any { it.route == s.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(s.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = s.icon,
                                    contentDescription = s.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(text = s.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.AddTransaction.route) {
                AddTransactionSheet(navController = navController)
            }
            composable(Screen.Categories.route) { CategoriesScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
