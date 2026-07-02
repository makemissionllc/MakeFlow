package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.domain.CashFlowProjection
import com.makemission.makeflow.domain.DashboardCardType
import com.makemission.makeflow.ui.components.AnimatedProgressBar
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.ui.viewmodel.HomeDashboardViewModel
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onOpenAccounts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    hapticManager: HapticManager? = null
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val parentCategorySpending by viewModel.parentCategorySpending.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val recurringPatterns by viewModel.recurringPatterns.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val cardOrder by viewModel.cardOrder.collectAsState()
    val cashFlowProjection by viewModel.cashFlowProjection.collectAsState()

    val listState = rememberLazyListState()
    val currentMonthLabel = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }

    val config = LocalAdaptiveConfig.current
    val formattedBalance = "$currencySymbol ${String.format("%,.2f", totalBalance)}"
    val formattedIncome = "$currencySymbol ${String.format("%,.2f", monthlyIncome)}"
    val formattedExpenses = "$currencySymbol ${String.format("%,.2f", monthlyExpenses)}"
    val formattedNet = "$currencySymbol ${String.format("%,.2f", monthlyIncome - monthlyExpenses)}"

    val accountChips = accounts.map { entity ->
        val color = try {
            Color(android.graphics.Color.parseColor(entity.colorHex))
        } catch (_: Exception) {
            Color(0xFF007AFF)
        }
        AccountChip(entity.name, "$currencySymbol ${String.format("%,.2f", entity.balance)}", color)
    }

    val categoryLegends = parentCategorySpending.map { pcs ->
        val color = try {
            Color(android.graphics.Color.parseColor(pcs.color))
        } catch (_: Exception) {
            Color(0xFFFF3B30)
        }
        CategoryLegend(pcs.categoryName, "$currencySymbol ${String.format("%,.2f", pcs.amount)}", color)
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
                TotalBalanceCard(balance = formattedBalance, monthLabel = currentMonthLabel)
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
                                }
                            )
                        }
                    }
                    DashboardCardType.BUDGET_PROGRESS -> if (budgets.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.animateItem().clickable {
                                    viewModel.logCardInteraction(DashboardCardType.BUDGET_PROGRESS)
                                }
                            ) {
                                BudgetsCard(budgets = budgets, currencySymbol = currencySymbol, hapticManager = hapticManager)
                            }
                        }
                    }
                    DashboardCardType.RECURRING -> if (recurringPatterns.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.animateItem().clickable {
                                    viewModel.logCardInteraction(DashboardCardType.RECURRING)
                                }
                            ) {
                                RecurringCard(patterns = recurringPatterns, currencySymbol = currencySymbol)
                            }
                        }
                    }
                    DashboardCardType.QUICK_ADD -> { }
                }
            }
            item {
                CategoriesCard(categories = categoryLegends.take(5), monthLabel = currentMonthLabel)
            }
            cashFlowProjection?.let { projection ->
                item {
                    ProjectionCard(projection = projection, currencySymbol = currencySymbol)
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Edit cards",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
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
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Local storage",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
    }
}

@Composable
fun TotalBalanceCard(
    balance: String = "KES 0.00",
    monthLabel: String = "June 2026"
) {
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total balance",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "Filter",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = balance,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFF007AFF), RoundedCornerShape(2.dp))
            )
            Text(
                text = "$balance $monthLabel",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun AccountsCard(
    accounts: List<AccountChip> = listOf(
        AccountChip("Cash", "KES 0.00", Color(0xFF34C759)),
        AccountChip("Bank account", "KES 0.00", Color(0xFF007AFF))
    ),
    onOpenAccounts: () -> Unit = {}
) {
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accounts",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            val chevronInteractionSource = remember { MutableInteractionSource() }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "View all",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .pressScale(interactionSource = chevronInteractionSource)
                    .clickable(
                        interactionSource = chevronInteractionSource,
                        indication = null
                    ) { onOpenAccounts() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(accounts) { account ->
                Row(
                    modifier = Modifier
                        .background(Color(0xFF121315), RoundedCornerShape(14.dp))
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
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = account.balance,
                            color = Color.White.copy(alpha = 0.5f),
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
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = monthLabel,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF34C759).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Income",
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Income",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Text(
                text = income,
                color = Color(0xFF34C759),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFFFF3B30).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = "Expenses",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Expenses",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Text(
                text = expenses,
                color = Color(0xFFFF3B30),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            thickness = 0.5.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = net,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    }
}

@Composable
fun CategoriesCard(
    categories: List<CategoryLegend> = listOf(
        CategoryLegend("Food &", "KES 0.00", Color(0xFFFF3B30)),
        CategoryLegend("Shopping", "KES 0.00", Color(0xFFFF2D55)),
        CategoryLegend("Housing", "KES 0.00", Color(0xFFAF52DE)),
        CategoryLegend("Bills", "KES 0.00", Color(0xFF34C759))
    ),
    monthLabel: String = "June 2026"
) {
    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Categories",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = monthLabel,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF121315), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Expense",
                        color = Color(0xFFFF3B30),
                        fontSize = 12.sp,
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(cat.color, CircleShape)
                            )
                            Text(
                                text = cat.name,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = cat.value,
                            color = Color(0xFFFF3B30),
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
    currencySymbol: String = "KES",
    hapticManager: HapticManager? = null
) {
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
                    "red" -> hapticManager?.fire(HapticEvent.BUDGET_EXCEEDED)
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
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "View all",
                tint = Color.White.copy(alpha = 0.5f),
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
                        .background(Color(0xFF121315), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No active budgets",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
        } else {
            budgets.take(3).forEach { budget ->
                val progress = (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
                val progressColor = when {
                    progress >= 1f -> Color(0xFFEF5350)
                    progress >= 0.7f -> Color(0xFFFFA726)
                    else -> Color(0xFF4CAF50)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = budget.categoryName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$currencySymbol ${String.format("%,.2f", budget.spentAmount)} / $currencySymbol ${String.format("%,.2f", budget.amount)}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }

                    AnimatedProgressBar(
                        progress = progress,
                        color = progressColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF2C2C2C), RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun RecurringCard(
    patterns: List<RecurringPattern>,
    currencySymbol: String
) {
    CardContainer {
        Text(
            text = "Upcoming",
            color = Color.White,
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
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (pattern.confidence < 0.6f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "estimated",
                                color = Color(0xFFFFA726),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    Text(
                        text = "$currencySymbol ${String.format("%,.2f", pattern.avgAmount)}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = relativeDate,
                    color = if (relativeDate == "Tomorrow") Color(0xFFFFA726) else Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (pattern != patterns.take(5).last()) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
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
        diffDays <= 14 -> "In $diffDays days"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
            sdf.format(java.util.Date(nextExpectedDate))
        }
    }
}

@Composable
fun CardContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun ProjectionCard(
    projection: CashFlowProjection,
    currencySymbol: String
) {
    val dateFormat = remember { SimpleDateFormat("d MMMM", Locale.getDefault()) }
    val endOfMonth = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, projection.daysRemainingInMonth + java.time.LocalDate.now().dayOfMonth)
        dateFormat.format(cal.time)
    }
    val isNegative = projection.projectedEndOfMonthBalance < 0
    val formattedProjected = "$currencySymbol ${String.format("%,.2f", projection.projectedEndOfMonthBalance)}"

    CardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isNegative) {
                Icon(
                    imageVector = Icons.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Projected balance",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "At this pace, you'll have $formattedProjected by $endOfMonth",
                    color = if (isNegative) Color(0xFFEF5350) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isNegative) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
