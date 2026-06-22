package com.makemission.makeflow.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.MainScreen
import com.makemission.makeflow.ui.screens.OnboardingScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigationEngine(
    initialUserExists: Boolean,
    onPersistProfileAndAccounts: (String, List<AccountEntity>) -> Unit,
    currentUsername: String
) {
    var userIsProfiled by remember { mutableStateOf(initialUserExists) }
    var activeUsername by remember { mutableStateOf(currentUsername) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefDataStore = remember { UserPreferencesDataStore(context.userPrefDataStore) }
    val currency by userPrefDataStore.selectedCurrency.collectAsState(initial = "KES")

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
                selectedCurrency = currency,
                onCurrencyChange = { scope.launch { userPrefDataStore.saveCurrency(it) } },
                onOnboardingComplete = { username, initialAccounts ->
                    onPersistProfileAndAccounts(username, initialAccounts)
                    activeUsername = username
                    userIsProfiled = true
                }
            )
        }
    }
}
