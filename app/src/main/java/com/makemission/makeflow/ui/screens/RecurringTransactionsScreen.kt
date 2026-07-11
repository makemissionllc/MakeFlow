package com.makemission.makeflow.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.ui.viewmodel.RecurringTransactionsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecurringTransactionsScreen(
    onBack: () -> Unit,
    viewModel: RecurringTransactionsViewModel
) {
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val editEntity by viewModel.editEntity.collectAsState()
    val listState = rememberLazyListState()
    val config = LocalAdaptiveConfig.current
    val layoutMode = LocalDeviceLayoutMode.current

    var showAddEdit by remember { mutableStateOf(false) }
    var entityToDelete by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    val scope = rememberCoroutineScope()

    val isTablet = layoutMode != com.makemission.makeflow.core.layout.DeviceLayoutMode.COMPACT

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = config.horizontalPadding),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                LargeTitleItem(title = "Recurring", scrollState = listState)
            }

            if (recurringTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No recurring transactions yet",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 32.dp, start = 4.dp)
                    )
                }
            } else {
                val staggeredItems = if (isTablet) {
                    recurringTransactions.chunked(2)
                } else {
                    recurringTransactions.map { listOf(it) }
                }

                items(staggeredItems) { chunk ->
                    StaggeredItem(index = 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(config.cardSpacing)
                        ) {
                            chunk.forEach { entity ->
                                Box(modifier = Modifier.weight(1f)) {
                                    RecurringTransactionCard(
                                        entity = entity,
                                        currencySymbol = currencySymbol,
                                        onToggleActive = { viewModel.toggleActive(entity.id) },
                                        onEdit = {
                                            viewModel.setEditEntity(entity)
                                            showAddEdit = true
                                        },
                                        onDelete = { entityToDelete = entity }
                                    )
                                }
                            }
                            if (chunk.size < 2 && isTablet) {
                                Box(modifier = Modifier.weight(1f)) {}
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.setEditEntity(null)
                        showAddEdit = true
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF121315)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Recurring",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        CollapsingTopBar(
            title = "Recurring",
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
            }
        )
    }

    if (showAddEdit) {
        AddEditRecurringTransactionBottomSheet(
            onDismiss = {
                showAddEdit = false
                viewModel.setEditEntity(null)
            },
            onSave = { entity ->
                scope.launch {
                    viewModel.save(entity)
                }
            },
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            editEntity = editEntity
        )
    }

    entityToDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { entityToDelete = null },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Delete recurring?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "\"${entity.description}\" will be removed permanently.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entity)
                    entityToDelete = null
                }) {
                    Text("Delete", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { entityToDelete = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }
}

@Composable
private fun RecurringTransactionCard(
    entity: RecurringTransactionEntity,
    currencySymbol: String,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .clickable(onClick = onEdit)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entity.description,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (entity.isActive) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (entity.isActive) "Pause" else "Resume",
                        tint = if (entity.isActive) Color(0xFFFFA726) else Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$currencySymbol ${formatAmount(entity.amount)}",
                color = if (entity.type == "INCOME") Color(0xFF6DD881) else Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entity.frequency.replace("_", " "),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "\u00B7",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
                Text(
                    text = if (entity.autoConfirm) "Auto" else "Pending review",
                    color = if (entity.autoConfirm) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color(0xFFFFA726).copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Next: ${dateFormat.format(Date(entity.nextDueDate))}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )

            if (!entity.isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paused",
                    color = Color(0xFFFFA726),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        String.format("%,d", amount.toLong())
    } else {
        String.format("%,.2f", amount)
    }
}
