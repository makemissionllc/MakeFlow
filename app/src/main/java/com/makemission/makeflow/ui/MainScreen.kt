package com.makemission.makeflow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.components.ExpressiveNavDock
import com.makemission.makeflow.ui.components.MakeFlowTab
import com.makemission.makeflow.ui.screens.AnalyticsScreen
import com.makemission.makeflow.ui.screens.FocusDashboardView
import com.makemission.makeflow.ui.screens.GeneralSettingsBottomSheet
import com.makemission.makeflow.ui.screens.HistoryScreen
import com.makemission.makeflow.ui.screens.TransactionEntrySheetContent
import com.makemission.makeflow.ui.screens.AccountsManagementScreen
import com.makemission.makeflow.ui.screens.CategoriesManagementScreen
import com.makemission.makeflow.ui.screens.BudgetsManagementScreen
import com.makemission.makeflow.ui.screens.TransactionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onboardingName: String = "Developer",
    currency: String = "KSh",
    initialDeposit: Double = 12500.00,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(MakeFlowTab.FOCUS) }
    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var isTransactionSheetOpen by remember { mutableStateOf(false) }

    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val transactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Room Database State Monitoring
    val db = remember { AppDatabase.getDatabase(context) }
    val accountsState by db.accountDao().getAllAccountsFlow().collectAsState(initial = emptyList())
    val categoriesState by db.categoryDao().getAllCategoriesFlow().collectAsState(initial = emptyList())
    val budgetsState by db.budgetDao().getAllBudgetsFlow().collectAsState(initial = emptyList())
    val transactionsState by db.transactionDao().getAllTransactions().collectAsState(initial = emptyList())

    var internalDashboardSubScreen by remember { mutableStateOf("MAIN") }

    val backPressedEnabled = activeTab != MakeFlowTab.FOCUS || internalDashboardSubScreen != "MAIN"
    BackHandler(enabled = backPressedEnabled) {
        if (internalDashboardSubScreen != "MAIN") {
            internalDashboardSubScreen = "MAIN"
        }
        if (activeTab != MakeFlowTab.FOCUS) {
            activeTab = MakeFlowTab.FOCUS
        }
    }

    val totalBalance = if (accountsState.isNotEmpty()) accountsState.sumOf { it.balance } else initialDeposit

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 80.dp)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    slideIntoContainer(
                        towards = direction,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ).togetherWith(
                        slideOutOfContainer(
                            towards = direction,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    )
                },
                label = "TabTransition",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    MakeFlowTab.FOCUS -> {
                        AnimatedContent(
                            targetState = internalDashboardSubScreen,
                            transitionSpec = {
                                if (targetState != "MAIN") {
                                    // Slide in from right, fade in
                                    (slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 3 },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeOut()
                                    )
                                } else {
                                    // Slide out to right, fade out
                                    (slideInHorizontally(
                                        initialOffsetX = { -it / 3 },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeOut()
                                    )
                                }
                            },
                            label = "SubScreenTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { subScreen ->
                            when (subScreen) {
                                "MAIN" -> FocusDashboardView(
                                    onboardingName = onboardingName,
                                    currency = currency,
                                    initialDeposit = totalBalance,
                                    transactions = transactionsState,
                                    categories = categoriesState,
                                    accounts = accountsState,
                                    onSettingsClick = { isSettingsSheetOpen = true },
                                    onNavigateToSubScreen = { internalDashboardSubScreen = it }
                                )
                                "ACCOUNTS" -> AccountsManagementScreen(
                                    accountsList = accountsState,
                                    onAddAccount = { acc -> coroutineScope.launch { db.accountDao().insertAccount(acc) } },
                                    onBack = { internalDashboardSubScreen = "MAIN" }
                                )
                                "CATEGORIES" -> CategoriesManagementScreen(
                                    categoriesList = categoriesState,
                                    onAddCategory = { cat -> coroutineScope.launch { db.categoryDao().insertCategory(cat) } },
                                    onBack = { internalDashboardSubScreen = "MAIN" }
                                )
                                "BUDGETS" -> BudgetsManagementScreen(
                                    budgetsList = budgetsState,
                                    categoriesList = categoriesState,
                                    onAddBudget = { bgt -> coroutineScope.launch { db.budgetDao().insertBudget(bgt) } },
                                    onBack = { internalDashboardSubScreen = "MAIN" }
                                )
                            }
                        }
                    }
                    MakeFlowTab.HISTORY -> HistoryScreen(
                        currency = currency,
                        initialDeposit = totalBalance,
                        transactions = transactionsState,
                        categories = categoriesState
                    )
                    MakeFlowTab.INSIGHTS -> AnalyticsScreen(
                        currency = currency,
                        initialDeposit = totalBalance,
                        transactions = transactionsState,
                        categories = categoriesState,
                        accounts = accountsState
                    )
                }
            }
        }

        ExpressiveNavDock(
            currentTab = activeTab,
            onTabSelected = { 
                activeTab = it 
                internalDashboardSubScreen = "MAIN" // reset internal state on tab change
            },
            onFabClick = { isTransactionSheetOpen = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        if (isSettingsSheetOpen) {
            GeneralSettingsBottomSheet(
                sheetState = settingsSheetState,
                onDismissRequest = { isSettingsSheetOpen = false }
            )
        }

        if (isTransactionSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isTransactionSheetOpen = false },
                sheetState = transactionSheetState,
                containerColor = Color(0xFF0F1011),
                dragHandle = null // Content uses its custom close layout system natively
            ) {
                TransactionEntrySheetContent(
                    dbAccounts = accountsState,
                    dbCategories = categoriesState,
                    onDismiss = { isTransactionSheetOpen = false },
                    onSaveTransaction = { type, account, category, amount ->
                        coroutineScope.launch {
                            try {
                                val database = AppDatabase.getDatabase(context)

                                // 1. Deduct or add from/to account balance
                                val updatedBalance = when (type) {
                                    TransactionType.INCOME -> account.balance + amount
                                    TransactionType.EXPENSE -> account.balance - amount
                                    TransactionType.TRANSFER -> account.balance - amount
                                }
                                val updatedAccount = account.copy(balance = updatedBalance)
                                database.accountDao().insertAccount(updatedAccount)

                                // 2. If transfer, add to target account balance
                                if (type == TransactionType.TRANSFER) {
                                    val destAccountName = category.name.removePrefix("To ").trim()
                                    val destAccount = accountsState.find { it.name == destAccountName }
                                    if (destAccount != null) {
                                        val destUpdatedAccount = destAccount.copy(balance = destAccount.balance + amount)
                                        database.accountDao().insertAccount(destUpdatedAccount)
                                    }
                                }

                                // 3. Update budget spending if it's an EXPENSE
                                if (type == TransactionType.EXPENSE) {
                                    database.budgetDao().updateBudgetSpending(category.id, amount)
                                }

                                // 4. Insert transaction entity
                                val tx = TransactionEntity(
                                    amount = amount,
                                    type = type.name,
                                    categoryId = category.id,
                                    note = if (type == TransactionType.TRANSFER) "Transfer to ${category.name}" else "Transaction via ${account.name} -> ${category.name}",
                                    timestamp = System.currentTimeMillis()
                                )
                                database.transactionDao().insertTransaction(tx)
                            } catch (e: Exception) {
                                android.util.Log.e("MakeFlowDebug", "Failed to save transaction", e)
                            }
                            isTransactionSheetOpen = false
                        }
                    }
                )
            }
        }
    }
}

