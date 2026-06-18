package com.makemission.makeflow.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makemission.makeflow.ui.navigation.Screen
import com.makemission.makeflow.ui.screens.AnalyticsScreen
import com.makemission.makeflow.ui.screens.CategoriesScreen
import com.makemission.makeflow.ui.screens.DashboardScreen
import com.makemission.makeflow.ui.screens.OnboardingScreen
import com.makemission.makeflow.ui.screens.SettingsScreen
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.haptic.lightTick

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isAddRoute = currentDestination?.route == Screen.AddTransaction.route
    val isOnboardingRoute = currentDestination?.route == Screen.Onboarding.route
    val showNavbar = !isAddRoute && !isOnboardingRoute
    val dimens = LocalAppDimens.current

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Onboarding.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(bottom = innerPadding.calculateBottomPadding() + if (showNavbar) 80.dp else 0.dp)
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

            if (showNavbar) {
                val isCompact = dimens.navbarMaxWidth == Dp.Infinity

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = innerPadding.calculateBottomPadding() + dimens.navbarMarginBottom)
                        .then(
                            if (isCompact) Modifier.padding(horizontal = dimens.navbarMarginHorizontal)
                            else Modifier.widthIn(max = dimens.navbarMaxWidth)
                        ),
                    shape = RoundedCornerShape(dimens.navbarCornerRadius),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(dimens.navbarCornerRadius)
                            )
                            .padding(
                                horizontal = dimens.navbarPaddingHorizontal,
                                vertical = dimens.navbarPaddingVertical
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            screen?.let { s ->
                                val selected =
                                    currentDestination?.hierarchy?.any { it.route == s.route } == true

                                FloatingNavItem(
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
                                    icon = s.icon,
                                    contentDescription = s.title,
                                    iconSize = dimens.navbarItemSize,
                                    cornerRadius = dimens.navbarCornerRadius,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    iconSize: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    val animatedIndicatorScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "navIndicatorScale"
    )

    val animatedIconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "navIconScale"
    )

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.lightTick()
                    onClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = animatedIndicatorScale
                    scaleY = animatedIndicatorScale
                    alpha = animatedIndicatorScale * 0.12f
                }
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(cornerRadius)
                )
        )

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer(
                    scaleX = animatedIconScale,
                    scaleY = animatedIconScale
                ),
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
