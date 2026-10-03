package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.notifications.NotificationGate
import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.CashFlowProjection
import com.makemission.makeflow.domain.DashboardCardType
import com.makemission.makeflow.domain.VelocityAlert
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.ui.components.AnimatedProgressBar
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.ui.viewmodel.FocusedAccountState
import com.makemission.makeflow.ui.viewmodel.HomeDashboardViewModel
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

data class AccountChip(
    val name: String,
    val balance: String,
    val color: Color
)

data class CategoryLegend(
    val name: String,
    val value: String,
    val color: Color
)

@Composable
fun HomeDashboard(
    viewModel: HomeDashboardViewModel,
    username: String = "",
    onOpenAccounts: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onAddTransaction: (String) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    hapticManager: HapticManager? = null,
    notificationGate: NotificationGate? = null
) {
    val dimens = LocalAppDimens.current
    val totalBalance by viewModel.totalBalance.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val focusedAccountReady by viewModel.focusedAccountReady.collectAsState()
    val focusedAccountId by viewModel.focusedAccountId.collectAsState()
    val focusedAccountState by viewModel.focusedAccountState.collectAsState()
    val balanceCurrencyCode by viewModel.balanceCurrencyCode.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val parentCategorySpending by viewModel.parentCategorySpending.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val recurringPatterns by viewModel.recurringPatterns.collectAsState()
    val pendingRecurringTransactions by viewModel.pendingRecurringTransactions.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val cardOrder by viewModel.cardOrder.collectAsState()
    val cashFlowProjection by viewModel.cashFlowProjection.collectAsState()
    val velocityAlerts by viewModel.velocityAlerts.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showReorderSheet by remember { mutableStateOf(false) }
    var showAccountFocusSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val currentMonthLabel = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }

    val config = LocalAdaptiveConfig.current
    val focusedAccountName = when (val state = focusedAccountState) {
        is FocusedAccountState.Loading -> null
        is FocusedAccountState.ShowAll -> null
        is FocusedAccountState.Focused -> accounts.find { it.id == state.accountId }?.name
    }

    val formattedBalance = CurrencyFormatter.format(totalBalance, balanceCurrencyCode)
    val formattedIncome = CurrencyFormatter.format(monthlyIncome, selectedCurrency)
    val formattedExpenses = CurrencyFormatter.format(monthlyExpenses, selectedCurrency)
    val formattedNet = CurrencyFormatter.format(monthlyIncome - monthlyExpenses, selectedCurrency)

    val accountChips = accounts.map { entity ->
        val color = try {
            Color(android.graphics.Color.parseColor(entity.colorHex))
        } catch (_: Exception) {
            MaterialTheme.colorScheme.primary
        }
        AccountChip(entity.name, CurrencyFormatter.format(entity.balance, selectedCurrency), color)
    }

    val categoryLegends = parentCategorySpending.map { pcs ->
        val color = try {
            Color(android.graphics.Color.parseColor(pcs.color))
        } catch (_: Exception) {
            MaterialTheme.colorScheme.error
        }
        CategoryLegend(pcs.categoryName, CurrencyFormatter.format(pcs.amount, selectedCurrency), color)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = config.horizontalPadding)
                .verticalFadingEdge(
                    topFadeHeight = config.fadingEdgeHeight,
                    bottomFadeHeight = config.fadingEdgeHeight
                ),
            verticalArrangement = Arrangement.spacedBy(config.cardSpacing),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
                bottom = 140.dp
            )
        ) {
            item {
                LargeTitleItem(title = "Home", scrollState = listState)
            }
            item {
                HomeWelcomeHeader(username = username)
            }
            if (focusedAccountReady) {
                item {
                    AccountFocusSelector(
                        focusedAccountName = focusedAccountName,
                        onClick = { showAccountFocusSheet = true }
                    )
                }
                item {
                    TotalBalanceCard(
                        balance = formattedBalance,
                        label = focusedAccountName ?: "Total balance",
                        supportingLabel = if (focusedAccountName == null) {
                            when (accounts.size) {
                                0 -> "No accounts yet"
                                1 -> "Across 1 account"
                                else -> "Across ${accounts.size} accounts"
                            }
                        } else {
                            "Focused account"
                        }
                    )
                }
                item {
                    RecentActivityCard(
                        transactions = recentTransactions,
                        accounts = accounts,
                        categories = categories,
                        currencyCode = selectedCurrency,
                        onAddTransaction = onAddTransaction,
                        onOpenHistory = onOpenHistory,
                        onEditTransaction = onEditTransaction
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(dimens.shapeCornerMedium))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
            }
            cardOrder.forEach { cardType ->
                when (cardType) {
                    DashboardCardType.INSIGHTS_SUMMARY -> item {
                        Box(Modifier.animateItem()) {
                            CashFlowCard(
                                income = formattedIncome,
                                expenses = formattedExpenses,
                                net = formattedNet,
                                monthLabel = currentMonthLabel,
                                onTap = { viewModel.logCardInteraction(DashboardCardType.INSIGHTS_SUMMARY) }
                            )
                        }
                    }
                    DashboardCardType.ACCOUNTS -> item {
                        Box(Modifier.animateItem()) {
                            AccountsCard(
                                accounts = accountChips,
                                onOpenAccounts = {
                                    viewModel.logCardInteraction(DashboardCardType.ACCOUNTS)
                                    onOpenAccounts()
                                },
                                onOpenSavingsGoals = onOpenSavingsGoals
                            )
                        }
                    }
                    DashboardCardType.BUDGET_PROGRESS -> if (budgets.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .clickable {
                                        viewModel.logCardInteraction(DashboardCardType.BUDGET_PROGRESS)
                                    }
                            ) {
                                BudgetsCard(budgets = budgets, currencyCode = selectedCurrency, hapticManager = hapticManager, notificationGate = notificationGate)
                            }
                        }
                    }
                    DashboardCardType.RECURRING -> {
                        if (pendingRecurringTransactions.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.animateItem()) {
                                    PendingRecurringCard(
                                        items = pendingRecurringTransactions,
                                        currencyCode = selectedCurrency,
                                        onConfirm = { viewModel.confirmRecurringTransaction(it) },
                                        onSkip = { viewModel.skipRecurringTransaction(it) }
                                    )
                                }
                            }
                        }
                        if (recurringPatterns.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .animateItem()
                                        .clickable {
                                            viewModel.logCardInteraction(DashboardCardType.RECURRING)
                                        }
                                ) {
                                    RecurringCard(patterns = recurringPatterns, currencyCode = selectedCurrency)
                                }
                            }
                        }
                    }
                }
            }
            if (categoryLegends.isNotEmpty()) item {
                CategoriesCard(categories = categoryLegends.take(5), monthLabel = currentMonthLabel)
            }
            if (velocityAlerts.isNotEmpty()) {
                item {
                    VelocityAlertsCard(
                        alerts = velocityAlerts,
                        currencyCode = selectedCurrency
                    )
                }
            }
            cashFlowProjection?.let { projection ->
                item {
                    ProjectionCard(projection = projection, currencyCode = selectedCurrency)
                }
            }
            item {
                Spacer(modifier = Modifier.height(dimens.spacingXSmall))
                Text(
                    text = "Edit cards",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReorderSheet = true }
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        CollapsingTopBar(
            title = "Home",
            scrollState = listState,
            modifier = Modifier.align(Alignment.TopStart),
            actions = {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Local storage",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
    }

    if (showReorderSheet) {
        ReorderCardsSheet(
            currentOrder = cardOrder,
            onSave = { newOrder ->
                viewModel.saveManualCardOrder(newOrder)
            },
            onReset = {
                viewModel.clearManualCardOrder()
            },
            onDismiss = {
                showReorderSheet = false
            }
        )
    }

    if (showAccountFocusSheet) {
        AccountFocusSheet(
            accounts = accounts,
            focusedAccountId = focusedAccountId,
            onSelectAccount = { accountId ->
                viewModel.setFocusedAccountId(accountId)
                showAccountFocusSheet = false
            },
            onDismiss = {
                showAccountFocusSheet = false
            }
        )
    }
}

@Composable
private fun HomeWelcomeHeader(username: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val today = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (username.isBlank()) "$greeting" else "$greeting, ${username.trim()}.",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$today | Your money at a glance",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
        )
    }
}

