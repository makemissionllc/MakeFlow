package com.makemission.makeflow.ui.screens

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makemission.makeflow.ui.components.ExpressiveLineChart
import com.makemission.makeflow.ui.haptic.lightTick
import com.makemission.makeflow.ui.state.AnalyticsUiState
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.viewmodel.AnalyticsViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel()
) {
    val dimens = LocalAppDimens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AnalyticsTopBar()
        MetricCardsRow(
            totalIncome = uiState.totalIncome,
            totalExpense = uiState.totalExpense
        )
        CashFlowChartSection(
            uiState = uiState,
            selectedFilter = selectedFilter,
            onFilterChange = { viewModel.onFilterChange(it) }
        )
        Spacer(modifier = Modifier.height(dimens.paddingLarge))
    }
}

@Composable
private fun AnalyticsTopBar() {
    val dimens = LocalAppDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall + 4.dp),
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
    val dimens = LocalAppDimens.current
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium),
        horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall + 4.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(dimens.cornerRadiusMedium),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimens.cardElevation)
        ) {
            Column(modifier = Modifier.padding(dimens.paddingMedium)) {
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
                        modifier = Modifier.size(dimens.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(dimens.paddingSmall))

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
            shape = RoundedCornerShape(dimens.cornerRadiusMedium),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimens.cardElevation)
        ) {
            Column(modifier = Modifier.padding(dimens.paddingMedium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(dimens.paddingSmall))

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
    uiState: AnalyticsUiState,
    selectedFilter: Int,
    onFilterChange: (Int) -> Unit
) {
    val dimens = LocalAppDimens.current

    if (uiState.isLoading) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(dimens.paddingMedium)
        )
        return
    }

    var selectedValue by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall + 4.dp)) {
        Text(
            text = "Cash Flow",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (selectedValue != null) {
            Text(
                text = selectedValue!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        val incomeAmounts = uiState.incomeChartPoints.map { it.second }
        val expenseAmounts = uiState.expenseChartPoints.map { it.second }

        if (incomeAmounts.isNotEmpty()) {
            ExpressiveLineChart(
                dataPoints = incomeAmounts,
                onPointSelected = { _, value ->
                    selectedValue = "Income: $${String.format(Locale.US, "%.2f", value)}"
                },
                onScrubCleared = { selectedValue = null }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (expenseAmounts.isNotEmpty()) {
            ExpressiveLineChart(
                dataPoints = expenseAmounts,
                onPointSelected = { _, value ->
                    selectedValue = "Expense: $${String.format(Locale.US, "%.2f", value)}"
                },
                onScrubCleared = { selectedValue = null }
            )
        }

        if (incomeAmounts.isNotEmpty()) {
            val labels = uiState.incomeChartPoints.map { it.first }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall, Alignment.CenterHorizontally)
        ) {
            val haptic = LocalHapticFeedback.current
            val filters = listOf("1W", "1M", "1Y")

            filters.forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedFilter == index,
                    onClick = { haptic.lightTick(); onFilterChange(index) },
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
