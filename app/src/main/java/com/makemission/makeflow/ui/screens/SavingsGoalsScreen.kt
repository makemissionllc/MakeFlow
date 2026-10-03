package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FlagCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.domain.FeasibilityStatus
import com.makemission.makeflow.domain.GoalFeasibilityResult
import com.makemission.makeflow.domain.SavingsGoalWithProgress
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.PrimaryDockContentInset
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.components.AnimatedDoodle
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.viewmodel.GoalWithFeasibility
import com.makemission.makeflow.ui.viewmodel.SavingsGoalsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    onBack: () -> Unit,
    viewModel: SavingsGoalsViewModel,
    accounts: List<AccountEntity>,
    currencyCode: String,
    currencySymbol: String,
    onAddAccount: () -> Unit,
    isPrimaryDestination: Boolean = false,
    dockAddRequest: Int = 0
) {
    val dimens = LocalAppDimens.current
    val goalItems by viewModel.goalsWithFeasibility.collectAsState()

    val config = LocalAdaptiveConfig.current
    val listState = rememberLazyListState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalWithFeasibility?>(null) }
    var goalToDelete by remember { mutableStateOf<com.makemission.makeflow.data.local.entity.SavingsGoalEntity?>(null) }
    var goalToAbandon by remember { mutableStateOf<com.makemission.makeflow.data.local.entity.SavingsGoalEntity?>(null) }
    var shouldReopenSheet by remember { mutableStateOf(false) }
    var createdAccountId by remember { mutableStateOf<String?>(null) }
    var previousAccountIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    var lastDockAddRequest by remember { mutableStateOf(dockAddRequest) }
    LaunchedEffect(dockAddRequest) {
        if (dockAddRequest != lastDockAddRequest) {
            lastDockAddRequest = dockAddRequest
            showAddSheet = true
            shouldReopenSheet = false
            createdAccountId = null
        }
    }

    LaunchedEffect(shouldReopenSheet, accounts) {
        if (shouldReopenSheet) {
            val currentIds = accounts.map { it.id }.toSet()
            if (currentIds.size > previousAccountIds.size) {
                val newId = (currentIds - previousAccountIds).firstOrNull()
                createdAccountId = newId
                shouldReopenSheet = false
                showAddSheet = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight),
            contentPadding = PaddingValues(bottom = PrimaryDockContentInset())
        ) {
            item {
                LargeTitleItem(title = "Goals", scrollState = listState)
            }

            if (goalItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = config.horizontalPadding, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedDoodle(
                                icon = Icons.Outlined.EmojiEvents,
                                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(dimens.spacingXLarge))
                            Text(
                                "No goals yet",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = dimens.textL
                            )
                            Spacer(Modifier.height(dimens.spacingXSmall))
                            Text(
                                "Create a savings goal and link it to an account",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontSize = dimens.textSm,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(goalItems, key = { it.goalWithProgress.goal.id }) { item ->
                GoalCard(
                    item = item,
                    currencyCode = currencyCode,
                    onEdit = { editingGoal = item },
                    onDelete = { goalToDelete = item.goalWithProgress.goal },
                    onAbandon = { goalToAbandon = item.goalWithProgress.goal }
                )
            }

        }

        CollapsingTopBar(
            title = "",
            scrollState = listState,
            navigationIcon = if (isPrimaryDestination) null else {
                {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                if (!isPrimaryDestination) {
                    IconButton(onClick = {
                        showAddSheet = true
                        shouldReopenSheet = false
                        createdAccountId = null
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add goal", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        )

        if (showAddSheet || editingGoal != null) {
            val existing = editingGoal
            AddEditSavingsGoalBottomSheet(
                existingGoal = existing?.goalWithProgress?.goal,
                accounts = accounts,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                onDismiss = {
                    showAddSheet = false
                    editingGoal = null
                },
                onSave = { name, targetAmount, targetDate, linkedAccountId ->
                    scope.launch {
                        if (existing != null) {
                            viewModel.updateGoal(existing.goalWithProgress.goal, name, targetAmount, targetDate, linkedAccountId)
                        } else {
                            viewModel.addGoal(name, targetAmount, targetDate, linkedAccountId)
                        }
                    }
                    showAddSheet = false
                    editingGoal = null
                },
                onAddAccount = {
                    previousAccountIds = accounts.map { it.id }.toSet()
                    showAddSheet = false
                    shouldReopenSheet = true
                    onAddAccount()
                },
                focusedAccountId = createdAccountId
            )
        }
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Delete Goal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${goal.name}\"? This cannot be undone.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.deleteGoal(goal) }
                    goalToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            }
        )
    }

    goalToAbandon?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToAbandon = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Abandon Goal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Mark \"${goal.name}\" as abandoned? You can create a new goal later.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.abandonGoal(goal) }
                    goalToAbandon = null
                }) { Text("Abandon", color = LocalMakeFlowColors.current.colorWarning) }
            },
            dismissButton = {
                TextButton(onClick = { goalToAbandon = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            }
        )
    }
}

@Composable
private fun GoalCard(
    item: GoalWithFeasibility,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAbandon: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val gwp = item.goalWithProgress
    val goal = gwp.goal
    val config = LocalAdaptiveConfig.current
    val isAchieved = goal.status == "ACHIEVED"
    val isAbandoned = goal.status == "ABANDONED"

    val cardBackground = when {
        isAchieved -> MaterialTheme.colorScheme.surfaceContainerLow
        isAbandoned -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val progressColor = when {
        isAchieved -> MaterialTheme.colorScheme.tertiary
        gwp.percentage >= 1f -> LocalMakeFlowColors.current.colorSuccess
        gwp.percentage >= 0.7f -> LocalMakeFlowColors.current.colorWarning
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = config.horizontalPadding)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(dimens.shapeCornerMedium))
            .background(cardBackground)
            .pressScale()
            .clickable { onEdit() }
            .padding(dimens.paddingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goal.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = dimens.textL,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            if (isAchieved) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Achieved",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(dimens.spacingXSmall))
                Text(
                    text = "Achieved",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = dimens.textS,
                    fontWeight = FontWeight.Bold
                )
            } else {
                StatusBadge(status = goal.status)
            }
        }

        Spacer(Modifier.height(dimens.spacingMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = CurrencyFormatter.format(gwp.progress, currencyCode),
                color = if (isAchieved) MaterialTheme.colorScheme.tertiary else progressColor,
                fontWeight = FontWeight.Bold,
                fontSize = dimens.textXl
            )
            Text(
                text = CurrencyFormatter.format(goal.targetAmount, currencyCode),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = dimens.textM
            )
        }

        Spacer(Modifier.height(dimens.spacingMedium))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(gwp.percentage)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }

        if (isAchieved) {
            Spacer(Modifier.height(dimens.spacingMedium))
            Text(
                text = "Goal completed! You reached your target of ${CurrencyFormatter.format(goal.targetAmount, currencyCode)}.",
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                fontSize = dimens.textS
            )
        }

        if (!isAchieved && !isAbandoned) {
            FeasibilityRow(
                feasibility = item.feasibility,
                currencyCode = currencyCode
            )
        }

        if (isAbandoned) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Abandoned",
                color = MaterialTheme.colorScheme.outline,
                fontSize = dimens.textS
            )
        }

        if (goal.targetDate != null && !isAchieved) {
            Spacer(Modifier.height(6.dp))
            val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
            Text(
                text = "Target: ${df.format(Date(goal.targetDate))}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = dimens.textS
            )
        }

        if (!isAbandoned) {
            Spacer(Modifier.height(dimens.spacingMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isAchieved) {
                    TextButton(onClick = onAbandon) {
                        Text("Abandon", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textS)
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), fontSize = dimens.textS)
                }
            }
        }
    }
}

