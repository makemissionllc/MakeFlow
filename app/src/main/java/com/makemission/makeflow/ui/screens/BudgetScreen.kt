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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import com.makemission.makeflow.core.layout.AdaptiveAlertSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.makemission.makeflow.ui.components.FittedAmountText
import com.makemission.makeflow.ui.components.PrimaryDockContentInset
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.components.AnimatedDoodle
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.domain.BudgetSearch
import com.makemission.makeflow.domain.BudgetSuggestion
import com.makemission.makeflow.domain.SuggestionReason
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.viewmodel.BudgetViewModel
import com.makemission.makeflow.widget.WidgetUpdateHelper
import com.makemission.makeflow.core.format.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel,
    hapticManager: HapticManager,
    notificationGate: NotificationGate? = null,
    isPrimaryDestination: Boolean = false,
    dockAddRequest: Int = 0
) {
    val dimens = LocalAppDimens.current
    val budgets by viewModel.budgets.collectAsState()
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
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
    var showAddBudget by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleBudgets = remember(budgets, searchQuery) { BudgetSearch.filter(budgets, searchQuery) }
    val scope = rememberCoroutineScope()

    var lastDockAddRequest by remember { mutableStateOf(dockAddRequest) }
    LaunchedEffect(dockAddRequest) {
        if (dockAddRequest != lastDockAddRequest) {
            lastDockAddRequest = dockAddRequest
            showAddBudget = true
        }
    }

    LaunchedEffect(budgets) {
        WidgetUpdateHelper.updateWidgets(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
                    ) {
                        AnimatedDoodle(
                            icon = Icons.Outlined.Savings,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "No budgets yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Tap + to set a spending limit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
                        bottom = PrimaryDockContentInset()
                    ),
                    verticalArrangement = Arrangement.spacedBy(config.cardSpacing)
                ) {
                    if (!isPrimaryDestination) {
                        item { LargeTitleItem(title = "Budget", scrollState = listState) }
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.budget_search)) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        hapticManager.fire(HapticEvent.NAV_SELECT)
                                        searchQuery = ""
                                    }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.budget_search_clear))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )
                    }
                    if (visibleBudgets.isEmpty()) {
                        item { Text(stringResource(R.string.budget_no_matches), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    item { MonthlyBudgetOverview(visibleBudgets, selectedCurrency) }
                    item { BudgetWarningNotice(visibleBudgets) }
                    if (config.layoutMode == DeviceLayoutMode.EXPANDED ||
                        (config.layoutMode == DeviceLayoutMode.MEDIUM && config.isLandscape)) {
                        val columns = if (config.layoutMode == DeviceLayoutMode.EXPANDED && config.isLandscape) 3 else 2
                        val chunked = visibleBudgets.chunked(columns)
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
                                            currencyCode = selectedCurrency,
                                            onApply = { viewModel.applySuggestion(suggestion) },
                                            onDismiss = { viewModel.dismissSuggestion(suggestion) }
                                        )
                                        Spacer(modifier = Modifier.height(dimens.spacingXSmall))
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        BudgetLimitRow(
                                            budget = budget,
                                            currencyCode = selectedCurrency,
                                            hapticManager = hapticManager,
                                            onEdit = { editingBudget = budget },
                                            onDelete = { budgetToDelete = budget }
                                        )
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            }
                        }
                    } else {
                        itemsIndexed(visibleBudgets, key = { _, budget -> budget.id }) { index, budget ->
                            StaggeredItem(index = index + 1) {
                            val suggestion = suggestionsByCategory[budget.categoryId]
                            if (suggestion != null) {
                                SuggestionCard(
                                    suggestion = suggestion,
                                    currencyCode = selectedCurrency,
                                    onApply = { viewModel.applySuggestion(suggestion) },
                                    onDismiss = { viewModel.dismissSuggestion(suggestion) }
                                )
                                Spacer(modifier = Modifier.height(config.cardSpacing))
                            }
                            val progress = (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
                            val progressColor = when {
                                progress >= 1f -> MaterialTheme.colorScheme.error
                                progress >= 0.7f -> LocalMakeFlowColors.current.colorWarning
                                else -> LocalMakeFlowColors.current.colorSuccess
                            }

                            BudgetLimitRow(
                                budget = budget,
                                currencyCode = selectedCurrency,
                                hapticManager = hapticManager,
                                onEdit = { editingBudget = budget },
                                onDelete = { budgetToDelete = budget }
                            )
                            }
                        }
                    }
                }
            }

        CollapsingTopBar(
            title = "Budget",
            scrollState = listState,
            modifier = Modifier.align(Alignment.TopStart),
            navigationIcon = if (isPrimaryDestination) null else {
                {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            actions = {
                if (!isPrimaryDestination) {
                    IconButton(onClick = { showAddBudget = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add budget",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )
    }

    if (showAddBudget || editingBudget != null) {
        AddBudgetBottomSheet(
            onDismiss = {
                showAddBudget = false
                editingBudget = null
            },
            onSave = { budgetId, categoryId, categoryName, amount, period, name ->
                scope.launch {
                    if (budgetId != null) {
                        viewModel.updateBudget(budgetId, amount, period, name)
                    } else {
                        viewModel.addBudget(categoryId, categoryName, amount, period, name)
                    }
                    showAddBudget = false
                    editingBudget = null
                }
            },
            categories = categories,
            currencySymbol = currencySymbol,
            editingBudget = editingBudget
        )
    }

    budgetToDelete?.let { budget ->
        AdaptiveAlertSheet(
            onDismissRequest = { budgetToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Delete Budget",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will remove the budget but not your transactions.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBudget(budget)
                    budgetToDelete = null
                }) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
private fun MonthlyBudgetOverview(budgets: List<BudgetEntity>, currencyCode: String) {
    val dimens = LocalAppDimens.current
    val monthlyBudgets = budgets.filter { it.period == "Monthly" }
    if (monthlyBudgets.isEmpty()) return

    val totalLimit = monthlyBudgets.sumOf { it.amount }
    val totalSpent = monthlyBudgets.sumOf { it.spentAmount }
    val progress = if (totalLimit > 0.0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
    val monthName = java.time.LocalDate.now().month.getDisplayName(
        java.time.format.TextStyle.FULL,
        java.util.Locale.getDefault()
    )

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
        Text(
            text = "$monthName · ${CurrencyFormatter.format(totalLimit, currencyCode)} planned",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium
        )
        FittedAmountText(
            text = CurrencyFormatter.format(totalSpent, currencyCode),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "spent so far",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        AnimatedProgressBar(
            progress = progress,
            color = if (progress >= 0.7f) LocalMakeFlowColors.current.colorWarning
            else LocalMakeFlowColors.current.colorSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.progressTrackHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusSmall))
        )
        val remaining = totalLimit - totalSpent
        Text(
            text = if (remaining >= 0.0) {
                "${CurrencyFormatter.format(remaining, currencyCode)} left across monthly budgets"
            } else {
                "${CurrencyFormatter.format(-remaining, currencyCode)} over monthly budgets"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BudgetWarningNotice(budgets: List<BudgetEntity>) {
    val dimens = LocalAppDimens.current
    val warningBudget = budgets
        .filter { it.amount > 0.0 }
        .maxByOrNull { it.spentAmount / it.amount }
        ?.takeIf { it.spentAmount / it.amount >= 0.7 }
        ?: return

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
            text = "Heads up: ${warningBudget.displayName} is nearing its ${warningBudget.period.lowercase()} limit.",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BudgetLimitRow(
    budget: BudgetEntity,
    currencyCode: String,
    hapticManager: HapticManager,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val progress = if (budget.amount > 0.0) {
        (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressColor = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.7f -> LocalMakeFlowColors.current.colorWarning
        else -> LocalMakeFlowColors.current.colorSuccess
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .combinedClickable(
                onClick = {
                    hapticManager.fire(HapticEvent.NAV_SELECT)
                    onEdit()
                },
                onLongClick = {
                    hapticManager.fire(HapticEvent.DELETE_UNDO_AVAILABLE)
                    onDelete()
                }
            )
            .padding(vertical = dimens.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = budget.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                if (budget.name != null) {
                    Text(
                        text = budget.categoryName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(dimens.spacingMedium))
            Text(
                text = CurrencyFormatter.format(budget.amount, currencyCode),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
        Text(
            text = "${CurrencyFormatter.format(budget.spentAmount, currencyCode)} of ${CurrencyFormatter.format(budget.amount, currencyCode)} · ${budget.period.lowercase()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
        AnimatedProgressBar(
            progress = progress,
            color = progressColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.progressTrackHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusSmall))
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            thickness = dimens.spacingXSmall / 2
        )
    }
}

@Composable
fun SuggestionCard(
    suggestion: BudgetSuggestion,
    currencyCode: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val lowConfidence = suggestion.confidence < 0.5f
    val alpha = if (lowConfidence) 0.6f else 1f
    val formattedAmount = CurrencyFormatter.format(suggestion.suggestedAmount, currencyCode)
    val message = when (suggestion.reason) {
        SuggestionReason.NO_BUDGET_SET -> "Based on your last 3 months, try a budget of $formattedAmount"
        SuggestionReason.CONSISTENTLY_OVER -> "You've gone over this budget 2 months running — adjust to $formattedAmount?"
        SuggestionReason.CONSISTENTLY_UNDER -> "You're consistently under this budget — lower it to $formattedAmount and free up room elsewhere?"
    }

    CardContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                fontSize = if (lowConfidence) 12.sp else 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                TextButton(
                    onClick = onApply,
                    modifier = Modifier.background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(dimens.shapeCornerXSmall))
                ) {
                    Text("Apply", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.SemiBold, fontSize = dimens.textSm)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerXSmall))
                ) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = dimens.textSm)
                }
            }
        }
    }
}
