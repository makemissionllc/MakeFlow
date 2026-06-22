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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel
) {
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var showAddBudget by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        Spacer(modifier = Modifier.height(96.dp))

        if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalFadingEdge(
                            topFadeHeight = 72.dp,
                            bottomFadeHeight = 80.dp
                        ),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = 96.dp, bottom = 140.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        val progress = (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f)
                        val progressColor = when {
                            progress >= 1f -> Color(0xFFEF5350)
                            progress >= 0.7f -> Color(0xFFFFA726)
                            else -> Color(0xFF4CAF50)
                        }

                        CardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { budgetToDelete = budget }
                                    ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = budget.categoryName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = when (budget.period) {
                                            "Weekly" -> "Weekly"
                                            "Monthly" -> "Monthly"
                                            "Annual" -> "Annual"
                                            else -> budget.period
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .background(
                                                Color(0xFF2C2C2C),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "$currencySymbol ${String.format("%,.2f", budget.spentAmount)} of $currencySymbol ${String.format("%,.2f", budget.amount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )

                                LinearProgressIndicator(
                                    progress = progress,
                                    color = progressColor,
                                    trackColor = Color(0xFF2C2C2C),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(Color(0xFF2C2C2C), RoundedCornerShape(3.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121315).copy(alpha = 0.85f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .align(Alignment.TopCenter)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            IconButton(onClick = { showAddBudget = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add budget",
                    tint = Color.White
                )
            }
        }
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
