package com.makemission.makeflow.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.ui.components.AdaptiveConfig
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.rememberAdaptiveConfig
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.rememberHapticManager
import com.makemission.makeflow.core.ml.CategoryClassifier
import com.makemission.makeflow.core.ml.RetrainableCategoryClassifier
import com.makemission.makeflow.data.repository.CorrectionRepository
import com.makemission.makeflow.data.repository.CorrectionRepositoryImpl
import com.makemission.makeflow.domain.CorrectionRoute
import com.makemission.makeflow.domain.CorrectionRouter
import com.makemission.makeflow.domain.CategorySuggester
import com.makemission.makeflow.core.notifications.NotificationGate
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.domain.AnomalyDetector
import com.makemission.makeflow.domain.PastTransactionForSuggestion
import com.makemission.makeflow.widget.WidgetUpdateHelper
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.data.backup.BackupManager
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.repository.ExchangeRateRepositoryImpl
import com.makemission.makeflow.data.repository.TransactionTemplateRepositoryImpl
import com.makemission.makeflow.domain.MpesaMessageParser
import com.makemission.makeflow.ui.screens.AccountsScreen
import com.makemission.makeflow.ui.screens.AddEditAccountBottomSheet
import com.makemission.makeflow.ui.screens.AddEditRecurringTransactionBottomSheet
import com.makemission.makeflow.ui.screens.AddTransactionScreen
import com.makemission.makeflow.ui.screens.BudgetScreen
import com.makemission.makeflow.ui.screens.CategoryManagementScreen
import com.makemission.makeflow.ui.screens.ExchangeRatesSheet
import com.makemission.makeflow.ui.screens.HistoryScreen
import com.makemission.makeflow.ui.screens.HomeDashboard
import com.makemission.makeflow.ui.screens.InsightsScreen
import com.makemission.makeflow.ui.screens.ManageTemplatesSheet
import com.makemission.makeflow.ui.screens.RecurringTransactionsScreen
import com.makemission.makeflow.ui.screens.SavingsGoalsScreen
import com.makemission.makeflow.ui.screens.SettingsScreen
import com.makemission.makeflow.ui.viewmodel.AccountsViewModel
import com.makemission.makeflow.ui.viewmodel.AccountsViewModelFactory
import com.makemission.makeflow.ui.viewmodel.BudgetViewModel
import com.makemission.makeflow.ui.viewmodel.BudgetViewModelFactory
import com.makemission.makeflow.ui.viewmodel.HistoryViewModel
import com.makemission.makeflow.ui.viewmodel.HistoryViewModelFactory
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.viewmodel.HomeDashboardViewModel
import com.makemission.makeflow.ui.viewmodel.HomeDashboardViewModelFactory
import com.makemission.makeflow.ui.viewmodel.InsightsViewModel
import com.makemission.makeflow.ui.viewmodel.InsightsViewModelFactory
import com.makemission.makeflow.ui.viewmodel.RecurringTransactionsViewModel
import com.makemission.makeflow.ui.viewmodel.RecurringTransactionsViewModelFactory
import com.makemission.makeflow.ui.viewmodel.SavingsGoalsViewModel
import com.makemission.makeflow.ui.viewmodel.SavingsGoalsViewModelFactory
import com.makemission.makeflow.ui.viewmodel.SettingsViewModel
import com.makemission.makeflow.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MakeFlowTab {
    HOME, INSIGHTS, HISTORY
}

