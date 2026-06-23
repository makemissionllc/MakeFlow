package com.makemission.makeflow.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsState()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val todayCal = remember { Calendar.getInstance() }
    val yesterdayCal = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) } }

    val groupedTransactions = remember(transactions) {
        val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        transactions.groupBy { tx ->
            keyFormat.format(Date(tx.timestamp))
        }.toSortedMap(reverseOrder())
    }

    LaunchedEffect(recentlyDeleted) {
        if (recentlyDeleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Transaction deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        }
    }

    val config = LocalAdaptiveConfig.current
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { _ ->
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
                start = config.horizontalPadding, end = config.horizontalPadding,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LargeTitleItem(title = "History", scrollState = listState)
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isSelected = selectedFilter == "ALL"
                        Text(
                            text = "All",
                            color = if (isSelected) Color(0xFF121315) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .pressScale()
                                .background(
                                    if (isSelected) Color.White else Color(0xFF1B1C1F),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectedFilter.value = "ALL" }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        val isSelected = selectedFilter == "INCOME"
                        Text(
                            text = "Income",
                            color = if (isSelected) Color(0xFF121315) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .pressScale()
                                .background(
                                    if (isSelected) Color.White else Color(0xFF1B1C1F),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectedFilter.value = "INCOME" }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        val isSelected = selectedFilter == "EXPENSE"
                        Text(
                            text = "Expense",
                            color = if (isSelected) Color(0xFF121315) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .pressScale()
                                .background(
                                    if (isSelected) Color.White else Color(0xFF1B1C1F),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectedFilter.value = "EXPENSE" }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Receipt,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Tap + to log your first transaction",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            } else {
                var itemIndex = 0
                groupedTransactions.forEach { (dateKey, txList) ->
                    item(key = "header_$dateKey") {
                        StaggeredItem(index = itemIndex++) {
                            val label = getDateHeaderLabel(dateKey, todayCal, yesterdayCal)
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    itemsIndexed(txList, key = { _, tx -> tx.id }) { txIndex, tx ->
                        StaggeredItem(index = itemIndex++) {
                        val category = categories.find { it.id == tx.categoryId }
                        val categoryName = category?.name ?: "Uncategorized"
                        val categoryColor = try {
                            Color(android.graphics.Color.parseColor(category?.colorHex ?: "#9E9E9E"))
                        } catch (_: Exception) {
                            Color(0xFF9E9E9E)
                        }
                        val account = accounts.find { it.id == tx.accountId }
                        val accountName = account?.name ?: ""
                        val prefix = if (tx.type == "INCOME") "+" else "-"
                        val amountColor = if (tx.type == "INCOME") Color(0xFF34C759) else Color(0xFFFF3B30)
                        val formattedAmount = "$currencySymbol ${String.format("%,.2f", tx.amount)}"

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteTransaction(tx)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val bgColor by animateColorAsState(
                                    targetValue = if (dismissState.progress > 0f)
                                        Color(0xFFB71C1C) else Color(0xFF2C2C2C),
                                    animationSpec = tween(300),
                                    label = "swipeBg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(bgColor)
                                        .padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Delete",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .padding(bottom = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(categoryColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White
                                        )
                                        val subtitle = buildString {
                                            if (accountName.isNotEmpty()) append(accountName)
                                            if (tx.note.isNotBlank()) {
                                                if (isNotEmpty()) append(" - ")
                                                append(tx.note)
                                            }
                                        }
                                        if (subtitle.isNotEmpty()) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$prefix$formattedAmount",
                                        color = amountColor,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End
                                    )
                                }
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.06f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                        }
                    }
                }
            }
        }

        CollapsingTopBar(
            title = "History",
            scrollState = listState,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
    }
}

private fun getDateHeaderLabel(dateKey: String, todayCal: Calendar, yesterdayCal: Calendar): String {
    val format = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val date = format.parse(dateKey) ?: return dateKey
    val cal = Calendar.getInstance().apply { time = date }
    return when {
        isSameDay(cal, todayCal) -> "Today"
        isSameDay(cal, yesterdayCal) -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
