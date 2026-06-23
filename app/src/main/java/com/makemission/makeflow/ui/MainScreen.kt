package com.makemission.makeflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.screens.AccountsScreen
import com.makemission.makeflow.ui.screens.AddTransactionBottomSheet
import com.makemission.makeflow.ui.screens.BudgetScreen
import com.makemission.makeflow.ui.screens.HistoryScreen
import com.makemission.makeflow.ui.screens.HomeDashboard
import com.makemission.makeflow.ui.screens.InsightsScreen
import com.makemission.makeflow.ui.screens.SettingsScreen
import com.makemission.makeflow.ui.viewmodel.AccountsViewModel
import com.makemission.makeflow.ui.viewmodel.AccountsViewModelFactory
import com.makemission.makeflow.ui.viewmodel.BudgetViewModel
import com.makemission.makeflow.ui.viewmodel.BudgetViewModelFactory
import com.makemission.makeflow.ui.viewmodel.HistoryViewModel
import com.makemission.makeflow.ui.viewmodel.HistoryViewModelFactory
import com.makemission.makeflow.ui.viewmodel.HomeDashboardViewModel
import com.makemission.makeflow.ui.viewmodel.HomeDashboardViewModelFactory
import com.makemission.makeflow.ui.viewmodel.InsightsViewModel
import com.makemission.makeflow.ui.viewmodel.InsightsViewModelFactory
import com.makemission.makeflow.ui.viewmodel.SettingsViewModel
import com.makemission.makeflow.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

enum class MakeFlowTab {
    HOME, INSIGHTS, HISTORY
}

@Composable
fun MainScreen(
    username: String,
    onLogout: () -> Unit
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
            budgetDao = db.budgetDao()
        )
    }
    val userPrefDataStore = remember { UserPreferencesDataStore(context.userPrefDataStore) }
    val homeViewModel: HomeDashboardViewModel = viewModel(
        factory = HomeDashboardViewModelFactory(repository, userPrefDataStore)
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
        factory = AccountsViewModelFactory(repository, userPrefDataStore)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(repository, userPrefDataStore)
    )

    val accounts by homeViewModel.accounts.collectAsState()
    val categories by homeViewModel.categories.collectAsState()
    val currencySymbol by homeViewModel.currencySymbol.collectAsState()

    var showAddTransaction by remember { mutableStateOf(false) }
    var showBudgetScreen by remember { mutableStateOf(false) }
    var showAccountsScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (activeTab) {
                MakeFlowTab.HOME -> {
                    HomeDashboard(
                        viewModel = homeViewModel,
                        onOpenAccounts = { showAccountsScreen = true },
                        onOpenSettings = { showSettingsScreen = true }
                    )
                }
                MakeFlowTab.INSIGHTS -> {
                    InsightsScreen(
                        viewModel = insightsViewModel,
                        onOpenBudgets = { showBudgetScreen = true }
                    )
                }
                MakeFlowTab.HISTORY -> {
                    HistoryScreen(viewModel = historyViewModel)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (activeTab == MakeFlowTab.HOME || activeTab == MakeFlowTab.HISTORY) {
                FloatingActionButton(
                    onClick = { showAddTransaction = true },
                    containerColor = Color.White,
                    contentColor = Color(0xFF121315),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add transaction",
                        tint = Color(0xFF121315)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            FloatingCapsuleNavBar(
                selectedTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }

        AnimatedVisibility(
            visible = showBudgetScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            BudgetScreen(
                onBack = { showBudgetScreen = false },
                viewModel = budgetViewModel
            )
        }

        AnimatedVisibility(
            visible = showAccountsScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0A))
            ) {
                AccountsScreen(
                    onBack = { showAccountsScreen = false },
                    viewModel = accountsViewModel
                )
            }
        }

        AnimatedVisibility(
            visible = showSettingsScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121315))
            ) {
                SettingsScreen(
                    onBack = { showSettingsScreen = false },
                    viewModel = settingsViewModel
                )
            }
        }
    }

    if (showAddTransaction) {
        AddTransactionBottomSheet(
            onDismiss = { showAddTransaction = false },
            onSave = { amount, type, categoryId, accountId, note, timestamp ->
                scope.launch {
                    repository.insertTransaction(
                        TransactionEntity(
                            amount = amount,
                            type = type,
                            categoryId = categoryId,
                            accountId = accountId,
                            note = note,
                            timestamp = timestamp
                        )
                    )
                    showAddTransaction = false
                }
            },
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol
        )
    }
}

@Composable
fun FloatingCapsuleNavBar(
    selectedTab: MakeFlowTab,
    onTabSelected: (MakeFlowTab) -> Unit
) {
    val tabs = MakeFlowTab.values()

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF24262B))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 10.dp),
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
                    modifier = Modifier.size(20.dp)
                )

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh))
                ) {
                    Text(
                        text = when (tab) {
                            MakeFlowTab.HOME -> "Home"
                            MakeFlowTab.INSIGHTS -> "Insights"
                            MakeFlowTab.HISTORY -> "History"
                        },
                        color = Color(0xFF121315),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
