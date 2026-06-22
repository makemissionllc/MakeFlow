package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val todayCal = remember { Calendar.getInstance() }
    val yesterdayCal = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) } }

    val groupedTransactions = remember(transactions) {
        val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        transactions.groupBy { tx ->
            keyFormat.format(Date(tx.timestamp))
        }.toSortedMap(reverseOrder())
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        Spacer(modifier = Modifier.height(96.dp))

        LazyRow(
            modifier = Modifier.padding(horizontal = 20.dp),
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
                        .background(
                            if (isSelected) Color.White else Color(0xFF1B1C1F),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.selectedFilter.value = "EXPENSE" }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
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
                    top = 12.dp, bottom = 140.dp
                )
            ) {
                groupedTransactions.forEach { (dateKey, items) ->
                    item(key = "header_$dateKey") {
                        val label = getDateHeaderLabel(dateKey, todayCal, yesterdayCal)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(items, key = { it.id }) { tx ->
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121315).copy(alpha = 0.85f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
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
