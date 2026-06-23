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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.makemission.makeflow.ui.components.AnimatedProgressBar
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.ui.viewmodel.CategorySpendingItem
import com.makemission.makeflow.ui.viewmodel.InsightsViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onOpenBudgets: () -> Unit = {}
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val monthlyBarData by viewModel.monthlyBarData.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
            .verticalFadingEdge(
                topFadeHeight = 80.dp,
                bottomFadeHeight = 80.dp
            ),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            text = "$currencySymbol ${String.format("%,.2f", totalIncome)}",
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
                            text = "$currencySymbol ${String.format("%,.2f", totalExpenses)}",
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
                            drawArc(
                                color = Color(0xFF2C2C2C),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(
                                    (size.width - outerR * 2) / 2f,
                                    (size.height - outerR * 2) / 2f
                                ),
                                size = Size(outerR * 2, outerR * 2),
                                style = Stroke(width = strokeW)
                            )
                        }
                        Text(
                            text = "No expenses",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
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
                                    val arcColor = try {
                                        Color(android.graphics.Color.parseColor(item.color))
                                    } catch (_: Exception) {
                                        Color(0xFF9E9E9E)
                                    }
                                    if (drawSweep > 0f) {
                                        drawArc(
                                            color = arcColor,
                                            startAngle = startAngle,
                                            sweepAngle = drawSweep,
                                            useCenter = false,
                                            topLeft = Offset(
                                                (size.width - outerR * 2) / 2f,
                                                (size.height - outerR * 2) / 2f
                                            ),
                                            size = Size(outerR * 2, outerR * 2),
                                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$currencySymbol ${String.format("%,.2f", totalExpenses)}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                val displayItems = if (categorySpending.size > 5) {
                    val top4 = categorySpending.take(4)
                    val others = categorySpending.drop(4)
                    val othersAmount = others.sumOf { it.amount }
                    val othersPercent = others.sumOf { it.percentage.toDouble() }.toFloat()
                    top4 + CategorySpendingItem("others", "Others", "#9E9E9E", othersAmount, othersPercent)
                } else {
                    categorySpending
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dotColor = try {
                                Color(android.graphics.Color.parseColor(item.color))
                            } catch (_: Exception) {
                                Color(0xFF9E9E9E)
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$currencySymbol ${String.format("%,.2f", item.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format("%.1f", item.percentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.End
                            )
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
                            .height(200.dp),
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
                            startAxis = null,
                            bottomAxis = null
                        ),
                        modelProducer = barModelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
            }
        }

        item {
            StaggeredItem(index = 5) {
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
                                Text(
                                    text = budget.categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "$currencySymbol ${String.format("%,.2f", budget.spentAmount)} / $currencySymbol ${String.format("%,.2f", budget.amount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
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

                        Spacer(modifier = Modifier.height(12.dp))
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
