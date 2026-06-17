package com.makemission.makeflow.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.makemission.makeflow.data.local.AppDatabase
import kotlin.math.cos
import kotlin.math.sin
import java.text.NumberFormat
import java.util.Locale

data class CashFlowPoint(val label: String, val income: Float, val expense: Float)

private fun generateChartData(totalIncome: Double, totalExpense: Double): List<CashFlowPoint> {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val incomeBase = (totalIncome / 12).toFloat().coerceAtLeast(100f)
    val expenseBase = (totalExpense / 12).toFloat().coerceAtLeast(100f)
    return months.mapIndexed { i, month ->
        CashFlowPoint(
            label = month,
            income = incomeBase * (1.0f + 0.3f * sin(i * 0.5f)),
            expense = expenseBase * (1.0f + 0.2f * cos(i * 0.4f))
        )
    }
}

@Composable
fun AnalyticsScreen() {
    Log.d("MakeFlowDebug", "AnalyticsScreen composing")
    val context = LocalContext.current
    val db = remember {
        try {
            AppDatabase.getDatabase(context)
        } catch (e: Exception) {
            Log.e("MakeFlowDebug", "AnalyticsScreen getDatabase failed", e)
            return@remember null
        }
    } ?: return
    val transactions by db.transactionDao().getAllTransactionsWithDetails().collectAsState(initial = emptyList())

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    val chartData = remember(totalIncome, totalExpense) {
        generateChartData(totalIncome, totalExpense)
    }

    var selectedTimeFilter by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AnalyticsTopBar()
        MetricCardsRow(totalIncome, totalExpense)
        CashFlowChartSection(
            chartData = chartData,
            selectedTimeFilter = selectedTimeFilter,
            onFilterChange = { selectedTimeFilter = it }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AnalyticsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun MetricCardsRow(totalIncome: Double, totalExpense: Double) {
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${fmt.format(totalIncome)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${fmt.format(totalExpense)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CashFlowChartSection(
    chartData: List<CashFlowPoint>,
    selectedTimeFilter: Int,
    onFilterChange: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "Cash Flow",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val w = size.width
            val h = size.height
            val leftPad = 40f
            val bottomPad = 25f
            val chartW = (w - leftPad * 2).coerceAtLeast(0f)
            val chartH = (h - bottomPad).coerceAtLeast(0f)

            if (chartW <= 0f || chartH <= 0f) return@Canvas

            val maxVal = chartData.maxOf { maxOf(it.income, it.expense) } * 1.15f

            for (i in 0..4) {
                val y = chartH * (1f - i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartW, y),
                    strokeWidth = 1f
                )
            }

            if (chartData.size < 2) return@Canvas

            val incomePath = Path().apply {
                chartData.forEachIndexed { idx, pt ->
                    val x = leftPad + chartW * idx / (chartData.size - 1)
                    val y = chartH * (1f - pt.income / maxVal)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(incomePath, color = primaryColor, style = Stroke(width = 3f))

            val expensePath = Path().apply {
                chartData.forEachIndexed { idx, pt ->
                    val x = leftPad + chartW * idx / (chartData.size - 1)
                    val y = chartH * (1f - pt.expense / maxVal)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                expensePath, color = primaryColor,
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )

            val fillPath = Path().apply {
                chartData.forEachIndexed { idx, pt ->
                    val x = leftPad + chartW * idx / (chartData.size - 1)
                    val y = chartH * (1f - pt.income / maxVal)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
                lineTo(leftPad + chartW, chartH)
                lineTo(leftPad, chartH)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f,
                    endY = chartH
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            chartData.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            val filters = listOf("1W", "1M", "1Y")
            filters.forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedTimeFilter == index,
                    onClick = { onFilterChange(index) },
                    label = { Text(label, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
