package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.notifications.NotificationGate
import com.makemission.makeflow.ui.components.AnimatedProgressBar
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.PrimaryDockContentInset
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.ui.viewmodel.FocusedAccountState
import com.makemission.makeflow.ui.viewmodel.CategorySpendingItem
import com.makemission.makeflow.ui.viewmodel.MonthlyBarItem
import com.makemission.makeflow.domain.FrugalStreak
import com.makemission.makeflow.domain.PatternType
import com.makemission.makeflow.domain.SeasonalPattern
import com.makemission.makeflow.ui.viewmodel.InsightsViewModel
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.core.format.CurrencyFormatter

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onOpenBudgets: () -> Unit = {},
    onSetUpRecurring: (Double) -> Unit = {},
    onExportPdf: () -> Unit = {},
    hapticManager: HapticManager? = null,
    notificationGate: NotificationGate? = null
) {
    val dimens = LocalAppDimens.current
    // AnomalyDetector is intentionally save-time-only per original spec
    // (in-the-moment callout in AddTransactionScreen via MainScreen).
    // Insights is a retrospective monthly summary and does not need it.
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val monthlyBarData by viewModel.monthlyBarData.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val projectedMonthlyBalance by viewModel.projectedMonthlyNet.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val momItems by viewModel.momItems.collectAsState()
    val budgetForecasts by viewModel.budgetForecasts.collectAsState()
    val dailyAverageSpends by viewModel.dailyAverageSpends.collectAsState()
    val savingsSuggestion by viewModel.savingsSuggestion.collectAsState()
    val frugalStreaks by viewModel.frugalStreaks.collectAsState()
    val upcomingSeasonalExpenses by viewModel.upcomingSeasonalExpenses.collectAsState()
    val monthlyFinanceReview by viewModel.monthlyFinanceReview.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val insightsFocusedState by viewModel.focusedAccountState.collectAsState()
    val insightsFocusedReady by viewModel.focusedAccountReady.collectAsState()
    var showMonthlyReview by remember { mutableStateOf(false) }

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

    val config = LocalAdaptiveConfig.current
    val listState = rememberLazyListState()
    val budgetWarning = budgetForecasts.firstOrNull { it.projectedOverspend != null }
    val longestFrugalStreak = frugalStreaks.maxOfOrNull { it.streakLengthMonths } ?: 0

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalFadingEdge(
                topFadeHeight = config.fadingEdgeHeight,
                bottomFadeHeight = config.fadingEdgeHeight
            ),
        contentPadding = PaddingValues(
            start = config.horizontalPadding, end = config.horizontalPadding,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
            bottom = PrimaryDockContentInset()
        ),
        verticalArrangement = Arrangement.spacedBy(config.cardSpacing)
    ) {
        item {
            LargeTitleItem(title = "Insights", scrollState = listState)
        }
        item {
            MonthlySpendingChart(monthlyBarData)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                Text(
                    text = "Forecast",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium
                )
                val forecastMessage = if (projectedMonthlyBalance >= 0.0) {
                    "You should end the month with ${CurrencyFormatter.format(projectedMonthlyBalance, selectedCurrency)} left"
                } else {
                    "You may finish ${CurrencyFormatter.format(-projectedMonthlyBalance, selectedCurrency)} below your income"
                }
                Text(
                    text = forecastMessage,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                InsightMetricCard(
                    title = "Frugal streak",
                    value = if (longestFrugalStreak > 0) "$longestFrugalStreak months" else "Start one",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "You can save",
                    value = savingsSuggestion?.let {
                        CurrencyFormatter.format(it.suggestedAmount, selectedCurrency)
                    } ?: "No estimate yet",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (budgetWarning != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            LocalMakeFlowColors.current.colorWarning.copy(alpha = 0.22f),
                            RoundedCornerShape(dimens.cornerRadiusLarge)
                        )
                        .padding(dimens.paddingMedium)
                ) {
                    Text(
                        text = "Heads up: ${budgetWarning.categoryName} is on pace to exceed its monthly budget.",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    hapticManager?.fire(HapticEvent.NAV_SELECT)
                    onExportPdf()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimens.cornerRadiusXL),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Export PDF statement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
        item {
            if (insightsFocusedReady) {
                Text(
                    text = "Account: ${
                        when (val state = insightsFocusedState) {
                            is FocusedAccountState.Loading -> ""
                            is FocusedAccountState.ShowAll -> "All Accounts"
                            is FocusedAccountState.Focused -> accounts.find { it.id == state.accountId }?.name ?: "All Accounts"
                        }
                    }",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = dimens.textS,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        item {
            StaggeredItem(index = 1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = selectedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + " " + selectedMonth.year,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = dimens.paddingMedium)
                    )
                    IconButton(
                        onClick = { viewModel.nextMonth() },
                        enabled = selectedMonth.isBefore(java.time.YearMonth.now())
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Next month",
                            tint = if (selectedMonth.isBefore(java.time.YearMonth.now())) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
                TextButton(
                    onClick = {
                        hapticManager?.fire(HapticEvent.NAV_SELECT)
                        showMonthlyReview = true
                    }
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(dimens.spacingSmall))
                    Text("Open monthly review")
                }
            }
            }
        }

        item {
            StaggeredItem(index = 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalMakeFlowColors.current.colorSuccess.copy(alpha = 0.15f), RoundedCornerShape(dimens.shapeCornerMedium))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = CurrencyFormatter.format(totalIncome, selectedCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalMakeFlowColors.current.colorSuccess
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(dimens.shapeCornerMedium))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = CurrencyFormatter.format(totalExpenses, selectedCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            }
        }

        item {
            StaggeredItem(index = 3) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusLarge))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                Text("vs previous month", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textL, fontWeight = FontWeight.Medium)
                if (momItems.isEmpty()) {
                    Text("No data for comparison", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textM)
                } else {
                    momItems.forEach { item ->
                        val (changeText, changeColor) = if (item.percentChange != null) {
                            val color = when {
                                item.label == "Income" && item.percentChange >= 0 -> LocalMakeFlowColors.current.colorSuccess
                                item.label == "Income" -> MaterialTheme.colorScheme.error
                                item.label == "Expenses" && item.percentChange <= 0 -> LocalMakeFlowColors.current.colorSuccess
                                item.label == "Expenses" -> MaterialTheme.colorScheme.error
                                item.percentChange >= 0 -> MaterialTheme.colorScheme.error
                                else -> LocalMakeFlowColors.current.colorSuccess
                            }
                            val arrow = if (item.percentChange >= 0) "\u25B2" else "\u25BC"
                            Pair("$arrow ${String.format("%+.1f", item.percentChange)}%", color)
                        } else {
                            Pair("New", LocalMakeFlowColors.current.colorWarning)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = changeText,
                                    color = changeColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = dimens.textM
                                )
                                Spacer(Modifier.width(dimens.spacingMedium))
                                Text(
                                    text = "vs ${String.format("%.0f", item.priorAmount)}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = dimens.textS
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        item {
            StaggeredItem(index = 5) {
            val displayItems = if (categorySpending.size > 5) {
                val top4 = categorySpending.take(4)
                val others = categorySpending.drop(4)
                val othersAmount = others.sumOf { it.amount }
                val othersPercent = others.sumOf { it.percentage.toDouble() }.toFloat()
                top4 + CategorySpendingItem("others", "Others", "#9E9E9E", othersAmount, othersPercent)
            } else {
                categorySpending
            }

            if (config.layoutMode != DeviceLayoutMode.COMPACT && config.isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(config.cardSpacing)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusLarge))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
                    ) {
                        Text(
                            text = "Spending by category",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = dimens.textL,
                            fontWeight = FontWeight.Medium
                        )
                        val emptyArcColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        val fallbackArcColor = MaterialTheme.colorScheme.outline
                        Box(
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (categorySpending.isEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val outerR = 90.dp.toPx()
                                    val innerR = 60.dp.toPx()
                                    val strokeW = outerR - innerR
                                    drawArc(color=emptyArcColor, startAngle=-90f, sweepAngle=360f, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW))
                                }
                                Text("No expenses", color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.4f), fontSize=dimens.textM)
                            } else {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val outerR = 90.dp.toPx()
                                    val innerR = 60.dp.toPx()
                                    val strokeW = outerR - innerR
                                    var startAngle = -90f
                                    val total = categorySpending.sumOf { it.amount }
                                    if (total > 0) {
                                        categorySpending.forEach { item ->
                                            val sweep = (item.percentage / 100f * 360f).coerceAtLeast(0f)
                                            val gapAngle = if (sweep > 0f) 2f else 0f
                                            val drawSweep = (sweep - gapAngle).coerceAtLeast(0f)
                                            val arcColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { fallbackArcColor }
                                            if (drawSweep > 0f) {
                                                drawArc(color=arcColor, startAngle=startAngle, sweepAngle=drawSweep, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW,cap=StrokeCap.Butt))
                                                startAngle += sweep
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text(CurrencyFormatter.format(totalExpenses, selectedCurrency), color=MaterialTheme.colorScheme.onSurface, fontSize=dimens.textXl, fontWeight=FontWeight.Bold)
                                    Text("Total", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusLarge))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Breakdown",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = dimens.textL,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(dimens.spacingLarge))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayItems.forEach { item ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                                    val dotColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { MaterialTheme.colorScheme.outline }
                                    Box(Modifier.size(10.dp).background(dotColor, CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Text(item.categoryName, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurface, modifier=Modifier.weight(1f))
                                    val dailyAvg = dailyAverageSpends.find { it.categoryName == item.categoryName }?.dailyAverage ?: 0.0
                                    Text(
                                        text = "${CurrencyFormatter.format(dailyAvg, selectedCurrency)}/day",
                                        style=MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        textAlign=TextAlign.End
                                    )
                                    Spacer(Modifier.width(dimens.spacingMedium))
                                    Text(CurrencyFormatter.format(item.amount, selectedCurrency), style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.End)
                                    Spacer(Modifier.width(dimens.spacingMedium))
                                    Text("${String.format("%.1f", item.percentage)}%", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f), textAlign=TextAlign.End)
                                }
                            }
                        }
                    }
                }
            } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusLarge))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                Text(
                    text = "Spending by category",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = dimens.textL,
                    fontWeight = FontWeight.Medium
                )

                val emptyArcColor2 = MaterialTheme.colorScheme.surfaceContainerHigh
                val fallbackArcColor2 = MaterialTheme.colorScheme.outline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (categorySpending.isEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val outerR = 80.dp.toPx()
                            val innerR = 52.dp.toPx()
                            val strokeW = outerR - innerR
                            drawArc(color=emptyArcColor2, startAngle=-90f, sweepAngle=360f, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW))
                        }
                        Text("No expenses", color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.4f), fontSize=dimens.textM)
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val outerR = 80.dp.toPx()
                            val innerR = 52.dp.toPx()
                            val strokeW = outerR - innerR
                            var startAngle = -90f
                            val total = categorySpending.sumOf { it.amount }
                            if (total > 0) {
                                categorySpending.forEach { item ->
                                    val sweep = (item.percentage / 100f * 360f).coerceAtLeast(0f)
                                    val gapAngle = if (sweep > 0f) 2f else 0f
                                    val drawSweep = (sweep - gapAngle).coerceAtLeast(0f)
                                    val arcColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { fallbackArcColor2 }
                                    if (drawSweep > 0f) {
                                        drawArc(color=arcColor, startAngle=startAngle, sweepAngle=drawSweep, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW,cap=StrokeCap.Butt))
                                        startAngle += sweep
                                    }
                                }
                            }
                        }
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(CurrencyFormatter.format(totalExpenses, selectedCurrency), color=MaterialTheme.colorScheme.onSurface, fontSize=dimens.textXl, fontWeight=FontWeight.Bold)
                            Text("Total", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                    displayItems.forEach { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                            val dotColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { MaterialTheme.colorScheme.outline }
                            Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                            Spacer(Modifier.width(dimens.spacingMedium))
                            Text(item.categoryName, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurface, modifier=Modifier.weight(1f))
                            val dailyAvg = dailyAverageSpends.find { it.categoryName == item.categoryName }?.dailyAverage ?: 0.0
                            Text(
                                text = "${CurrencyFormatter.format(dailyAvg, selectedCurrency)}/day",
                                style=MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign=TextAlign.End
                            )
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text(CurrencyFormatter.format(item.amount, selectedCurrency), style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.End)
                            Spacer(Modifier.width(dimens.spacingSmall))
                            Text("${String.format("%.1f", item.percentage)}%", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f), textAlign=TextAlign.End)
                        }
                    }
                }
            }
            }
            }
        }

        savingsSuggestion?.let { suggestion ->
            item {
            StaggeredItem(index = 7) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusLarge))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                Text(
                    text = "Savings Capacity",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = dimens.textL,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "You could save ${CurrencyFormatter.format(suggestion.suggestedAmount, selectedCurrency)} per month",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = dimens.textM
                )
                Text(
                    text = "Based on your lowest surplus month (${CurrencyFormatter.format(suggestion.safeFloor, selectedCurrency)})",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = dimens.textS
                )
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                    val confidencePercent = (suggestion.confidence * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Confidence",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = dimens.textS
                        )
                        Text(
                            text = "$confidencePercent%",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = dimens.textS
                        )
                    }
                    val confidenceColor = when {
                        suggestion.confidence >= 0.7f -> LocalMakeFlowColors.current.colorSuccess
                        suggestion.confidence >= 0.4f -> LocalMakeFlowColors.current.colorWarning
                        else -> MaterialTheme.colorScheme.error
                    }
                    AnimatedProgressBar(
                        progress = suggestion.confidence.coerceIn(0f, 1f),
                        color = confidenceColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(3.dp))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
                ) {
                    TextButton(onClick = { viewModel.dismissSavingsSuggestion() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Button(
                        onClick = { onSetUpRecurring(suggestion.suggestedAmount) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(dimens.shapeCornerSmall)
                    ) {
                        Text("Set up recurring transfer")
                    }
                }
            }
            }
            }
        }

        if (frugalStreaks.isNotEmpty()) {
            item {
            StaggeredItem(index = 8) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalMakeFlowColors.current.colorSuccess.copy(alpha = 0.15f), RoundedCornerShape(dimens.cornerRadiusLarge))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frugality Streaks",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = dimens.textL,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "2 months and counting",
                        color = LocalMakeFlowColors.current.colorSuccess,
                        fontSize = dimens.textS,
                        fontWeight = FontWeight.Medium
                    )
                }
                frugalStreaks.forEach { streak ->
                    FrugalStreakRow(
                        streak = streak,
                        currencySymbol = currencySymbol,
                        selectedCurrency = selectedCurrency,
                        onDismiss = { viewModel.dismissFrugalStreak(streak.categoryId) }
                    )
                }
            }
            }
            }
        }

        if (upcomingSeasonalExpenses.isNotEmpty()) {
            item {
            StaggeredItem(index = 9) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalMakeFlowColors.current.colorWarning.copy(alpha = 0.15f), RoundedCornerShape(dimens.cornerRadiusLarge))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                Text(
                    text = "Upcoming seasonal expenses",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = dimens.textL,
                    fontWeight = FontWeight.Medium
                )
                upcomingSeasonalExpenses.forEach { pattern ->
                    SeasonalPatternRow(
                        pattern = pattern,
                        currencySymbol = currencySymbol,
                        selectedCurrency = selectedCurrency
                    )
                }
            }
            }
            }
        }

        item {
            StaggeredItem(index = 6) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusLarge))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budgets",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = dimens.textL,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onOpenBudgets) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "View budgets",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                if (budgets.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBudgets() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "No active budgets",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = dimens.textM
                        )
                        Text(
                            text = "Tap + to set a spending limit",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = dimens.textM
                        )
                    }
                } else {
                    if (config.layoutMode != DeviceLayoutMode.COMPACT && config.isLandscape) {
                        val chunked = budgets.chunked(2)
                        chunked.forEachIndexed { _, chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(config.cardSpacing)
                            ) {
                                chunk.forEach { budget ->
                                    Box(modifier = Modifier.weight(1f)) {
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
                                                Text(budget.displayName, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurface)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val forecast = budgetForecasts.find { it.budgetId == budget.id }
                                                    if (forecast != null) {
                                                        val (statusColor, statusText) = when (forecast.status) {
                                                            com.makemission.makeflow.domain.BudgetForecastStatus.ON_TRACK -> LocalMakeFlowColors.current.colorSuccess to "On track"
                                                            com.makemission.makeflow.domain.BudgetForecastStatus.OFF_TRACK -> LocalMakeFlowColors.current.colorWarning to "Off track"
                                                            com.makemission.makeflow.domain.BudgetForecastStatus.OVERSPENT -> MaterialTheme.colorScheme.error to "Overspent"
                                                        }
                                                        Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                        Spacer(Modifier.width(dimens.spacingSmall))
                                                    }
                                                    Text("${CurrencyFormatter.format(budget.spentAmount, selectedCurrency)} / ${CurrencyFormatter.format(budget.amount, selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                                }
                                            }
                                            AnimatedProgressBar(progress=progress, color=progressColor, modifier=Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(3.dp)))
                                        }
                                        Spacer(modifier = Modifier.height(dimens.spacingLarge))
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        budgets.forEach { budget ->
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
                                    Text(budget.displayName, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurface)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val forecast = budgetForecasts.find { it.budgetId == budget.id }
                                        if (forecast != null) {
                                            val (statusColor, statusText) = when (forecast.status) {
                                                com.makemission.makeflow.domain.BudgetForecastStatus.ON_TRACK -> LocalMakeFlowColors.current.colorSuccess to "On track"
                                                com.makemission.makeflow.domain.BudgetForecastStatus.OFF_TRACK -> LocalMakeFlowColors.current.colorWarning to "Off track"
                                                com.makemission.makeflow.domain.BudgetForecastStatus.OVERSPENT -> MaterialTheme.colorScheme.error to "Overspent"
                                            }
                                            Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Spacer(Modifier.width(dimens.spacingSmall))
                                        }
                                        Text("${CurrencyFormatter.format(budget.spentAmount, selectedCurrency)} / ${CurrencyFormatter.format(budget.amount, selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                    }
                                }

                                AnimatedProgressBar(progress=progress, color=progressColor, modifier=Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(3.dp)))
                            }

                            Spacer(modifier = Modifier.height(dimens.spacingLarge))
                        }
                    }
                }
            }
            }
        }
    }

        CollapsingTopBar(
            title = "Insights",
            scrollState = listState,
            modifier = Modifier.align(Alignment.TopStart)
        )
        if (showMonthlyReview) {
            MonthlyFinanceReviewSheet(
                review = monthlyFinanceReview,
                currencyCode = selectedCurrency,
                onDismiss = { showMonthlyReview = false },
                hapticManager = hapticManager
            )
        }
    }
}