@Composable
private fun FeasibilityRow(
    feasibility: GoalFeasibilityResult?,
    currencyCode: String
) {
    val dimens = LocalAppDimens.current
    if (feasibility == null || feasibility.status == FeasibilityStatus.INSUFFICIENT_DATA) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Track more spending to see goal feasibility",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            fontSize = dimens.textXs
        )
        return
    }

    Spacer(Modifier.height(6.dp))
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
    Spacer(Modifier.height(6.dp))

    when (feasibility.status) {
        FeasibilityStatus.FEASIBLE -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "On track",
                    color = LocalMakeFlowColors.current.colorSuccess,
                    fontSize = dimens.textS,
                    fontWeight = FontWeight.Medium
                )
                if (feasibility.monthsRemaining != null) {
                    Text(
                        text = " \u00B7 ${feasibility.monthsRemaining} months to go",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = dimens.textXs
                    )
                }
            }
        }

        FeasibilityStatus.NOT_FEASIBLE -> {
            val tradeOff = feasibility.tradeOffSuggestion
            if (tradeOff != null) {
                val suggestion = tradeOff.suggestions.firstOrNull()
                if (suggestion != null) {
                    val pct = (suggestion.reductionPercent * 100).toInt()
                    Text(
                        text = "Reduce ${suggestion.categoryName} by $pct% to hit your date",
                        color = LocalMakeFlowColors.current.colorWarning,
                        fontSize = dimens.textS,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (feasibility.revisedTimelineMonths != null) {
                val diff = feasibility.revisedTimelineMonths - (feasibility.monthsRemaining ?: 0)
                val timelineText = if (diff > 0) {
                    "Revised estimate: $diff month${if (diff > 1) "s" else ""} later than planned"
                } else {
                    "Revised estimate: ${feasibility.revisedTimelineMonths} months"
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = timelineText,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = dimens.textXs
                )
            }
        }

        FeasibilityStatus.PROJECTION_ONLY -> {
            if (feasibility.revisedTimelineMonths != null) {
                Text(
                    text = "Estimated ${feasibility.revisedTimelineMonths} month${if (feasibility.revisedTimelineMonths > 1) "s" else ""} at current savings rate",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = dimens.textXs
                )
            }
        }

        FeasibilityStatus.INSUFFICIENT_DATA -> {}
    }
}

@Composable
private fun StatusBadge(status: String) {
    val dimens = LocalAppDimens.current
    val (label, color) = when (status) {
        "ACTIVE" -> "Active" to LocalMakeFlowColors.current.colorSuccess
        "ACHIEVED" -> "Achieved" to LocalMakeFlowColors.current.colorSuccess
        "ABANDONED" -> "Abandoned" to MaterialTheme.colorScheme.outline
        else -> status to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    Text(
        text = label,
        color = color,
        fontSize = dimens.textXs,
        fontWeight = FontWeight.Medium
    )
}
