package com.makemission.makeflow.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.makemission.makeflow.R
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.PrimaryDockContentInset
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.components.AnimatedDoodle
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.HistoryPeriod
import com.makemission.makeflow.domain.MpesaFeeMatcher
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.theme.LocalExpressiveMotion
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.OverlayTransition
import com.makemission.makeflow.ui.viewmodel.HistoryViewModel
import com.makemission.makeflow.core.format.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    hapticManager: HapticManager,
    onEditTransaction: ((TransactionEntity) -> Unit)? = null
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsState()
    val calendarNow by viewModel.calendarNow.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDateFilters() }

    val layoutMode = LocalDeviceLayoutMode.current
    var selectedTransactionId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTransaction = transactions.find { it.id == selectedTransactionId }
    val selectedIndex = transactions.indexOfFirst { it.id == selectedTransactionId }
    val feeByReference = remember(allTransactions) { MpesaFeeMatcher.byOriginalReference(allTransactions) }
    val selectedFee = selectedTransaction?.externalReference
        ?.takeUnless { it.endsWith("-fee") }
        ?.let(feeByReference::get)

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val todayCal = remember(calendarNow) {
        Calendar.getInstance(TimeZone.getTimeZone(calendarNow.zone)).apply {
            timeInMillis = calendarNow.toInstant().toEpochMilli()
        }
    }
    val yesterdayCal = remember(todayCal) {
        (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    }

    val groupedTransactions = remember(transactions, calendarNow.zone) {
        val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(calendarNow.zone)
        }
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
                    feeByReference = feeByReference,
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
                    onTransactionClick = { tx ->
                        hapticManager.fire(HapticEvent.NAV_SELECT)
                        selectedTransactionId = tx.id
                    }
                )
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                HistoryReviewPane(
                    transaction = selectedTransaction,
                    feeTransaction = selectedFee,
                    accounts = accounts,
                    categories = categories,
                    defaultCurrency = selectedCurrency,
                    zone = calendarNow.zone,
                    position = selectedIndex + 1,
                    count = transactions.size,
                    hasPrevious = selectedIndex > 0,
                    hasNext = selectedIndex >= 0 && selectedIndex < transactions.lastIndex,
                    topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
                    onPrevious = { transactions.getOrNull(selectedIndex - 1)?.let { selectedTransactionId = it.id } },
                    onNext = { transactions.getOrNull(selectedIndex + 1)?.let { selectedTransactionId = it.id } },
                    onStartReview = { selectedTransactionId = transactions.firstOrNull()?.id },
                    onClearSelection = { selectedTransactionId = null },
                    onEdit = onEditTransaction,
                    hapticManager = hapticManager
                )
            }
        }
    } else {
        HistoryListContent(
            viewModel = viewModel,
            hapticManager = hapticManager,
            config = config,
            transactions = transactions,
            feeByReference = feeByReference,
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
    feeByReference: Map<String, TransactionEntity>,
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
    val dimens = LocalAppDimens.current
    val allTransactions by viewModel.transactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val calendarNow by viewModel.calendarNow.collectAsState()
    var showPeriodSheet by rememberSaveable { mutableStateOf(false) }
    val focusedAccountId by viewModel.focusedAccountId.collectAsState()
    val accountReady by viewModel.focusedAccountReady.collectAsState()
    val accountLabel = if (focusedAccountId == null) {
        stringResource(R.string.history_all_accounts)
    } else {
        accounts.find { it.id == focusedAccountId }?.name
            ?: stringResource(R.string.history_account_unavailable)
    }
    val isReviewLayout = LocalDeviceLayoutMode.current == DeviceLayoutMode.EXPANDED
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    if (showAccountSheet) {
        AccountFocusSheet(
            accounts = accounts,
            focusedAccountId = focusedAccountId,
            onSelectAccount = {
                hapticManager.fire(HapticEvent.NAV_SELECT)
                viewModel.selectAccount(it)
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false }
        )
    }
    val summary by viewModel.resultSummary.collectAsState()
    var showSummarySheet by rememberSaveable { mutableStateOf(false) }
    if (showSummarySheet) {
        HistorySummarySheet(
            summary = summary,
            scopeLabel = listOf(
                historyPeriodLabel(selectedPeriod),
                stringResource(when (selectedFilter) {
                    "INCOME" -> R.string.history_summary_income
                    "EXPENSE" -> R.string.history_summary_spending
                    else -> R.string.history_all_types
                }),
                accountLabel
            ).joinToString(" / "),
            searchQuery = searchQuery,
            onDismiss = { showSummarySheet = false }
        )
    }
    if (showPeriodSheet) {
        HistoryPeriodSheet(
            selectedPeriod = selectedPeriod,
            calendarNow = calendarNow,
            onSelect = {
                hapticManager.fire(HapticEvent.TAB_SWITCH)
                viewModel.selectedPeriod.value = it
                showPeriodSheet = false
            },
            onDismiss = { showPeriodSheet = false }
        )
    }
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
                .background(MaterialTheme.colorScheme.background)
                .verticalFadingEdge(
                    topFadeHeight = config.fadingEdgeHeight,
                    bottomFadeHeight = config.fadingEdgeHeight
                ),
            contentPadding = PaddingValues(
                start = config.horizontalPadding, end = config.horizontalPadding,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
                bottom = PrimaryDockContentInset()
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
        ) {
            item {
                LargeTitleItem(title = "History", scrollState = listState)
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search transactions") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
                ) {
                    FilterChip(
                        selected = selectedPeriod != HistoryPeriod.ALL_TIME,
                        onClick = {
                            hapticManager.fire(HapticEvent.NAV_SELECT)
                            viewModel.refreshDateFilters()
                            showPeriodSheet = true
                        },
                        label = { Text(historyPeriodLabel(selectedPeriod)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.DateRange, contentDescription = stringResource(R.string.history_date_range))
                        }
                    )
                    listOf("ALL" to R.string.history_all_types, "INCOME" to R.string.add_income,
                        "EXPENSE" to R.string.add_expense, "FEES" to R.string.history_fees)
                        .forEach { (filter, label) ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = {
                                    hapticManager.fire(HapticEvent.TAB_SWITCH)
                                    viewModel.selectedFilter.value = filter
                                },
                                label = { Text(stringResource(label)) }
                            )
                        }
                }
            }
            item {
                FilterChip(
                    selected = focusedAccountId != null,
                    enabled = accountReady,
                    onClick = {
                        hapticManager.fire(HapticEvent.NAV_SELECT)
                        showAccountSheet = true
                    },
                    label = { Text(accountLabel) },
                    leadingIcon = {
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = stringResource(R.string.history_choose_account))
                    }
                )
            }

            item(key = "result_summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
                ) {
                    Text(
                        pluralStringResource(R.plurals.history_result_count, transactions.size, transactions.size),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        enabled = transactions.isNotEmpty(),
                        onClick = {
                            hapticManager.fire(HapticEvent.NAV_SELECT)
                            showSummarySheet = true
                        }
                    ) {
                        Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(dimens.iconSizeSmall))
                        Spacer(Modifier.width(dimens.spacingSmall))
                        Text(stringResource(R.string.history_summary))
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
                            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
                        ) {
                            AnimatedDoodle(
                                icon = Icons.Rounded.Receipt,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                            Text(
                                text = if (allTransactions.isEmpty()) "No transactions yet" else "No matching transactions",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (allTransactions.isEmpty()) {
                                    "Tap + to log your first transaction"
                                } else if (searchQuery.isBlank() && selectedFilter == "ALL" && selectedPeriod == HistoryPeriod.ALL_TIME) {
                                    "There are no transactions in the selected account"
                                } else {
                                    stringResource(R.string.history_no_matches)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            if (allTransactions.isNotEmpty() &&
                                (searchQuery.isNotBlank() || selectedFilter != "ALL" || selectedPeriod != HistoryPeriod.ALL_TIME)
                            ) {
                                TextButton(
                                    onClick = {
                                        hapticManager.fire(HapticEvent.NAV_SELECT)
                                        viewModel.clearFilters()
                                    }
                                ) {
                                    Text(stringResource(R.string.history_clear_filters))
                                }
                            }
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                            MaterialTheme.colorScheme.outline
                        }
                        val account = accounts.find { it.id == tx.accountId }
                        val linkedFee = tx.externalReference?.takeUnless { it.endsWith("-fee") }
                            ?.let(feeByReference::get)
                        val feeAccount = linkedFee?.let { fee -> accounts.find { it.id == fee.accountId } }
                        val feeLabel = linkedFee?.let { fee ->
                            stringResource(R.string.history_mpesa_fee_amount,
                                CurrencyFormatter.format(fee.amount, feeAccount?.currencyCode ?: selectedCurrency))
                        }
                        val accountName = account?.name ?: ""
                        val fromAccount = if (tx.type == "TRANSFER") accounts.find { it.id == tx.fromAccountId } else null
                        val toAccount = if (tx.type == "TRANSFER") accounts.find { it.id == tx.toAccountId } else null
                        val fromAccountName = fromAccount?.name ?: ""
                        val toAccountName = toAccount?.name ?: ""
                        val prefix = if (tx.type == "INCOME") "+" else if (tx.type == "TRANSFER") "" else "-"
                        val amountColor = if (tx.type == "INCOME") LocalMakeFlowColors.current.colorSuccess else if (tx.type == "TRANSFER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val transactionCurrency = account?.currencyCode ?: selectedCurrency
                        val formattedAmount = if (tx.type == "TRANSFER") {
                            "${CurrencyFormatter.formatCompact(tx.amount, fromAccount?.currencyCode ?: selectedCurrency)} → " +
                                CurrencyFormatter.formatCompact(tx.destinationAmount ?: tx.amount, toAccount?.currencyCode ?: selectedCurrency)
                        } else CurrencyFormatter.format(tx.amount, transactionCurrency)
                        val rowBackground by animateColorAsState(
                            targetValue = if (isReviewLayout && selectedTransaction?.id == tx.id) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            animationSpec = if (LocalReducedMotion.current) OverlayTransition.reducedColor
                                else LocalExpressiveMotion.current.colorAnimationSpec,
                            label = "historySelection"
                        )

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
                                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    animationSpec = if (LocalReducedMotion.current) {
                                        OverlayTransition.reducedColor
                                    } else {
                                        LocalExpressiveMotion.current.colorAnimationSpec
                                    },
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
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Delete",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("history_transaction_${tx.id}")
                                    .background(rowBackground, MaterialTheme.shapes.medium)
                                    .then(
                                        if (onTransactionClick != null) {
                                            if (isReviewLayout) Modifier.selectable(
                                                selected = selectedTransaction?.id == tx.id,
                                                role = Role.Button,
                                                onClick = { onTransactionClick(tx) }
                                            ) else Modifier.clickable { onTransactionClick(tx) }
                                        } else Modifier
                                    )
                            ) {
                                if (config.isTablet && config.isLandscape && !isReviewLayout) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (tx.type == "TRANSFER") {
                                            Icon(
                                                imageVector = Icons.Rounded.SwapHoriz,
                                                contentDescription = "Transfer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Box(Modifier.size(10.dp).background(categoryColor, CircleShape))
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(0.3f)) {
                                            if (tx.type == "TRANSFER") {
                                                Text("Transfer", style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
                                            } else {
                                                Text(categoryName, style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(0.4f),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (tx.type == "TRANSFER") formattedAmount else "$prefix$formattedAmount",
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
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else {
                                            Box(Modifier.size(8.dp).background(categoryColor, CircleShape))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (tx.type == "TRANSFER") {
                                                Text("Transfer", style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
                                            } else {
                                                Text(categoryName, style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
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
                                                Text(
                                                    subtitle,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = if (isReviewLayout) 2 else Int.MAX_VALUE,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text(if (tx.type == "TRANSFER") formattedAmount else "$prefix$formattedAmount", color=amountColor, style=MaterialTheme.typography.bodyLarge, fontWeight=FontWeight.SemiBold, textAlign=TextAlign.End, modifier = Modifier.padding(end = 4.dp))
                                    }
                                }
                                feeLabel?.let { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .padding(start = dimens.spacingXXLarge, bottom = dimens.spacingSmall)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingXSmall)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), thickness = 0.5.dp)
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
