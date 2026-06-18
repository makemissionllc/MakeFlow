package com.makemission.makeflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makemission.makeflow.ui.components.ExpressiveNavDock
import com.makemission.makeflow.ui.components.MakeFlowTab
import com.makemission.makeflow.ui.navigation.Screen
import com.makemission.makeflow.ui.screens.AnalyticsScreen
import com.makemission.makeflow.ui.screens.CategoriesScreen
import com.makemission.makeflow.ui.screens.DashboardScreen
import com.makemission.makeflow.ui.screens.OnboardingScreen
import com.makemission.makeflow.ui.screens.SettingsScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isAddRoute = currentDestination?.route == Screen.AddTransaction.route
    val isOnboardingRoute = currentDestination?.route == Screen.Onboarding.route
    val showNavbar = !isAddRoute && !isOnboardingRoute

    val currentTab = when (currentDestination?.route) {
        Screen.Dashboard.route -> MakeFlowTab.FOCUS
        Screen.Analytics.route -> MakeFlowTab.INSIGHTS
        else -> null
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Onboarding.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(onCompleted = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Dashboard.route) { DashboardScreen() }
                composable(Screen.Analytics.route) { AnalyticsScreen() }
                composable(Screen.Categories.route) { CategoriesScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }

            if (showNavbar && currentTab != null) {
                ExpressiveNavDock(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        val route = when (tab) {
                            MakeFlowTab.FOCUS -> Screen.Dashboard.route
                            MakeFlowTab.INSIGHTS -> Screen.Analytics.route
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onFabClick = {
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
