package com.makemission.makeflow.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.makemission.makeflow.ui.screens.OnboardingScreen

@Composable
fun AppNavigationEngine() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("make_flow_prefs", Context.MODE_PRIVATE) }

    var isOnboardingComplete by remember {
        mutableStateOf(sharedPrefs.getBoolean("onboarding_complete", false))
    }
    var savedName by remember { mutableStateOf(sharedPrefs.getString("user_name", "Jeremy") ?: "Jeremy") }
    var savedCurrency by remember { mutableStateOf(sharedPrefs.getString("currency", "KSh") ?: "KSh") }
    var savedDeposit by remember { mutableStateOf(sharedPrefs.getFloat("initial_deposit", 12500.00f).toDouble()) }

    if (!isOnboardingComplete) {
        OnboardingScreen(
            onOnboardingComplete = { name, currency, deposit ->
                sharedPrefs.edit().apply {
                    putBoolean("onboarding_complete", true)
                    putString("user_name", name)
                    putString("currency", currency)
                    putFloat("initial_deposit", deposit.toFloat())
                    apply()
                }
                savedName = name
                savedCurrency = currency
                savedDeposit = deposit
                isOnboardingComplete = true
            },
            onCompleted = {
                sharedPrefs.edit().apply {
                    putBoolean("onboarding_complete", true)
                    apply()
                }
                isOnboardingComplete = true
            }
        )
    } else {
        MainScreen(
            onboardingName = savedName,
            currency = savedCurrency,
            initialDeposit = savedDeposit,
            onLogout = {
                sharedPrefs.edit().clear().apply()
                isOnboardingComplete = false
            }
        )
    }
}
