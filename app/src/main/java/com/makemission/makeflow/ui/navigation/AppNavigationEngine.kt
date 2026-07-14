package com.makemission.makeflow.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.makemission.makeflow.core.device.InterruptionFilterProvider
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.device.rememberPowerState
import com.makemission.makeflow.core.haptics.rememberHapticManager
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.core.layout.rememberDeviceLayoutMode
import com.makemission.makeflow.core.notifications.NotificationChannels
import com.makemission.makeflow.core.notifications.NotificationGate
import com.makemission.makeflow.core.notifications.NotificationSenderImpl
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.MainScreen
import com.makemission.makeflow.ui.screens.OnboardingScreen
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import kotlinx.coroutines.flow.first
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
            accountDao = db.accountDao(),
            recurringTransactionDao = db.recurringTransactionDao()
        )
    }
    val userPrefDataStore = remember { UserPreferencesDataStore(context.userPrefDataStore) }

    val isProfiled by userPrefDataStore.isProfiled.collectAsState(initial = false)
    val username by userPrefDataStore.userName.collectAsState(initial = "")
    val currency by userPrefDataStore.selectedCurrency.collectAsState(initial = "KES")
    val selectedTheme by userPrefDataStore.selectedTheme.collectAsState(initial = "forest")
    val hapticManager = rememberHapticManager()
    val powerState by rememberPowerState()

    LaunchedEffect(Unit) {
        NotificationChannels.createChannels(context)
    }

    val interruptionFilterProvider = remember { InterruptionFilterProvider(context) }
    val notificationSender = remember { NotificationSenderImpl(context.applicationContext) }
    val notificationGate = remember {
        NotificationGate(
            interruptionFilterProvider = interruptionFilterProvider,
            queuedNotificationDao = db.queuedNotificationDao(),
            notificationSender = notificationSender
        )
    }

    LaunchedEffect(Unit) {
        interruptionFilterProvider.observeState().collect { state ->
            if (state == com.makemission.makeflow.core.device.InterruptionState.NORMAL) {
                notificationGate.flushQueueIfClear()
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val patterns = db.recurringPatternDao().getAllOnce()
            val now = System.currentTimeMillis()
            val oneDay = 24L * 60 * 60 * 1000
            for (pattern in patterns) {
                if (pattern.nextExpectedDate in (now - oneDay)..(now + oneDay)) {
                    if (pattern.nextExpectedDate != pattern.lastNotifiedNextDate) {
                        notificationGate.dispatchOrQueue(
                            type = "bill_reminder",
                            title = "Upcoming bill",
                            body = "${pattern.normalizedDescription} is due soon"
                        )
                        db.recurringPatternDao().updateLastNotifiedNextDate(
                            pattern.id, pattern.nextExpectedDate
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(Unit) {
        try {
            repository.processDueRecurringTransactions()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    val windowSizeClass = calculateWindowSizeClass(context as ComponentActivity)
    val deviceLayoutMode = rememberDeviceLayoutMode(windowSizeClass)

    val shortcutType = remember {
        (context as ComponentActivity).intent?.getStringExtra("shortcut_type")
            ?.takeIf { it == "EXPENSE" || it == "INCOME" }
    }

    val appLockEnabled by userPrefDataStore.appLockEnabled.collectAsState(initial = false)
    var isUnlocked by remember { mutableStateOf(!appLockEnabled) }

    LaunchedEffect(appLockEnabled) {
        isUnlocked = !appLockEnabled
    }

    CompositionLocalProvider(
        LocalReducedMotion provides powerState.shouldReduceMotion,
        LocalDeviceLayoutMode provides deviceLayoutMode
    ) {
    MakeFlowTheme(themePalette = selectedTheme) {
        if (!isUnlocked) {
            com.makemission.makeflow.core.lock.LockScreen(
                onUnlocked = { isUnlocked = true }
            )
        } else {
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
                    },
                    notificationGate = notificationGate,
                    initialTransactionType = shortcutType
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
                    },
                    hapticManager = hapticManager
                )
        }
    }
    }
    }
    }
}
