package com.makemission.makeflow.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.MainScreen
import com.makemission.makeflow.ui.screens.OnboardingScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigationEngine() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        FinanceRepositoryImpl(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            taskDao = db.taskDao(),
            accountDao = db.accountDao()
        )
    }
    val userPrefDataStore = remember { UserPreferencesDataStore(context.userPrefDataStore) }

    val isProfiled by userPrefDataStore.isProfiled.collectAsState(initial = false)
    val username by userPrefDataStore.userName.collectAsState(initial = "")
    val currency by userPrefDataStore.selectedCurrency.collectAsState(initial = "KES")

    Crossfade(
        targetState = isProfiled,
        animationSpec = tween(durationMillis = 400),
        label = "AppNavigationEngineCrossfade"
    ) { profiled ->
        if (profiled) {
            MainScreen(
                username = username,
                onLogout = {
                    scope.launch {
                        userPrefDataStore.saveName("")
                        userPrefDataStore.clearProfile()
                    }
                }
            )
        } else {
            OnboardingScreen(
                selectedCurrency = currency,
                onCurrencyChange = { scope.launch { userPrefDataStore.saveCurrency(it) } },
                onSaveName = { name -> userPrefDataStore.saveName(name) },
                onSaveAccounts = { accounts ->
                    for (account in accounts) {
                        val entity = account.copy(colorHex = account.colorHex.ifEmpty { "#4CAF50" })
                        repository.insertAccount(entity)
                    }
                },
                onOnboardingComplete = {
                    scope.launch { userPrefDataStore.setProfiled() }
                }
            )
        }
    }
}
