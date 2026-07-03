package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.domain.BudgetSuggestion
import com.makemission.makeflow.domain.SuggestionReason
import com.makemission.makeflow.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel,
    hapticManager: HapticManager,
    notificationGate: NotificationGate? = null
) {
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val budgetSuggestions by viewModel.budgetSuggestions.collectAsState()

    val suggestionsByCategory = remember(budgetSuggestions) {
        budgetSuggestions.associateBy { it.categoryId }
    }

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
                    "orange" -> hapticManager.fire(HapticEvent.BUDGET_WARNING)
                    "red" -> {
                        hapticManager.fire(HapticEvent.BUDGET_EXCEEDED)
                        notificationGate?.dispatchOrQueue(
                            type = "budget_alert",
                            title = "Budget exceeded",
                            body = "${budget.categoryName} has exceeded its limit"
                        )
                    }
                }
            }
            previousBudgetZones[budget.id] = currentZone
        }
    }

    val config = LocalAdaptiveConfig.current
    val listState = rememberLazyListState()
    var showAddBudget by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
                            start = config.horizontalPadding, end = config.horizontalPadding, bottom = 32.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No budgets yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Tap + to set a spending limit",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
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
                        LargeTitleItem(title = "Budgets", scrollState = listState)
                    }
                    if (config.isTablet && config.isLandscape) {
                        val chunked = budgets.chunked(2)
                        itemsIndexed(chunked, key = { _, chunk -> chunk.map { it.id }.joinToString() }) { index, chunk ->
                            StaggeredItem(index = index + 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(config.cardSpacing)
                            ) {
                                chunk.forEach { budget ->
                                    val suggestion = suggestionsByCategory[budget.categoryId]
                                    if (suggestion != null) {
                                        SuggestionCard(
                                            suggestion = suggestion,
                                            currencySymbol = currencySymbol,
                                            onApply = { viewModel.applySuggestion(suggestion) },
                                            onDismiss = { viewModel.dismissSuggestion(suggestion) }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        val progress = (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
                                        val progressColor = when {
                                            progress >= 1f -> Color(0xFFEF5350)
                                            progress >= 0.7f -> Color(0xFFFFA726)
                                            else -> Color(0xFF4CAF50)
                                        }

                                        CardContainer {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().pressScale().combinedClickable(onClick={}, onLongClick={ budgetToDelete = budget }),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                                                    Text(budget.categoryName, style=MaterialTheme.typography.bodyLarge, color=Color.White)
                                                    Text(
                                                        when (budget.period) { "Weekly"->"Weekly" "Monthly"->"Monthly" "Annual"->"Annual" else->budget.period },
                                                        style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.6f),
                                                        modifier=Modifier.background(Color(0xFF2C2C2C), RoundedCornerShape(6.dp)).padding(horizontal=8.dp, vertical=4.dp)
                                                    )
                                                }
                                                Text("$currencySymbol ${String.format("%,.2f", budget.spentAmount)} of $currencySymbol ${String.format("%,.2f", budget.amount)}", style=MaterialTheme.typography.labelMedium, color=Color.White.copy(alpha=0.6f))
                                                AnimatedProgressBar(progress=progress, color=progressColor, modifier=Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(3.dp)))
                                            }
                                        }
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            }
                        }
                    } else {
                        itemsIndexed(budgets, key = { _, budget -> budget.id }) { index, budget ->
                            StaggeredItem(index = index + 1) {
                            val suggestion = suggestionsByCategory[budget.categoryId]
                            if (suggestion != null) {
                                SuggestionCard(
                                    suggestion = suggestion,
                                    currencySymbol = currencySymbol,
                                    onApply = { viewModel.applySuggestion(suggestion) },
                                    onDismiss = { viewModel.dismissSuggestion(suggestion) }
                                )
                                Spacer(modifier = Modifier.height(config.cardSpacing))
                            }
                            val progress = (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
                            val progressColor = when {
                                progress >= 1f -> Color(0xFFEF5350)
                                progress >= 0.7f -> Color(0xFFFFA726)
                                else -> Color(0xFF4CAF50)
                            }

                            CardContainer {
                                Column(
                                    modifier = Modifier.fillMaxWidth().pressScale().combinedClickable(onClick={}, onLongClick={ budgetToDelete = budget }),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                                        Text(budget.categoryName, style=MaterialTheme.typography.bodyLarge, color=Color.White)
                                        Text(
                                            when (budget.period) { "Weekly"->"Weekly" "Monthly"->"Monthly" "Annual"->"Annual" else->budget.period },
                                            style=MaterialTheme.typography.labelSmall, color=Color.White.copy(alpha=0.6f),
                                            modifier=Modifier.background(Color(0xFF2C2C2C), RoundedCornerShape(6.dp)).padding(horizontal=8.dp, vertical=4.dp)
                                        )
                                    }
                                    Text("$currencySymbol ${String.format("%,.2f", budget.spentAmount)} of $currencySymbol ${String.format("%,.2f", budget.amount)}", style=MaterialTheme.typography.labelMedium, color=Color.White.copy(alpha=0.6f))
                                    AnimatedProgressBar(progress=progress, color=progressColor, modifier=Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(3.dp)))
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }

        CollapsingTopBar(
            title = "Budgets",
            scrollState = listState,
            modifier = Modifier.align(Alignment.TopStart),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { showAddBudget = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add budget",
                        tint = Color.White
                    )
                }
            }
        )
    }

    if (showAddBudget) {
        AddBudgetBottomSheet(
            onDismiss = { showAddBudget = false },
            onSave = { categoryId, categoryName, amount, period ->
                scope.launch {
                    viewModel.addBudget(categoryId, categoryName, amount, period)
                    showAddBudget = false
                }
            },
            categories = categories,
            currencySymbol = currencySymbol
        )
    }

    budgetToDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text(
                    text = "Delete Budget",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will remove the budget but not your transactions.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBudget(budget)
                    budgetToDelete = null
                }) {
                    Text("Confirm", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
fun SuggestionCard(
    suggestion: BudgetSuggestion,
    currencySymbol: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val lowConfidence = suggestion.confidence < 0.5f
    val alpha = if (lowConfidence) 0.6f else 1f
    val message = when (suggestion.reason) {
        SuggestionReason.NO_BUDGET_SET -> "Based on your last 3 months, try a budget of $currencySymbol ${String.format("%,.2f", suggestion.suggestedAmount)}"
        SuggestionReason.CONSISTENTLY_OVER -> "You've gone over this budget 2 months running — adjust to $currencySymbol ${String.format("%,.2f", suggestion.suggestedAmount)}?"
        SuggestionReason.CONSISTENTLY_UNDER -> "You're consistently under this budget — lower it to $currencySymbol ${String.format("%,.2f", suggestion.suggestedAmount)} and free up room elsewhere?"
    }

    CardContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = Color.White.copy(alpha = alpha),
                fontSize = if (lowConfidence) 12.sp else 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onApply,
                    modifier = Modifier.background(Color.White, RoundedCornerShape(10.dp))
                ) {
                    Text("Apply", color = Color(0xFF121315), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color(0xFF2C2C2C), RoundedCornerShape(10.dp))
                ) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
    }
}