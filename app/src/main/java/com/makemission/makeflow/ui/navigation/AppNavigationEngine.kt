package com.makemission.makeflow.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.MainScreen
import com.makemission.makeflow.ui.screens.OnboardingScreen

@Composable
fun AppNavigationEngine(
    initialUserExists: Boolean,
    onPersistProfileAndAccounts: (String, List<AccountEntity>) -> Unit,
    currentUsername: String
) {
    var userIsProfiled by remember { mutableStateOf(initialUserExists) }
    var activeUsername by remember { mutableStateOf(currentUsername) }

    Crossfade(
        targetState = userIsProfiled,
        animationSpec = tween(durationMillis = 400),
        label = "AppNavigationEngineCrossfade"
    ) { profiled ->
        if (profiled) {
            MainScreen(
                username = activeUsername,
                onLogout = {
                    userIsProfiled = false
                    activeUsername = ""
                }
            )
        } else {
            OnboardingScreen(
                onOnboardingComplete = { username, initialAccounts ->
                    onPersistProfileAndAccounts(username, initialAccounts)
                    activeUsername = username
                    userIsProfiled = true
                }
            )
        }
    }
}