@Composable
private fun RecentActivityCard(
    transactions: List<TransactionEntity>,
    accounts: List<com.makemission.makeflow.data.local.entity.AccountEntity>,
    categories: List<com.makemission.makeflow.data.local.entity.CategoryEntity>,
    currencyCode: String,
    onAddTransaction: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    val dimens = LocalAppDimens.current
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Recent activity",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (transactions.isEmpty()) "Your latest entries will appear here" else "Your latest money moves",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            if (transactions.isNotEmpty()) {
                TextButton(onClick = onOpenHistory) {
                    Text("View all")
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(dimens.spacingMedium))

        if (transactions.isEmpty()) {
            Text(
                text = "Keep a simple record of what comes in and goes out. You can add an entry whenever it suits you.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium
            )
            QuickAddActions(onAddTransaction)
        } else {
            transactions.forEachIndexed { index, transaction ->
                val category = categories.find { it.id == transaction.categoryId }
                val account = accounts.find { it.id == transaction.accountId }
                val title = if (transaction.type == "TRANSFER") "Transfer" else category?.name ?: "Uncategorized"
                val accountLabel = when (transaction.type) {
                    "TRANSFER" -> {
                        val from = accounts.find { it.id == transaction.fromAccountId }?.name
                        val to = accounts.find { it.id == transaction.toAccountId }?.name
                        listOfNotNull(from, to).joinToString(" to ").ifBlank { "Transfer" }
                    }
                    else -> account?.name ?: ""
                }
                val amountColor = when (transaction.type) {
                    "INCOME" -> LocalMakeFlowColors.current.colorSuccess
                    "TRANSFER" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
                val amountPrefix = when (transaction.type) {
                    "INCOME" -> "+"
                    "EXPENSE" -> "-"
                    else -> ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
                        .clickable { onEditTransaction(transaction) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val categoryColor = runCatching {
                        Color(android.graphics.Color.parseColor(category?.colorHex ?: "#6C8F72"))
                    }.getOrElse { MaterialTheme.colorScheme.primary }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryColor, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1
                        )
                        val dateLabel = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                            .format(Date(transaction.timestamp))
                        val subtitle = listOf(accountLabel, transaction.note, dateLabel)
                            .filter { it.isNotBlank() }
                            .joinToString(" | ")
                        Text(
                            text = subtitle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "$amountPrefix${CurrencyFormatter.format(transaction.amount, currencyCode)}",
                        color = amountColor,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
                if (index < transactions.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), thickness = 0.5.dp)
                }
            }
            QuickAddActions(onAddTransaction)
        }
    }
}