@Composable
private fun MonthlySpendingChart(months: List<MonthlyBarItem>) {
    val dimens = LocalAppDimens.current
    val maxSpending = months.maxOfOrNull { it.expense } ?: 0.0
    val chartHeight = dimens.cardMinHeight * 2

    if (maxSpending <= 0.0) {
        Text(
            text = "No spending data yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = dimens.spacingLarge)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall),
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEachIndexed { index, month ->
                val fraction = (month.expense / maxSpending).toFloat().coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(chartHeight * fraction.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(dimens.cornerRadiusSmall))
                            .background(
                                if (index == months.lastIndex) LocalMakeFlowColors.current.colorSuccess
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
        ) {
            months.forEachIndexed { index, month ->
                Text(
                    text = month.label,
                    modifier = Modifier.weight(1f),
                    color = if (index == months.lastIndex) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InsightMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(dimens.cornerRadiusLarge)
            )
            .padding(dimens.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Clip
        )
    }
}

@Composable
private fun FrugalStreakRow(
    streak: FrugalStreak,
    currencySymbol: String,
    selectedCurrency: String,
    onDismiss: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = streak.categoryName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = dimens.textM,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Streak!",
                color = LocalMakeFlowColors.current.colorSuccess,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Saved ${CurrencyFormatter.format(streak.totalSavedVsAverage, selectedCurrency)} vs your average",
            color = LocalMakeFlowColors.current.colorSuccess,
            fontSize = 13.sp
        )
        Text(
            text = "Projected ${CurrencyFormatter.format(streak.projectedAnnualSavings, selectedCurrency)}/year",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = dimens.textS
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Dismiss", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textS)
        }
    }
}

@Composable
private fun SeasonalPatternRow(
    pattern: SeasonalPattern,
    currencySymbol: String,
    selectedCurrency: String
) {
    val dimens = LocalAppDimens.current
    val monthNames = pattern.peakMonths.map { month ->
        java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
    }
    val typeLabel = when (pattern.patternType) {
        PatternType.ANNUAL_BILL -> "Annual bill"
        PatternType.SEASONAL_SPIKE -> "Seasonal spike"
    }
    val typeBadgeColor = when (pattern.patternType) {
        PatternType.ANNUAL_BILL -> LocalMakeFlowColors.current.colorWarning
        PatternType.SEASONAL_SPIKE -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pattern.categoryName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = dimens.textM,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = typeLabel,
                color = typeBadgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = monthNames.joinToString(", "),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = dimens.textS
        )
        Text(
            text = "Expect ~${CurrencyFormatter.format(pattern.avgPeakAmount, selectedCurrency)}",
            color = LocalMakeFlowColors.current.colorWarning.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }
}
