package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.pressScale
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
    onAddAccount: () -> Unit
) {
    val goalItems by viewModel.goalsWithFeasibility.collectAsState()

    val config = LocalAdaptiveConfig.current
    val listState = rememberLazyListState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalWithFeasibility?>(null) }
    var goalToDelete by remember { mutableStateOf<com.makemission.makeflow.data.local.entity.SavingsGoalEntity?>(null) }
    var goalToAbandon by remember { mutableStateOf<com.makemission.makeflow.data.local.entity.SavingsGoalEntity?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121315))) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight)
        ) {
            item {
                LargeTitleItem(title = "Savings Goals", scrollState = listState)
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
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No goals yet",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Create a savings goal and link it to an account",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 13.sp,
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

            item { Spacer(Modifier.height(80.dp)) }
        }

        CollapsingTopBar(
            title = "",
            scrollState = listState,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add goal", tint = Color.White)
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
                onAddAccount = onAddAccount
            )
        }
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            containerColor = Color(0xFF1B1C1F),
            title = { Text("Delete Goal", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${goal.name}\"? This cannot be undone.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.deleteGoal(goal) }
                    goalToDelete = null
                }) { Text("Delete", color = Color(0xFFEF5350)) }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text("Cancel", color = Color.White.copy(alpha = 0.5f)) }
            }
        )
    }

    goalToAbandon?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToAbandon = null },
            containerColor = Color(0xFF1B1C1F),
            title = { Text("Abandon Goal", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Mark \"${goal.name}\" as abandoned? You can create a new goal later.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.abandonGoal(goal) }
                    goalToAbandon = null
                }) { Text("Abandon", color = Color(0xFFFFA726)) }
            },
            dismissButton = {
                TextButton(onClick = { goalToAbandon = null }) { Text("Cancel", color = Color.White.copy(alpha = 0.5f)) }
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
    val gwp = item.goalWithProgress
    val goal = gwp.goal
    val config = LocalAdaptiveConfig.current
    val isAchieved = goal.status == "ACHIEVED"
    val isAbandoned = goal.status == "ABANDONED"

    val cardBackground = when {
        isAchieved -> Color(0xFF1E2810)
        isAbandoned -> Color(0xFF1B1C1F)
        else -> Color(0xFF1B1C1F)
    }

    val progressColor = when {
        isAchieved -> Color(0xFFFFD700)
        gwp.percentage >= 1f -> Color(0xFF4CAF50)
        gwp.percentage >= 0.7f -> Color(0xFFFFA726)
        else -> Color(0xFF42A5F5)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = config.horizontalPadding)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .pressScale()
            .clickable { onEdit() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goal.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            if (isAchieved) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Achieved",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Achieved",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                StatusBadge(status = goal.status)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = CurrencyFormatter.format(gwp.progress, currencyCode),
                color = if (isAchieved) Color(0xFFFFD700) else progressColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = CurrencyFormatter.format(goal.targetAmount, currencyCode),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
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
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Goal completed! You reached your target of ${CurrencyFormatter.format(goal.targetAmount, currencyCode)}.",
                color = Color(0xFFFFD700).copy(alpha = 0.8f),
                fontSize = 12.sp
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
                color = Color(0xFF757575),
                fontSize = 12.sp
            )
        }

        if (goal.targetDate != null && !isAchieved) {
            Spacer(Modifier.height(6.dp))
            val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
            Text(
                text = "Target: ${df.format(Date(goal.targetDate))}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }

        if (!isAbandoned) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isAchieved) {
                    TextButton(onClick = onAbandon) {
                        Text("Abandon", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color(0xFFEF5350).copy(alpha = 0.6f), fontSize = 12.sp)
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
    if (feasibility == null || feasibility.status == FeasibilityStatus.INSUFFICIENT_DATA) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Track more spending to see goal feasibility",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 11.sp
        )
        return
    }

    Spacer(Modifier.height(6.dp))
    HorizontalDivider(
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.06f)
    )
    Spacer(Modifier.height(6.dp))

    when (feasibility.status) {
        FeasibilityStatus.FEASIBLE -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "On track",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (feasibility.monthsRemaining != null) {
                    Text(
                        text = " \u00B7 ${feasibility.monthsRemaining} months to go",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
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
                        color = Color(0xFFFFA726),
                        fontSize = 12.sp,
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
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }

        FeasibilityStatus.PROJECTION_ONLY -> {
            if (feasibility.revisedTimelineMonths != null) {
                Text(
                    text = "Estimated ${feasibility.revisedTimelineMonths} month${if (feasibility.revisedTimelineMonths > 1) "s" else ""} at current savings rate",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }

        FeasibilityStatus.INSUFFICIENT_DATA -> {}
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "ACTIVE" -> "Active" to Color(0xFF4CAF50)
        "ACHIEVED" -> "Achieved" to Color(0xFF66BB6A)
        "ABANDONED" -> "Abandoned" to Color(0xFF757575)
        else -> status to Color.White.copy(alpha = 0.5f)
    }
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}