@Composable
fun MainScreen(
    username: String,
    onLogout: () -> Unit,
    notificationGate: NotificationGate? = null,
    initialTransactionType: String? = null,
    sharedMpesaData: MpesaMessageParser.MpesaParsedMessage? = null,
    shareParseError: String? = null,
    onShareParseErrorShown: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(MakeFlowTab.HOME) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        FinanceRepositoryImpl(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            taskDao = db.taskDao(),
            accountDao = db.accountDao(),
            budgetDao = db.budgetDao(),
            recurringPatternDao = db.recurringPatternDao(),
            recurringTransactionDao = db.recurringTransactionDao()
        )
    }
    val exchangeRateRepository = remember { ExchangeRateRepositoryImpl(db.exchangeRateDao()) }
    val backupManager = remember {
        BackupManager(
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            taskDao = db.taskDao(),
            recurringPatternDao = db.recurringPatternDao(),
            recurringTransactionDao = db.recurringTransactionDao(),
            transactionTemplateDao = db.transactionTemplateDao(),
            exchangeRateDao = db.exchangeRateDao(),
            database = db
        )
    }
    val templateRepository = remember { TransactionTemplateRepositoryImpl(db.transactionTemplateDao()) }
    val templates by templateRepository.getAllTemplates().collectAsState(initial = emptyList())
    val goalRepository = remember { com.makemission.makeflow.data.repository.GoalRepositoryImpl(db.goalDao()) }
    val correctionRepository = remember { CorrectionRepositoryImpl(db.correctionDao(), db.correctionMemoryDao()) as CorrectionRepository }
    val userPrefDataStore = remember { UserPreferencesDataStore(context.userPrefDataStore) }
    val homeViewModel: HomeDashboardViewModel = viewModel(
        factory = HomeDashboardViewModelFactory(repository, userPrefDataStore, db.cardInteractionDao(), exchangeRateRepository)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(repository, userPrefDataStore)
    )
    val insightsViewModel: InsightsViewModel = viewModel(
        factory = InsightsViewModelFactory(repository, userPrefDataStore)
    )
    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModelFactory(repository, userPrefDataStore)
    )
    val accountsViewModel: AccountsViewModel = viewModel(
        factory = AccountsViewModelFactory(repository, userPrefDataStore, exchangeRateRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(repository, userPrefDataStore, correctionRepository, backupManager, exchangeRateRepository)
    )
    val recurringTransactionsViewModel: RecurringTransactionsViewModel = viewModel(
        factory = RecurringTransactionsViewModelFactory(repository, userPrefDataStore)
    )
    val savingsGoalsViewModel: SavingsGoalsViewModel = viewModel(
        factory = SavingsGoalsViewModelFactory(goalRepository, repository, userPrefDataStore)
    )
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()
    val smartCategorizationEnabled by settingsViewModel.smartCategorizationEnabled.collectAsState()

    val hapticManager = rememberHapticManager()
    hapticManager.isEnabled = hapticsEnabled

    var categoryClassifier by remember { mutableStateOf<CategoryClassifier?>(null) }
    var labelSet by remember { mutableStateOf<List<String>>(emptyList()) }
    var correctionMemory by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val classifier = withContext(Dispatchers.IO) {
            val c = RetrainableCategoryClassifier.create(context)
            val labels = try { RetrainableCategoryClassifier.loadLabelSet(context) } catch (_: Exception) { emptyList<String>() }
            c?.restoreCheckpoint()
            c to labels
        }
        categoryClassifier = classifier?.first
        labelSet = classifier?.second ?: emptyList()
        correctionMemory = withContext(Dispatchers.IO) { correctionRepository.getAllCorrectionMemory() }
        withContext(Dispatchers.IO) {
            val clf = categoryClassifier as? RetrainableCategoryClassifier ?: return@withContext
            var trained = true
            while (trained) {
                trained = correctionRepository.trainBatchIfReady(clf)
            }
        }
    }

    val accounts by homeViewModel.accounts.collectAsState()
    val categories by homeViewModel.categories.collectAsState()
    val currencySymbol by homeViewModel.currencySymbol.collectAsState()
    val currencyCode by homeViewModel.selectedCurrency.collectAsState()

    var showAddTransaction by remember { mutableStateOf(false) }
    val shortcutPendingType = remember { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(initialTransactionType) {
        if (initialTransactionType != null) {
            shortcutPendingType.value = initialTransactionType
            showAddTransaction = true
        }
    }

    // M-PESA share intent handling
    var mpesaInitialAmount by remember { mutableStateOf("") }
    var mpesaInitialType by remember { mutableStateOf<String?>(null) }
    var mpesaInitialNote by remember { mutableStateOf("") }
    var mpesaInitialTimestamp by remember { mutableStateOf<Long?>(null) }
    var mpesaExternalReference by remember { mutableStateOf<String?>(null) }
    var mpesaDuplicateMessage by remember { mutableStateOf<String?>(null) }
    var mpesaShareErrorMessage by remember { mutableStateOf<String?>(null) }
    var mpesaCreatedAccountMessage by remember { mutableStateOf<String?>(null) }
    var mpesaInitialAccountId by remember { mutableStateOf<String?>(null) }
    var mpesaInitialTransferFromId by remember { mutableStateOf<String?>(null) }
    var mpesaInitialTransferToId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sharedMpesaData) {
        val data = sharedMpesaData ?: return@LaunchedEffect
        if (data.code.isNotEmpty()) {
            val isDuplicate = repository.hasExternalReference(data.code)
            if (isDuplicate) {
                mpesaDuplicateMessage = "This M-PESA transaction has already been logged"
                return@LaunchedEffect
            }
        }

        val normalizedName = MpesaMessageParser.normalizeWhitespace(data.counterpartyName)
        val isZiidi = normalizedName.equals("ZIIDI", ignoreCase = true)

        val mpesaAccountId = withContext(Dispatchers.IO) {
            db.accountDao().getAllAccountsOnce().firstOrNull { a ->
                a.name.contains("mpesa", ignoreCase = true)
            }?.id
        }

        if (isZiidi) {
            val ziidiAccountId = withContext(Dispatchers.IO) {
                val existing = db.accountDao().getAllAccountsOnce()
                    .firstOrNull { a -> a.name.equals("Ziidi", ignoreCase = true) }
                if (existing != null) {
                    existing.id
                } else {
                    val newId = java.util.UUID.randomUUID().toString()
                    db.accountDao().insertAccount(
                        com.makemission.makeflow.data.local.entity.AccountEntity(
                            id = newId, name = "Ziidi", balance = 0.0,
                            initialBalance = 0.0, colorHex = "#26A69A"
                        )
                    )
                    mpesaCreatedAccountMessage = "Created 'Ziidi' account for M-PESA savings transfers"
                    newId
                }
            }
            mpesaInitialTransferFromId = mpesaAccountId
            mpesaInitialTransferToId = ziidiAccountId
            mpesaInitialType = "TRANSFER"
        } else {
            val type = when (data.type) {
                MpesaMessageParser.TransactionType.RECEIVED -> "INCOME"
                MpesaMessageParser.TransactionType.SENT,
                MpesaMessageParser.TransactionType.PAYBILL,
                MpesaMessageParser.TransactionType.WITHDRAW,
                MpesaMessageParser.TransactionType.AIRTIME -> "EXPENSE"
                else -> "EXPENSE"
            }
            mpesaInitialAccountId = mpesaAccountId
            mpesaInitialType = type
        }

        mpesaInitialAmount = if (data.amount == data.amount.toLong().toDouble())
            data.amount.toLong().toString() else data.amount.toString()
        mpesaInitialNote = data.counterpartyName ?: ""
        mpesaInitialTimestamp = try {
            val normalizedDate = data.date.replace(Regex("""/(\d{2})$""")) { "/20${it.groupValues[1]}" }
            val fmt = java.text.SimpleDateFormat("d/M/yyyy h:mm a", java.util.Locale.getDefault())
            fmt.parse("$normalizedDate ${data.time}")?.time
        } catch (_: Exception) { null }
        mpesaExternalReference = data.code
        showAddTransaction = true
    }

    LaunchedEffect(shareParseError) {
        shareParseError?.let {
            mpesaShareErrorMessage = it
            onShareParseErrorShown()
            showAddTransaction = true
        }
    }

    if (mpesaDuplicateMessage != null) {
        AlertDialog(
            onDismissRequest = { mpesaDuplicateMessage = null },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Duplicate Transaction", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(mpesaDuplicateMessage ?: "", color = Color.White.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = { mpesaDuplicateMessage = null }) {
                    Text("OK", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    if (mpesaShareErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { mpesaShareErrorMessage = null },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Couldn't Parse Message", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(mpesaShareErrorMessage ?: "", color = Color.White.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = { mpesaShareErrorMessage = null }) {
                    Text("OK", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    if (mpesaCreatedAccountMessage != null) {
        LaunchedEffect(mpesaCreatedAccountMessage) {
            kotlinx.coroutines.delay(5000)
            mpesaCreatedAccountMessage = null
        }
        AlertDialog(
            onDismissRequest = { mpesaCreatedAccountMessage = null },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Account Created", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(mpesaCreatedAccountMessage ?: "", color = Color.White.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = { mpesaCreatedAccountMessage = null }) {
                    Text("OK", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    var showBudgetScreen by remember { mutableStateOf(false) }
    var showAccountsScreen by remember { mutableStateOf(false) }
    var showSavingsGoalsScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showRecurringTransactionsScreen by remember { mutableStateOf(false) }
    var showRecurringSetupFromInsights by remember { mutableStateOf(false) }
    var recurringSetupAmount by remember { mutableStateOf(0.0) }
    var showManageTemplates by remember { mutableStateOf(false) }
    var showManageCategories by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var showExchangeRates by remember { mutableStateOf(false) }
    val exchangeRates by exchangeRateRepository.getAllRates().collectAsState(initial = emptyList())
    val previousTab = remember { mutableStateOf(activeTab) }
    LaunchedEffect(activeTab) {
        if (previousTab.value != activeTab) {
            hapticManager.fire(HapticEvent.TAB_SWITCH)
            previousTab.value = activeTab
        }
    }
    val scope = rememberCoroutineScope()

    var backupJson by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    settingsViewModel.exportBackup { json ->
                        backupJson = json
                    }
                    // wait briefly for the json to be ready
                    val json = backupJson
                    if (json != null) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Backup saved successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() } ?: ""
                    val validation = backupManager.validateBackup(json)
                    if (!validation.valid) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Restore failed: ${validation.error}", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        showRestoreConfirm = true
                        backupJson = json
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to read backup: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun performBackup() {
        backupLauncher.launch("Mindful_Backup_${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())}.json")
    }

    fun pickRestoreFile() {
        restoreLauncher.launch(arrayOf("application/json", "*/*"))
    }

    val config = rememberAdaptiveConfig()
    val reducedMotion = LocalReducedMotion.current
    val layoutMode = LocalDeviceLayoutMode.current
    val isNavRail = layoutMode != DeviceLayoutMode.COMPACT

    val maxWidthModifier = if (layoutMode == DeviceLayoutMode.MEDIUM && !config.isLandscape)
        Modifier.widthIn(max = config.contentMaxWidth) else Modifier

    CompositionLocalProvider(LocalAdaptiveConfig provides config) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        if (isNavRail) {
            Row(Modifier.fillMaxSize()) {
                MakeFlowNavRail(
                    selectedTab = activeTab,
                    onTabSelected = { activeTab = it },
                    layoutMode = layoutMode
                )

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(maxWidthModifier)
                            .align(Alignment.Center)
                    ) {
                        TabContentAnimated(
                            activeTab = activeTab,
                            reducedMotion = reducedMotion,
                            homeViewModel = homeViewModel,
                            insightsViewModel = insightsViewModel,
                            historyViewModel = historyViewModel,
                            hapticManager = hapticManager,
                            notificationGate = notificationGate,
                    onOpenAccounts = { showAccountsScreen = true },
                    onOpenSavingsGoals = { showSavingsGoalsScreen = true },
                    onOpenSettings = { showSettingsScreen = true },
                    onOpenBudgets = { showBudgetScreen = true },
                    onOpenRecurringTransactions = { showRecurringTransactionsScreen = true },
                    onNavigateToHome = { activeTab = MakeFlowTab.HOME },
                    onSetUpRecurringFromInsights = { amount ->
                        recurringSetupAmount = amount
                        showRecurringSetupFromInsights = true
                    },
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                        showAddTransaction = true
                    }
                )
            }

            Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = (activeTab == MakeFlowTab.HOME || activeTab == MakeFlowTab.HISTORY) && !showAddTransaction,
                            enter = scaleIn(
                                animationSpec = if (reducedMotion) tween(150) else spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(tween(150)),
                            exit = scaleOut(
                                animationSpec = if (reducedMotion) tween(100) else spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(tween(100)) + slideOutVertically(
                                animationSpec = if (reducedMotion) tween(100) else spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                targetOffsetY = { -it / 3 }
                            )
                        ) {
                            AddTransactionFloatingActionButton(
                                onClick = { hapticManager.fire(HapticEvent.NAV_SELECT); showAddTransaction = true },
                                fabSize = config.fabSize
                            )
                        }
                    }

                    OverlayPanels(
                        showBudgetScreen = showBudgetScreen,
                        onHideBudget = { showBudgetScreen = false },
                        showAccountsScreen = showAccountsScreen,
                        onHideAccounts = { showAccountsScreen = false },
                        showSettingsScreen = showSettingsScreen,
                        onHideSettings = { showSettingsScreen = false },
                        showRecurringTransactionsScreen = showRecurringTransactionsScreen,
                        onHideRecurringTransactions = { showRecurringTransactionsScreen = false },
                        budgetViewModel = budgetViewModel,
                        accountsViewModel = accountsViewModel,
                        settingsViewModel = settingsViewModel,
                        recurringTransactionsViewModel = recurringTransactionsViewModel,
                        hapticManager = hapticManager,
                        notificationGate = notificationGate,
                        reducedMotion = reducedMotion,
                        onOpenManageTemplates = { showManageTemplates = true },
                        onOpenSavingsGoals = { showSavingsGoalsScreen = true },
                        onOpenManageCategories = { showManageCategories = true },
                        onOpenExchangeRates = { showExchangeRates = true },
                        onBackup = { performBackup() },
                        onRestore = { pickRestoreFile() },
                        onDeleteAccount = { account ->
                            savingsGoalsViewModel.getActiveGoalCountForAccount(account.id)
                                .let { count ->
                                    if (count > 0) "Cannot delete account with active savings goals" else null
                                }
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                TabContentAnimated(
                    activeTab = activeTab,
                    reducedMotion = reducedMotion,
                    homeViewModel = homeViewModel,
                    insightsViewModel = insightsViewModel,
                    historyViewModel = historyViewModel,
                    hapticManager = hapticManager,
                    notificationGate = notificationGate,
                    onOpenAccounts = { showAccountsScreen = true },
                    onOpenSavingsGoals = { showSavingsGoalsScreen = true },
                    onOpenSettings = { showSettingsScreen = true },
                    onOpenBudgets = { showBudgetScreen = true },
                    onOpenRecurringTransactions = { showRecurringTransactionsScreen = true },
                    onNavigateToHome = { activeTab = MakeFlowTab.HOME },
                    onSetUpRecurringFromInsights = { amount ->
                        recurringSetupAmount = amount
                        showRecurringSetupFromInsights = true
                    },
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                        showAddTransaction = true
                    }
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(
                            top = 24.dp,
                            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    AnimatedVisibility(
                        visible = (activeTab == MakeFlowTab.HOME || activeTab == MakeFlowTab.HISTORY) && !showAddTransaction,
                        enter = scaleIn(
                            animationSpec = if (reducedMotion) tween(150) else spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(tween(150)),
                        exit = scaleOut(
                            animationSpec = if (reducedMotion) tween(100) else spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(tween(100)) + slideOutVertically(
                            animationSpec = if (reducedMotion) tween(100) else spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            targetOffsetY = { -it / 3 }
                        )
                    ) {
                        AddTransactionFloatingActionButton(
                            onClick = { hapticManager.fire(HapticEvent.NAV_SELECT); showAddTransaction = true },
                            fabSize = config.fabSize
                        )
                    }
                    AnimatedVisibility(
                        visible = activeTab == MakeFlowTab.HOME || activeTab == MakeFlowTab.HISTORY,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(100))
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    FloatingCapsuleNavBar(
                        selectedTab = activeTab,
                        onTabSelected = { activeTab = it },
                        config = config
                    )
                }

                OverlayPanels(
                    showBudgetScreen = showBudgetScreen,
                    onHideBudget = { showBudgetScreen = false },
                    showAccountsScreen = showAccountsScreen,
                    onHideAccounts = { showAccountsScreen = false },
                    showSettingsScreen = showSettingsScreen,
                    onHideSettings = { showSettingsScreen = false },
                    showRecurringTransactionsScreen = showRecurringTransactionsScreen,
                    onHideRecurringTransactions = { showRecurringTransactionsScreen = false },
                    budgetViewModel = budgetViewModel,
                    accountsViewModel = accountsViewModel,
                    settingsViewModel = settingsViewModel,
                    recurringTransactionsViewModel = recurringTransactionsViewModel,
                    hapticManager = hapticManager,
                    notificationGate = notificationGate,
                    reducedMotion = reducedMotion,
                    onOpenManageTemplates = { showManageTemplates = true },
                    onOpenSavingsGoals = { showSavingsGoalsScreen = true },
                    onOpenManageCategories = { showManageCategories = true },
                    onOpenExchangeRates = { showExchangeRates = true },
                    onBackup = { performBackup() },
                    onRestore = { pickRestoreFile() },
                    onDeleteAccount = { account ->
                        savingsGoalsViewModel.getActiveGoalCountForAccount(account.id)
                            .let { count ->
                                if (count > 0) "Cannot delete account with active savings goals" else null
                            }
                    }
                )
            }
        }
    }

    if (showExchangeRates) {
        ExchangeRatesSheet(
            rates = exchangeRates,
            displayCurrency = currencyCode,
            onDismiss = { showExchangeRates = false },
            onSaveRate = { rate ->
                scope.launch(Dispatchers.IO) {
                    exchangeRateRepository.upsertRate(rate)
                }
            }
        )
    }

    if (showManageCategories) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121315))
        ) {
            BackHandler { showManageCategories = false }
            CategoryManagementScreen(
                categories = categories,
                onBack = { showManageCategories = false },
                onMerge = { sourceId, targetId ->
                    repository.mergeCategories(sourceId, targetId)
                },
                onConvertType = { categoryId, newType ->
                    repository.convertCategoryType(categoryId, newType)
                },
                onCreate = { entity ->
                    repository.insertCategory(entity)
                    null
                },
                onDelete = { categoryId ->
                    repository.deleteCategoryWithGuards(categoryId)
                }
            )
        }
    }

    if (showRestoreConfirm && backupJson != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; backupJson = null },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Restore Backup?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will replace ALL current data (accounts, transactions, categories, budgets, etc.) with the data from the backup file. This action cannot be undone.", color = Color.White.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = {
                    val json = backupJson
                    showRestoreConfirm = false
                    backupJson = null
                    if (json != null) {
                        scope.launch(Dispatchers.IO) {
                            settingsViewModel.importBackup(json) { error ->
                                scope.launch {
                                    if (error != null) {
                                        restoreError = error
                                    } else {
                                        Toast.makeText(context, "Restore completed successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }) {
                    Text("Restore", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false; backupJson = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    if (restoreError != null) {
        AlertDialog(
            onDismissRequest = { restoreError = null },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Restore Failed", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(restoreError ?: "Unknown error", color = Color.White.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = { restoreError = null }) {
                    Text("OK", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showAddTransaction,
        enter = slideInHorizontally(
            animationSpec = if (reducedMotion) tween(200) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { it }
        ) + fadeIn(tween(200)),
        exit = slideOutHorizontally(
            animationSpec = if (reducedMotion) tween(150) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            targetOffsetX = { it }
        ) + fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121315))
        ) {
            val onDismissAction = {
                shortcutPendingType.value = null
                editingTransaction = null
                mpesaInitialAmount = ""
                mpesaInitialType = null
                mpesaInitialNote = ""
                mpesaInitialTimestamp = null
                mpesaExternalReference = null
                mpesaInitialAccountId = null
                mpesaInitialTransferFromId = null
                mpesaInitialTransferToId = null
                showAddTransaction = false
            }
            BackHandler { onDismissAction() }
            AddTransactionScreen(
                initialType = editingTransaction?.type ?: mpesaInitialType ?: shortcutPendingType.value ?: "INCOME",
                initialAmount = mpesaInitialAmount,
                initialNote = mpesaInitialNote,
                initialTimestamp = mpesaInitialTimestamp,
                initialAccountId = mpesaInitialAccountId,
                initialTransferFromId = mpesaInitialTransferFromId,
                initialTransferToId = mpesaInitialTransferToId,
                onDismiss = onDismissAction,
                onSave = { amount, type, categoryId, accountId, note, timestamp, fromAccountId, toAccountId ->
                    scope.launch(Dispatchers.IO) {
                        repository.insertTransaction(
                            TransactionEntity(
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                accountId = accountId,
                                fromAccountId = fromAccountId,
                                toAccountId = toAccountId,
                                note = note,
                                timestamp = timestamp,
                                externalReference = mpesaExternalReference
                            )
                        )
                        WidgetUpdateHelper.updateWidgets(context)
                        mpesaExternalReference = null
                        showAddTransaction = false
                    }
                },
                accounts = accounts,
                categories = categories,
                currencySymbol = currencySymbol,
                currencyCode = currencyCode,
                hapticManager = hapticManager,
                templates = if (editingTransaction == null) templates else emptyList(),
                onSaveAsTemplate = if (editingTransaction == null) ({ name, amount, type, categoryId, accountId, note ->
                    scope.launch(Dispatchers.IO) {
                        templateRepository.insertTemplate(
                            TransactionTemplateEntity(
                                name = name,
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                accountId = accountId,
                                note = note
                            )
                        )
                    }
                }) else null,
                onAnomalyCheck = { amount, categoryId ->
                    if (categoryId == null) return@AddTransactionScreen null
                    val history = repository.getRecentAmountsForCategory(categoryId)
                    AnomalyDetector.analyze(amount, history)
                },
                onCheckDuplicate = { entity ->
                    repository.checkDuplicate(entity)
                },
                onGetPastTransactions = {
                    val pairs = repository.getRecentDescriptionsAndCategories()
                    pairs.map { PastTransactionForSuggestion(it.description, it.categoryId) }
                },
                categoryClassifier = categoryClassifier,
                smartCategorizationEnabled = smartCategorizationEnabled,
                correctionMemory = correctionMemory,
                onCategoryCorrected = { description, correctedCategoryId, originalSuggestedCategoryId ->
                    val correctedCat = categories.find { it.id == correctedCategoryId } ?: return@AddTransactionScreen
                    val originalCat = originalSuggestedCategoryId?.let { id -> categories.find { it.id == id } }
                    val route = CorrectionRouter.route(correctedCat.name, labelSet)
                    scope.launch(Dispatchers.IO) {
                        when (route) {
                            CorrectionRoute.PART_B_TFLITE -> {
                                val labelIndex = labelSet.indexOf(correctedCat.name)
                                if (labelIndex >= 0) {
                                    correctionRepository.queueCorrection(
                                        description = description,
                                        correctedLabelName = correctedCat.name,
                                        correctedLabelIndex = labelIndex,
                                        originalSuggestedLabelName = originalCat?.name
                                    )
                                    if (categoryClassifier is RetrainableCategoryClassifier) {
                                        correctionRepository.trainBatchIfReady(
                                            categoryClassifier as RetrainableCategoryClassifier
                                        )
                                    }
                                }
                            }
                            CorrectionRoute.PART_C_CORRECTION_MEMORY -> {
                                val normalized = CategorySuggester.normalize(description)
                                correctionRepository.upsertCorrectionMemory(normalized, correctedCategoryId)
                                val updatedMemory = correctionRepository.getAllCorrectionMemory()
                                withContext(Dispatchers.Main) {
                                    correctionMemory = updatedMemory
                                }
                            }
                        }
                    }
                },
                editTransaction = editingTransaction,
                onUpdate = { entity ->
                    scope.launch(Dispatchers.IO) {
                        repository.updateTransaction(entity)
                        WidgetUpdateHelper.updateWidgets(context)
                        editingTransaction = null
                        showAddTransaction = false
                    }
                }
            )
        }
    }

    if (showManageTemplates) {
        ManageTemplatesSheet(
            templates = templates,
            onDismiss = { showManageTemplates = false },
            onRename = { id, newName ->
                scope.launch(Dispatchers.IO) {
                    templateRepository.getTemplateById(id)?.let {
                        templateRepository.updateTemplate(it.copy(name = newName))
                    }
                }
            },
            onDelete = { template ->
                scope.launch(Dispatchers.IO) {
                    templateRepository.deleteTemplate(template)
                }
            }
        )
    }

    if (showSavingsGoalsScreen) {
        SavingsGoalsScreen(
            onBack = { showSavingsGoalsScreen = false },
            viewModel = savingsGoalsViewModel,
            accounts = accounts,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            onAddAccount = { showAddAccountSheet = true }
        )
    }

    if (showAddAccountSheet) {
        AddEditAccountBottomSheet(
            onDismiss = { showAddAccountSheet = false },
            onSave = { name, balance, colorHex ->
                scope.launch(Dispatchers.IO) {
                    val account = com.makemission.makeflow.data.local.entity.AccountEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        balance = balance,
                        initialBalance = balance,
                        colorHex = colorHex,
                        currencyCode = null
                    )
                    repository.insertAccount(account)
                }
            },
            currencySymbol = currencySymbol
        )
    }

    if (showRecurringSetupFromInsights) {
        AddEditRecurringTransactionBottomSheet(
            onDismiss = { showRecurringSetupFromInsights = false },
            onSave = { entity: com.makemission.makeflow.data.local.entity.RecurringTransactionEntity ->
                scope.launch(Dispatchers.IO) {
                    repository.createRecurringTransaction(entity)
                    showRecurringSetupFromInsights = false
                }
            },
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            prefillAmount = recurringSetupAmount,
            prefillDescription = "Savings"
        )
    }
}

}

@Composable
private fun TabContentAnimated(
    activeTab: MakeFlowTab,
    reducedMotion: Boolean,
    homeViewModel: HomeDashboardViewModel,
    insightsViewModel: InsightsViewModel,
    historyViewModel: HistoryViewModel,
    hapticManager: com.makemission.makeflow.core.haptics.HapticManager,
    notificationGate: NotificationGate?,
    onOpenAccounts: () -> Unit,
    onOpenSavingsGoals: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurringTransactions: () -> Unit,
    onNavigateToHome: () -> Unit,
    onSetUpRecurringFromInsights: (Double) -> Unit = {},
    onEditTransaction: ((TransactionEntity) -> Unit)? = null
) {
    val tabIndex = when (activeTab) {
        MakeFlowTab.HOME -> 0
        MakeFlowTab.INSIGHTS -> 1
        MakeFlowTab.HISTORY -> 2
    }
    val previousTabIndex = remember { mutableStateOf(0) }

    AnimatedContent(
        targetState = activeTab,
        transitionSpec = {
            val direction = if (tabIndex >= previousTabIndex.value) 1 else -1
            previousTabIndex.value = tabIndex
            val slideSpec = if (reducedMotion) tween<androidx.compose.ui.unit.IntOffset>(200) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
            (fadeIn(if (reducedMotion) tween(150) else tween(220)) + slideInHorizontally(
                animationSpec = slideSpec,
                initialOffsetX = { it / 6 * direction }
            )) togetherWith
            (fadeOut(if (reducedMotion) tween(120) else tween(180)) + slideOutHorizontally(
                animationSpec = slideSpec,
                targetOffsetX = { -it / 6 * direction }
            ))
        },
        label = "TabContent"
    ) { tab ->
        when (tab) {
            MakeFlowTab.HOME -> {
                HomeDashboard(
                    viewModel = homeViewModel,
                    onOpenAccounts = onOpenAccounts,
                    onOpenSavingsGoals = onOpenSavingsGoals,
                    onOpenSettings = onOpenSettings,
                    hapticManager = hapticManager,
                    notificationGate = notificationGate
                )
            }
            MakeFlowTab.INSIGHTS -> {
                BackHandler { onNavigateToHome() }
                InsightsScreen(
                    viewModel = insightsViewModel,
                    onOpenBudgets = onOpenBudgets,
                    onSetUpRecurring = onSetUpRecurringFromInsights,
                    hapticManager = hapticManager,
                    notificationGate = notificationGate
                )
            }
            MakeFlowTab.HISTORY -> {
                BackHandler { onNavigateToHome() }
                HistoryScreen(
                    viewModel = historyViewModel,
                    hapticManager = hapticManager,
                    onEditTransaction = onEditTransaction
                )
            }
        }
    }
}

@Composable
private fun AddTransactionFloatingActionButton(
    onClick: () -> Unit,
    fabSize: androidx.compose.ui.unit.Dp
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.White,
        contentColor = Color(0xFF121315),
        shape = CircleShape,
        modifier = Modifier.size(fabSize)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add transaction",
            tint = Color(0xFF121315),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun OverlayPanels(
    showBudgetScreen: Boolean,
    onHideBudget: () -> Unit,
    showAccountsScreen: Boolean,
    onHideAccounts: () -> Unit,
    showSettingsScreen: Boolean,
    onHideSettings: () -> Unit,
    showRecurringTransactionsScreen: Boolean,
    onHideRecurringTransactions: () -> Unit,
    budgetViewModel: BudgetViewModel,
    accountsViewModel: AccountsViewModel,
    settingsViewModel: SettingsViewModel,
    recurringTransactionsViewModel: RecurringTransactionsViewModel,
    hapticManager: com.makemission.makeflow.core.haptics.HapticManager,
    notificationGate: NotificationGate?,
    reducedMotion: Boolean,
    onOpenManageTemplates: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenManageCategories: () -> Unit = {},
    onOpenExchangeRates: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onDeleteAccount: (suspend (AccountEntity) -> String?)? = null
) {
    AnimatedVisibility(
        visible = showBudgetScreen,
        enter = slideInVertically(
            animationSpec = if (reducedMotion) tween(200) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it }
        ) + fadeIn(tween(200)),
        exit = slideOutVertically(
            animationSpec = if (reducedMotion) tween(150) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            targetOffsetY = { it }
        ) + fadeOut(tween(150))
    ) {
        Box(Modifier.fillMaxSize()) {
            BackHandler { onHideBudget() }
            BudgetScreen(
                onBack = onHideBudget,
                viewModel = budgetViewModel,
                hapticManager = hapticManager,
                notificationGate = notificationGate
            )
        }
    }

    AnimatedVisibility(
        visible = showAccountsScreen,
        enter = slideInHorizontally(
            animationSpec = if (reducedMotion) tween(200) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { it }
        ) + fadeIn(tween(200)),
        exit = slideOutHorizontally(
            animationSpec = if (reducedMotion) tween(150) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            targetOffsetX = { it }
        ) + fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
        ) {
            BackHandler { onHideAccounts() }
            AccountsScreen(
                onBack = onHideAccounts,
                viewModel = accountsViewModel,
                onDeleteAccount = onDeleteAccount
            )
        }
    }

    AnimatedVisibility(
        visible = showSettingsScreen,
        enter = slideInHorizontally(
            animationSpec = if (reducedMotion) tween(200) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { it }
        ) + fadeIn(tween(200)),
        exit = slideOutHorizontally(
            animationSpec = if (reducedMotion) tween(150) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            targetOffsetX = { it }
        ) + fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121315))
        ) {
            BackHandler { onHideSettings() }
            SettingsScreen(
                onBack = onHideSettings,
                viewModel = settingsViewModel,
                hapticManager = hapticManager,
                notificationGate = notificationGate,
                onOpenRecurringTransactions = onHideRecurringTransactions,
                onOpenSavingsGoals = onOpenSavingsGoals,
                onOpenManageTemplates = onOpenManageTemplates,
                onOpenManageCategories = onOpenManageCategories,
                onOpenExchangeRates = onOpenExchangeRates,
                onBackup = onBackup,
                onRestore = onRestore
            )
        }
    }

    AnimatedVisibility(
        visible = showRecurringTransactionsScreen,
        enter = slideInHorizontally(
            animationSpec = if (reducedMotion) tween(200) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { it }
        ) + fadeIn(tween(200)),
        exit = slideOutHorizontally(
            animationSpec = if (reducedMotion) tween(150) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            targetOffsetX = { it }
        ) + fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121315))
        ) {
            BackHandler { onHideRecurringTransactions() }
            RecurringTransactionsScreen(
                onBack = onHideRecurringTransactions,
                viewModel = recurringTransactionsViewModel
            )
        }
    }
}

