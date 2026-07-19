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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.viewmodel.FocusedAccountState
import com.makemission.makeflow.ui.viewmodel.HistoryViewModel
import com.makemission.makeflow.core.format.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    hapticManager: HapticManager,
    onEditTransaction: ((TransactionEntity) -> Unit)? = null
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsState()

    val layoutMode = LocalDeviceLayoutMode.current
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

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
            hapticManager.fire(HapticEvent.DELETE_UNDO_AVAILABLE)
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

    if (layoutMode == DeviceLayoutMode.EXPANDED) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                HistoryListContent(
                    viewModel = viewModel,
                    hapticManager = hapticManager,
                    config = config,
                    transactions = transactions,
                    accounts = accounts,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    selectedCurrency = selectedCurrency,
                    selectedFilter = selectedFilter,
                    selectedTransaction = selectedTransaction,
                    groupedTransactions = groupedTransactions,
                    listState = listState,
                    snackbarHostState = snackbarHostState,
                    todayCal = todayCal,
                    yesterdayCal = yesterdayCal,
                    onTransactionClick = { tx -> selectedTransaction = tx }
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                if (selectedTransaction != null) {
                    val tx = selectedTransaction!!
                    val category = categories.find { it.id == tx.categoryId }
                    val categoryName = category?.name ?: "Uncategorized"
                    val categoryColor = try {
                        Color(android.graphics.Color.parseColor(category?.colorHex ?: "#9E9E9E"))
                    } catch (_: Exception) { Color(0xFF9E9E9E) }
                    val account = accounts.find { it.id == tx.accountId }
                    val accountName = account?.name ?: ""
                    val prefix = if (tx.type == "INCOME") "+" else "-"
                    val amountColor = if (tx.type == "INCOME") Color(0xFF34C759) else Color(0xFFFF3B30)
                    val formattedAmount = CurrencyFormatter.format(tx.amount, selectedCurrency)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight + 8.dp,
                                start = 24.dp, end = 24.dp
                            )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(
                                text = "Transaction Details",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(categoryColor, CircleShape)
                            )
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "$prefix$formattedAmount",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tx.type,
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = android.text.format.DateFormat.format("dd MMM yyyy", Date(tx.timestamp)).toString(),
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = accountName,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (tx.note.isNotBlank()) {
                                Text(
                                    text = tx.note,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (onEditTransaction != null) {
                                androidx.compose.material3.Button(
                                    onClick = { onEditTransaction(tx) },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF121315)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Edit", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select a transaction to see details",
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    } else {
        HistoryListContent(
            viewModel = viewModel,
            hapticManager = hapticManager,
            config = config,
            transactions = transactions,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            selectedCurrency = selectedCurrency,
            selectedFilter = selectedFilter,
            selectedTransaction = selectedTransaction,
            groupedTransactions = groupedTransactions,
            listState = listState,
            snackbarHostState = snackbarHostState,
            todayCal = todayCal,
            yesterdayCal = yesterdayCal,
            onTransactionClick = onEditTransaction
        )
    }
}

@Composable
private fun HistoryListContent(
    viewModel: HistoryViewModel,
    hapticManager: HapticManager,
    config: com.makemission.makeflow.ui.components.AdaptiveConfig,
    transactions: List<TransactionEntity>,
    accounts: List<com.makemission.makeflow.data.local.entity.AccountEntity>,
    categories: List<com.makemission.makeflow.data.local.entity.CategoryEntity>,
    currencySymbol: String,
    selectedCurrency: String,
    selectedFilter: String,
    selectedTransaction: TransactionEntity?,
    groupedTransactions: Map<String, List<TransactionEntity>>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snackbarHostState: SnackbarHostState,
    todayCal: Calendar,
    yesterdayCal: Calendar,
    onTransactionClick: ((TransactionEntity) -> Unit)?
) {
    val dismissStates = remember { mutableMapOf<Long, androidx.compose.material3.SwipeToDismissBoxState>() }
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
                    topFadeHeight = config.fadingEdgeHeight,
                    bottomFadeHeight = config.fadingEdgeHeight
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(if (config.isTablet && config.isLandscape) 12.dp else 8.dp)
                ) {
                    val isSelectedAll = selectedFilter == "ALL"
                    Text(
                        text = "All",
                        color = if (isSelectedAll) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (config.isTablet && config.isLandscape) 14.sp else 13.sp,
                        modifier = Modifier
                            .pressScale()
                            .background(
                                if (isSelectedAll) Color.White else Color(0xFF1B1C1F),
                                RoundedCornerShape(if (config.isTablet && config.isLandscape) 16.dp else 14.dp)
                            )
                            .clickable { viewModel.selectedFilter.value = "ALL" }
                            .padding(horizontal = if (config.isTablet && config.isLandscape) 18.dp else 14.dp, vertical = if (config.isTablet && config.isLandscape) 10.dp else 8.dp)
                    )
                    val isSelectedIncome = selectedFilter == "INCOME"
                    Text(
                        text = "Income",
                        color = if (isSelectedIncome) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (config.isTablet && config.isLandscape) 14.sp else 13.sp,
                        modifier = Modifier
                            .pressScale()
                            .background(
                                if (isSelectedIncome) Color.White else Color(0xFF1B1C1F),
                                RoundedCornerShape(if (config.isTablet && config.isLandscape) 16.dp else 14.dp)
                            )
                            .clickable { viewModel.selectedFilter.value = "INCOME" }
                            .padding(horizontal = if (config.isTablet && config.isLandscape) 18.dp else 14.dp, vertical = if (config.isTablet && config.isLandscape) 10.dp else 8.dp)
                    )
                    val isSelectedExpense = selectedFilter == "EXPENSE"
                    Text(
                        text = "Expense",
                        color = if (isSelectedExpense) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (config.isTablet && config.isLandscape) 14.sp else 13.sp,
                        modifier = Modifier
                            .pressScale()
                            .background(
                                if (isSelectedExpense) Color.White else Color(0xFF1B1C1F),
                                RoundedCornerShape(if (config.isTablet && config.isLandscape) 16.dp else 14.dp)
                            )
                            .clickable { viewModel.selectedFilter.value = "EXPENSE" }
                            .padding(horizontal = if (config.isTablet && config.isLandscape) 18.dp else 14.dp, vertical = if (config.isTablet && config.isLandscape) 10.dp else 8.dp)
                    )
                }
            }
            item {
                val historyFocusedState by viewModel.focusedAccountState.collectAsState()
                val historyFocusedReady by viewModel.focusedAccountReady.collectAsState()
                if (historyFocusedReady) {
                    Text(
                        text = "Account: ${
                            when (val state = historyFocusedState) {
                                is FocusedAccountState.Loading -> ""
                                is FocusedAccountState.ShowAll -> "All Accounts"
                                is FocusedAccountState.Focused -> accounts.find { it.id == state.accountId }?.name ?: "All Accounts"
                            }
                        }",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
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
                        val fromAccount = if (tx.type == "TRANSFER") accounts.find { it.id == tx.fromAccountId } else null
                        val toAccount = if (tx.type == "TRANSFER") accounts.find { it.id == tx.toAccountId } else null
                        val fromAccountName = fromAccount?.name ?: ""
                        val toAccountName = toAccount?.name ?: ""
                        val prefix = if (tx.type == "INCOME") "+" else if (tx.type == "TRANSFER") "" else "-"
                        val amountColor = if (tx.type == "INCOME") Color(0xFF34C759) else if (tx.type == "TRANSFER") Color(0xFF6DD881) else Color(0xFFFF3B30)
                        val formattedAmount = CurrencyFormatter.format(tx.amount, selectedCurrency)

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    hapticManager.fire(HapticEvent.SWIPE_ACTION_ARMED)
                                    viewModel.deleteTransaction(tx)
                                    hapticManager.fire(HapticEvent.TRANSACTION_DELETED)
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
                                    .fillMaxWidth()
                                    .background(Color(0xFF121315), MaterialTheme.shapes.medium)
                                    .then(
                                        if (onTransactionClick != null) {
                                            Modifier.clickable { onTransactionClick(tx) }
                                        } else Modifier
                                    )
                            ) {
                                if (config.isTablet && config.isLandscape) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (tx.type == "TRANSFER") {
                                            Icon(
                                                imageVector = Icons.Rounded.SwapHoriz,
                                                contentDescription = "Transfer",
                                                tint = Color(0xFF6DD881),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Box(Modifier.size(10.dp).background(categoryColor, CircleShape))
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(0.3f)) {
                                            if (tx.type == "TRANSFER") {
                                                Text("Transfer", style=MaterialTheme.typography.bodyLarge, color=Color.White)
                                            } else {
                                                Text(categoryName, style=MaterialTheme.typography.bodyLarge, color=Color.White)
                                            }
                                        }
                                        val subtitle = buildString {
                                            if (tx.type == "TRANSFER") {
                                                if (fromAccountName.isNotEmpty() && toAccountName.isNotEmpty()) {
                                                    append("From $fromAccountName to $toAccountName")
                                                }
                                            } else {
                                                if (accountName.isNotEmpty()) append(accountName)
                                            }
                                            if (tx.note.isNotBlank()) {
                                                if (isNotEmpty()) append(" - ")
                                                append(tx.note)
                                            }
                                        }
                                        Text(
                                            text = subtitle.ifEmpty { "" },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.weight(0.4f),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "$prefix$formattedAmount",
                                            color = amountColor,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.3f)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (tx.type == "TRANSFER") {
                                            Icon(
                                                imageVector = Icons.Rounded.SwapHoriz,
                                                contentDescription = "Transfer",
                                                tint = Color(0xFF6DD881),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else {
                                            Box(Modifier.size(8.dp).background(categoryColor, CircleShape))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (tx.type == "TRANSFER") {
                                                Text("Transfer", style=MaterialTheme.typography.bodyLarge, color=Color.White)
                                            } else {
                                                Text(categoryName, style=MaterialTheme.typography.bodyLarge, color=Color.White)
                                            }
                                            val subtitle = buildString {
                                                if (tx.type == "TRANSFER") {
                                                    if (fromAccountName.isNotEmpty() && toAccountName.isNotEmpty()) {
                                                        append("From $fromAccountName to $toAccountName")
                                                    }
                                                } else {
                                                    if (accountName.isNotEmpty()) append(accountName)
                                                }
                                                if (tx.note.isNotBlank()) {
                                                    if (isNotEmpty()) append(" - ")
                                                    append(tx.note)
                                                }
                                            }
                                            if (subtitle.isNotEmpty()) {
                                                Text(subtitle, style=MaterialTheme.typography.labelMedium, color=Color.White.copy(alpha=0.5f))
                                            }
                                        }
                                        Text("$prefix$formattedAmount", color=amountColor, style=MaterialTheme.typography.bodyLarge, fontWeight=FontWeight.SemiBold, textAlign=TextAlign.End, modifier = Modifier.padding(end = 4.dp))
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
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
