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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.ui.viewmodel.CategorySpendingItem
import com.makemission.makeflow.domain.FrugalStreak
import com.makemission.makeflow.domain.PatternType
import com.makemission.makeflow.domain.SeasonalPattern
import com.makemission.makeflow.ui.viewmodel.InsightsViewModel
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onOpenBudgets: () -> Unit = {},
    onSetUpRecurring: (Double) -> Unit = {},
    hapticManager: HapticManager? = null,
    notificationGate: NotificationGate? = null
) {
    // AnomalyDetector is intentionally save-time-only per original spec
    // (in-the-moment callout in AddTransactionScreen via MainScreen).
    // Insights is a retrospective monthly summary and does not need it.
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val monthlyBarData by viewModel.monthlyBarData.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val momItems by viewModel.momItems.collectAsState()
    val budgetForecasts by viewModel.budgetForecasts.collectAsState()
    val dailyAverageSpends by viewModel.dailyAverageSpends.collectAsState()
    val savingsSuggestion by viewModel.savingsSuggestion.collectAsState()
    val frugalStreaks by viewModel.frugalStreaks.collectAsState()
    val upcomingSeasonalExpenses by viewModel.upcomingSeasonalExpenses.collectAsState()

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

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
            .verticalFadingEdge(
                topFadeHeight = config.fadingEdgeHeight,
                bottomFadeHeight = config.fadingEdgeHeight
            ),
        contentPadding = PaddingValues(
            start = config.horizontalPadding, end = config.horizontalPadding,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(config.cardSpacing)
    ) {
        item {
            LargeTitleItem(title = "Insights", scrollState = listState)
        }
        item {
            StaggeredItem(index = 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = Color.White
                    )
                }
                Text(
                    text = selectedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + " " + selectedMonth.year,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                IconButton(
                    onClick = { viewModel.nextMonth() },
                    enabled = selectedMonth.isBefore(java.time.YearMonth.now())
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next month",
                        tint = if (selectedMonth.isBefore(java.time.YearMonth.now())) Color.White else Color.White.copy(alpha = 0.2f)
                    )
                }
            }
            }
        }

        item {
            StaggeredItem(index = 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1B3A2A), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = CurrencyFormatter.format(totalIncome, selectedCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF3A1B1B), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = CurrencyFormatter.format(totalExpenses, selectedCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFEF5350)
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
                    .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("vs previous month", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (momItems.isEmpty()) {
                    Text("No data for comparison", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                } else {
                    momItems.forEach { item ->
                        val (changeText, changeColor) = if (item.percentChange != null) {
                            val color = when {
                                item.label == "Income" && item.percentChange >= 0 -> Color(0xFF4CAF50)
                                item.label == "Income" -> Color(0xFFEF5350)
                                item.label == "Expenses" && item.percentChange <= 0 -> Color(0xFF4CAF50)
                                item.label == "Expenses" -> Color(0xFFEF5350)
                                item.percentChange >= 0 -> Color(0xFFEF5350)
                                else -> Color(0xFF4CAF50)
                            }
                            val arrow = if (item.percentChange >= 0) "\u25B2" else "\u25BC"
                            Pair("$arrow ${String.format("%+.1f", item.percentChange)}%", color)
                        } else {
                            Pair("New", Color(0xFFFFA726))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = changeText,
                                    color = changeColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "vs ${String.format("%.0f", item.priorAmount)}",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp
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
                            .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Spending by category",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (categorySpending.isEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val outerR = 90.dp.toPx()
                                    val innerR = 60.dp.toPx()
                                    val strokeW = outerR - innerR
                                    drawArc(color=Color(0xFF2C2C2C), startAngle=-90f, sweepAngle=360f, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW))
                                }
                                Text("No expenses", color=Color.White.copy(alpha=0.4f), fontSize=14.sp)
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
                                            val arcColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { Color(0xFF9E9E9E) }
                                            if (drawSweep > 0f) {
                                                drawArc(color=arcColor, startAngle=startAngle, sweepAngle=drawSweep, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW,cap=StrokeCap.Butt))
                                                startAngle += sweep
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text(CurrencyFormatter.format(totalExpenses, selectedCurrency), color=Color.White, fontSize=18.sp, fontWeight=FontWeight.Bold)
                                    Text("Total", style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.5f))
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Breakdown",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayItems.forEach { item ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                                    val dotColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { Color(0xFF9E9E9E) }
                                    Box(Modifier.size(10.dp).background(dotColor, CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Text(item.categoryName, style=MaterialTheme.typography.bodyMedium, color=Color.White, modifier=Modifier.weight(1f))
                                    val dailyAvg = dailyAverageSpends.find { it.categoryName == item.categoryName }?.dailyAverage ?: 0.0
                                    Text(
                                        text = "${CurrencyFormatter.format(dailyAvg, selectedCurrency)}/day",
                                        style=MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                        textAlign=TextAlign.End
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(CurrencyFormatter.format(item.amount, selectedCurrency), style=MaterialTheme.typography.bodyMedium, color=Color.White, textAlign=TextAlign.End)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${String.format("%.1f", item.percentage)}%", style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.5f), textAlign=TextAlign.End)
                                }
                            }
                        }
                    }
                }
            } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Spending by category",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

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
                            drawArc(color=Color(0xFF2C2C2C), startAngle=-90f, sweepAngle=360f, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW))
                        }
                        Text("No expenses", color=Color.White.copy(alpha=0.4f), fontSize=14.sp)
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
                                    val arcColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { Color(0xFF9E9E9E) }
                                    if (drawSweep > 0f) {
                                        drawArc(color=arcColor, startAngle=startAngle, sweepAngle=drawSweep, useCenter=false, topLeft=Offset((size.width-outerR*2)/2f,(size.height-outerR*2)/2f), size=Size(outerR*2,outerR*2), style=Stroke(width=strokeW,cap=StrokeCap.Butt))
                                        startAngle += sweep
                                    }
                                }
                            }
                        }
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(CurrencyFormatter.format(totalExpenses, selectedCurrency), color=Color.White, fontSize=18.sp, fontWeight=FontWeight.Bold)
                            Text("Total", style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.5f))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayItems.forEach { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                            val dotColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (_: Exception) { Color(0xFF9E9E9E) }
                            Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(item.categoryName, style=MaterialTheme.typography.bodyMedium, color=Color.White, modifier=Modifier.weight(1f))
                            val dailyAvg = dailyAverageSpends.find { it.categoryName == item.categoryName }?.dailyAverage ?: 0.0
                            Text(
                                text = "${CurrencyFormatter.format(dailyAvg, selectedCurrency)}/day",
                                style=MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign=TextAlign.End
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(CurrencyFormatter.format(item.amount, selectedCurrency), style=MaterialTheme.typography.bodyMedium, color=Color.White, textAlign=TextAlign.End)
                            Spacer(Modifier.width(6.dp))
                            Text("${String.format("%.1f", item.percentage)}%", style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.5f), textAlign=TextAlign.End)
                        }
                    }
                }
            }
            }
            }
        }

        item {
            StaggeredItem(index = 4) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cash flow — last 6 months",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                val allZeros = monthlyBarData.all { it.income == 0.0 && it.expense == 0.0 }

                if (monthlyBarData.isEmpty() || allZeros) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (config.isTablet && config.isLandscape) 260.dp else 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data yet",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val columnProvider = remember {
                        ColumnCartesianLayer.ColumnProvider.series(
                            LineComponent(color = 0xFF4CAF50.toInt()),
                            LineComponent(color = 0xFFFF5252.toInt())
                        )
                    }

                    val barModelProducer = remember { CartesianChartModelProducer() }

                    LaunchedEffect(monthlyBarData) {
                        barModelProducer.runTransaction {
                            columnSeries {
                                series(monthlyBarData.map { it.income })
                                series(monthlyBarData.map { it.expense })
                            }
                        }
                    }

                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            layers = arrayOf(rememberColumnCartesianLayer(columnProvider = columnProvider)),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom()
                        ),
                        modelProducer = barModelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (config.isTablet && config.isLandscape) 260.dp else 200.dp)
                    )
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
                    .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Savings Capacity",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "You could save ${CurrencyFormatter.format(suggestion.suggestedAmount, selectedCurrency)} per month",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Based on your lowest surplus month (${CurrencyFormatter.format(suggestion.safeFloor, selectedCurrency)})",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val confidencePercent = (suggestion.confidence * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Confidence",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$confidencePercent%",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    val confidenceColor = when {
                        suggestion.confidence >= 0.7f -> Color(0xFF4CAF50)
                        suggestion.confidence >= 0.4f -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    }
                    AnimatedProgressBar(
                        progress = suggestion.confidence.coerceIn(0f, 1f),
                        color = confidenceColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF2C2C2C), RoundedCornerShape(3.dp))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { viewModel.dismissSavingsSuggestion() }) {
                        Text("Dismiss", color = Color.White.copy(alpha = 0.5f))
                    }
                    Button(
                        onClick = { onSetUpRecurring(suggestion.suggestedAmount) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF121315)
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                    .background(Color(0xFF1B3A2A), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frugality Streaks",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "2 months and counting",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
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
                    .background(Color(0xFF2C241B), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Upcoming seasonal expenses",
                    color = Color.White,
                    fontSize = 16.sp,
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
                    .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    IconButton(onClick = onOpenBudgets) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "View budgets",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                if (budgets.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBudgets() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Text(
                            text = "No active budgets",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap + to set a spending limit",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
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
                                                Text(budget.displayName, style=MaterialTheme.typography.bodyMedium, color=Color.White)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val forecast = budgetForecasts.find { it.budgetId == budget.id }
                                                    if (forecast != null) {
                                                        val (statusColor, statusText) = when (forecast.status) {
                                                            com.makemission.makeflow.domain.BudgetForecastStatus.ON_TRACK -> Color(0xFF4CAF50) to "On track"
                                                            com.makemission.makeflow.domain.BudgetForecastStatus.OFF_TRACK -> Color(0xFFFFA726) to "Off track"
                                                            com.makemission.makeflow.domain.BudgetForecastStatus.OVERSPENT -> Color(0xFFEF5350) to "Overspent"
                                                        }
                                                        Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                        Spacer(Modifier.width(6.dp))
                                                    }
                                                    Text("${CurrencyFormatter.format(budget.spentAmount, selectedCurrency)} / ${CurrencyFormatter.format(budget.amount, selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.5f))
                                                }
                                            }
                                            AnimatedProgressBar(progress=progress, color=progressColor, modifier=Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(3.dp)))
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
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
                                    Text(budget.displayName, style=MaterialTheme.typography.bodyMedium, color=Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val forecast = budgetForecasts.find { it.budgetId == budget.id }
                                        if (forecast != null) {
                                            val (statusColor, statusText) = when (forecast.status) {
                                                com.makemission.makeflow.domain.BudgetForecastStatus.ON_TRACK -> Color(0xFF4CAF50) to "On track"
                                                com.makemission.makeflow.domain.BudgetForecastStatus.OFF_TRACK -> Color(0xFFFFA726) to "Off track"
                                                com.makemission.makeflow.domain.BudgetForecastStatus.OVERSPENT -> Color(0xFFEF5350) to "Overspent"
                                            }
                                            Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text("${CurrencyFormatter.format(budget.spentAmount, selectedCurrency)} / ${CurrencyFormatter.format(budget.amount, selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.5f))
                                    }
                                }

                                AnimatedProgressBar(progress=progress, color=progressColor, modifier=Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(3.dp)))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
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
    }
}

@Composable
private fun FrugalStreakRow(
    streak: FrugalStreak,
    currencySymbol: String,
    selectedCurrency: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1C1F), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = streak.categoryName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Streak!",
                color = Color(0xFF4CAF50),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Saved ${CurrencyFormatter.format(streak.totalSavedVsAverage, selectedCurrency)} vs your average",
            color = Color(0xFF81C784),
            fontSize = 13.sp
        )
        Text(
            text = "Projected ${CurrencyFormatter.format(streak.projectedAnnualSavings, selectedCurrency)}/year",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Dismiss", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SeasonalPatternRow(
    pattern: SeasonalPattern,
    currencySymbol: String,
    selectedCurrency: String
) {
    val monthNames = pattern.peakMonths.map { month ->
        java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
    }
    val typeLabel = when (pattern.patternType) {
        PatternType.ANNUAL_BILL -> "Annual bill"
        PatternType.SEASONAL_SPIKE -> "Seasonal spike"
    }
    val typeBadgeColor = when (pattern.patternType) {
        PatternType.ANNUAL_BILL -> Color(0xFFFFA726)
        PatternType.SEASONAL_SPIKE -> Color(0xFFEF5350)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1C1F), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pattern.categoryName,
                color = Color.White,
                fontSize = 14.sp,
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
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        Text(
            text = "Expect ~${CurrencyFormatter.format(pattern.avgPeakAmount, selectedCurrency)}",
            color = Color(0xFFFFCC80),
            fontSize = 13.sp
        )
    }
}