@Composable
private fun MakeFlowNavRail(
    selectedTab: MakeFlowTab,
    onTabSelected: (MakeFlowTab) -> Unit,
    layoutMode: DeviceLayoutMode
) {
    val alwaysShowLabel = layoutMode == DeviceLayoutMode.EXPANDED

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp),
        containerColor = Color.Transparent,
        header = {
            Text(
                text = "Mindful Wallet",
                color = Color(0xFF8B938A),
                fontSize = if (alwaysShowLabel) 12.sp else 0.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    ) {
        MakeFlowTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            MakeFlowTab.HOME -> Icons.Rounded.SpaceDashboard
                            MakeFlowTab.INSIGHTS -> Icons.Rounded.Insights
                            MakeFlowTab.HISTORY -> Icons.Rounded.History
                        },
                        contentDescription = tab.name
                    )
                },
                label = {
                    Text(
                        text = when (tab) {
                            MakeFlowTab.HOME -> "Home"
                            MakeFlowTab.INSIGHTS -> "Insights"
                            MakeFlowTab.HISTORY -> "History"
                        },
                        fontSize = 12.sp
                    )
                },
                alwaysShowLabel = alwaysShowLabel
            )
        }
    }
}

@Composable
fun FloatingCapsuleNavBar(
    selectedTab: MakeFlowTab,
    onTabSelected: (MakeFlowTab) -> Unit,
    config: AdaptiveConfig = AdaptiveConfig()
) {
    val tabs = MakeFlowTab.values()
    val reducedMotion = LocalReducedMotion.current

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF24262B))
            .padding(horizontal = if (config.isTablet) 12.dp else 8.dp, vertical = if (config.isTablet) 8.dp else 6.dp),
        horizontalArrangement = Arrangement.spacedBy(if (config.isTablet) 10.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    .padding(horizontal = if (isSelected) (if (config.isTablet) 20.dp else 16.dp) else (if (config.isTablet) 16.dp else 12.dp), vertical = if (config.isTablet) 14.dp else 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (tab) {
                        MakeFlowTab.HOME -> Icons.Rounded.SpaceDashboard
                        MakeFlowTab.INSIGHTS -> Icons.Rounded.Insights
                        MakeFlowTab.HISTORY -> Icons.Rounded.History
                    },
                    contentDescription = tab.name,
                    tint = if (isSelected) Color(0xFF121315) else Color.White,
                    modifier = Modifier.size(config.navIconSize)
                )

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(
                        animationSpec = if (reducedMotion) tween(100) else spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + slideInVertically(
                        animationSpec = if (reducedMotion) tween(100) else spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        initialOffsetY = { it }
                    ),
                    exit = fadeOut(
                        animationSpec = if (reducedMotion) tween(80) else spring(stiffness = Spring.StiffnessHigh)
                    ) + slideOutVertically(
                        animationSpec = if (reducedMotion) tween(80) else spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        ),
                        targetOffsetY = { -it / 2 }
                    )
                ) {
                    Text(
                        text = when (tab) {
                            MakeFlowTab.HOME -> "Home"
                            MakeFlowTab.INSIGHTS -> "Insights"
                            MakeFlowTab.HISTORY -> "History"
                        },
                        color = Color(0xFF121315),
                        fontSize = if (config.isTablet) 16.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(start = if (config.isTablet) 10.dp else 8.dp)
                    )
                }
            }
        }
    }
}