@Composable
private fun QuickAddActions(onAddTransaction: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { onAddTransaction("EXPENSE") }) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Expense")
        }
        TextButton(onClick = { onAddTransaction("INCOME") }) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Income")
        }
    }
}

@Composable
fun TotalBalanceCard(
    balance: String = "KES 0.00",
    label: String = "Total balance",
    supportingLabel: String = "Across all accounts"
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(dimens.cornerRadiusLarge))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimens.spacingLarge))

        Text(
            text = balance,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(dimens.spacingMedium))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
            )
            Text(
                text = supportingLabel,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun AccountsCard(
    accounts: List<AccountChip> = emptyList(),
    onOpenAccounts: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {}
) {
    val dimens = LocalAppDimens.current
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accounts",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            val chevronInteractionSource = remember { MutableInteractionSource() }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "View all",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .pressScale(interactionSource = chevronInteractionSource)
                    .clickable(
                        interactionSource = chevronInteractionSource,
                        indication = null
                    ) { onOpenAccounts() }
            )
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSavingsGoals() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Savings Goals",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "View goals",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accounts.forEach { account ->
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusMedium))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(account.color, CircleShape)
                    )
                    Column {
                        Text(
                            text = account.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = account.balance,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowCard(
    income: String = "KES 0.00",
    expenses: String = "KES 0.00",
    net: String = "KES 0.00",
    monthLabel: String = "June 2026",
    onTap: (() -> Unit)? = null
) {
    val dimens = LocalAppDimens.current
    Box(
        modifier = if (onTap != null) Modifier.clickable { onTap() } else Modifier
    ) {
        CardContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Cash flow",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = monthLabel,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingXLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(LocalMakeFlowColors.current.colorSuccess.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Income",
                            tint = LocalMakeFlowColors.current.colorSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Income",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = income,
                    color = LocalMakeFlowColors.current.colorSuccess,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = "Expenses",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Expenses",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = expenses,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingLarge))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(dimens.spacingLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total:",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = net,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoriesCard(
    categories: List<CategoryLegend> = emptyList(),
    monthLabel: String = "June 2026"
) {
    val dimens = LocalAppDimens.current
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Categories",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = monthLabel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimens.spacingXLarge))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingXXLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Expense",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = dimens.textS,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(cat.color, CircleShape)
                            )
                            Text(
                                text = cat.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = cat.value,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsCard(
    budgets: List<BudgetEntity> = emptyList(),
    currencyCode: String = "KES",
    hapticManager: HapticManager? = null,
    notificationGate: NotificationGate? = null
) {
    val dimens = LocalAppDimens.current
    val previousBudgetZones = remember { mutableMapOf<String, String>() }
    LaunchedEffect(budgets) {
        budgets.forEach { budget ->
            val progress = (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
            val currentZone = when {
                progress >= 1f -> "red"
                progress >= 0.7f -> "orange"
                else -> "green"
            }
            val prevZone = previousBudgetZones[budget.id]
            if (prevZone != null && prevZone != currentZone) {
                when (currentZone) {
                    "orange" -> hapticManager?.fire(HapticEvent.BUDGET_WARNING)
                    "red" -> {
                        hapticManager?.fire(HapticEvent.BUDGET_EXCEEDED)
                        notificationGate?.dispatchOrQueue(
                            type = "budget_alert",
                            title = "Budget exceeded",
                            body = "${budget.displayName} has exceeded its limit"
                        )
                    }
                }
            }
            previousBudgetZones[budget.id] = currentZone
        }
    }
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budgets",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "View all",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (budgets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No active budgets",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
        } else {
            budgets.take(3).forEach { budget ->
                val progress = (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
                val progressColor = when {
                    progress >= 1f -> MaterialTheme.colorScheme.error
                    progress >= 0.7f -> LocalMakeFlowColors.current.colorWarning
                    else -> LocalMakeFlowColors.current.colorSuccess
                }

                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = budget.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${CurrencyFormatter.format(budget.spentAmount, currencyCode)} / ${CurrencyFormatter.format(budget.amount, currencyCode)}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = dimens.textS
                        )
                    }

                    AnimatedProgressBar(
                        progress = progress,
                        color = progressColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(dimens.spacingXSmall))
    }
}

@Composable
fun RecurringCard(
    patterns: List<RecurringPattern>,
    currencyCode: String
) {
    val dimens = LocalAppDimens.current
    CardContainer {
        Text(
            text = "Upcoming",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(14.dp))
        patterns.take(5).forEach { pattern ->
            val titleCase = pattern.normalizedDescription
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            val relativeDate = getRelativeDateLabel(pattern.nextExpectedDate)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = titleCase,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (pattern.confidence < 0.6f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "estimated",
                                color = LocalMakeFlowColors.current.colorWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    Text(
                        text = CurrencyFormatter.format(pattern.avgAmount, currencyCode),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = relativeDate,
                    color = if (relativeDate == "Tomorrow") LocalMakeFlowColors.current.colorWarning else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (pattern != patterns.take(5).last()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

private fun getRelativeDateLabel(nextExpectedDate: Long): String {
    val now = System.currentTimeMillis()
    val diffDays = ((nextExpectedDate - now) / (1000.0 * 60 * 60 * 24)).roundToInt()
    return when {
        diffDays <= 0 -> "Today"
        diffDays == 1 -> "Tomorrow"
        diffDays <= 7 -> "In $diffDays days"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
            sdf.format(java.util.Date(nextExpectedDate))
        }
    }
}

@Composable
fun PendingRecurringCard(
    items: List<RecurringTransactionEntity>,
    currencyCode: String,
    onConfirm: (RecurringTransactionEntity) -> Unit,
    onSkip: (RecurringTransactionEntity) -> Unit
) {
    val dimens = LocalAppDimens.current
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    CardContainer {
        Text(
            text = "Pending transactions",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        items.take(3).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.description,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${CurrencyFormatter.format(item.amount, currencyCode)} \u00B7 Due ${dateFormat.format(Date(item.nextDueDate))}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = dimens.textS
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
                    Text(
                        text = "Confirm",
                        color = LocalMakeFlowColors.current.colorSuccess,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.shapeCornerXSmall))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onConfirm(item) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "Skip",
                        color = LocalMakeFlowColors.current.colorWarning,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.shapeCornerXSmall))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onSkip(item) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (item != items.take(3).last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun CardContainer(content: @Composable () -> Unit) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(dimens.cornerRadiusLarge))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun VelocityAlertsCard(
    alerts: List<VelocityAlert>,
    currencyCode: String
) {
    val dimens = LocalAppDimens.current
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                contentDescription = null,
                tint = LocalMakeFlowColors.current.colorWarning,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Spending velocity",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(dimens.spacingLarge))
        alerts.forEach { alert ->
            val severityColor = when (alert.severity) {
                com.makemission.makeflow.domain.VelocitySeverity.CRITICAL -> MaterialTheme.colorScheme.error
                com.makemission.makeflow.domain.VelocitySeverity.HIGH -> MaterialTheme.colorScheme.error
                com.makemission.makeflow.domain.VelocitySeverity.ELEVATED -> LocalMakeFlowColors.current.colorWarning
                com.makemission.makeflow.domain.VelocitySeverity.NONE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
            val formattedPct = "${(alert.percentAhead * 100).toInt()}%"
            val projectedFormatted = CurrencyFormatter.format(alert.projectedMonthEndAmount, currencyCode)
            val actualFormatted = CurrencyFormatter.format(alert.actualCumulativeSpend, currencyCode)
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.categoryName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$formattedPct ahead",
                        color = severityColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$actualFormatted spent \u00B7 at this pace: $projectedFormatted by month end",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = dimens.textS
                )
            }
            if (alert != alerts.last()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ProjectionCard(
    projection: CashFlowProjection,
    currencyCode: String
) {
    val dimens = LocalAppDimens.current
    val dateFormat = remember { SimpleDateFormat("d MMMM", Locale.getDefault()) }
    val endOfMonth = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, projection.daysRemainingInMonth + java.time.LocalDate.now().dayOfMonth)
        dateFormat.format(cal.time)
    }
    val isNegative = projection.projectedEndOfMonthBalance < 0
    val formattedProjected = CurrencyFormatter.format(projection.projectedEndOfMonthBalance, currencyCode)

    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isNegative) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Projected balance",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = dimens.textS,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(dimens.spacingXSmall))
                Text(
                    text = "At this pace, you'll have $formattedProjected by $endOfMonth",
                    color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = if (isNegative) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun AccountFocusSelector(
    focusedAccountName: String?,
    onClick: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = focusedAccountName ?: "All Accounts",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "Change account",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AccountFocusSheet(
    accounts: List<com.makemission.makeflow.data.local.entity.AccountEntity>,
    focusedAccountId: String?,
    onSelectAccount: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val dimens = LocalAppDimens.current
    AdaptiveSheetDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Focus Account",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(dimens.spacingXLarge))

            val allAccountsItem = @Composable {
                val isSelected = focusedAccountId == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.shapeCornerSmall))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onSelectAccount(null) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Accounts",
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            allAccountsItem()

            Spacer(Modifier.height(dimens.spacingMedium))

            accounts.forEach { account ->
                val isSelected = focusedAccountId == account.id
                val accountColor = try {
                    Color(android.graphics.Color.parseColor(account.colorHex))
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.shapeCornerSmall))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onSelectAccount(account.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(accountColor, CircleShape)
                        )
                        Text(
                            text = account.name,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                if (account != accounts.last()) {
                    Spacer(Modifier.height(dimens.spacingXSmall))
                }
            }

            Spacer(Modifier.height(dimens.spacingMedium))
        }
    }
}
