package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import com.makemission.makeflow.core.layout.AdaptiveAlertSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.widget.WidgetUpdateHelper
import com.makemission.makeflow.ui.components.AnimatedCounter
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.components.AnimatedDoodle
import com.makemission.makeflow.ui.components.PrimaryButton
import com.makemission.makeflow.ui.verticalFadingEdge
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.viewmodel.AccountsViewModel
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel,
    onDeleteAccount: (suspend (AccountEntity) -> String?)? = null,
    onOpenExchangeRates: () -> Unit = {},
    hapticManager: HapticManager? = null
) {
    val dimens = LocalAppDimens.current
    val accounts by viewModel.accounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val unconvertedAccountCount by viewModel.unconvertedAccountCount.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()
    val context = LocalContext.current

    val config = LocalAdaptiveConfig.current
    val listState = rememberLazyListState()
    var showAddAccount by remember { mutableStateOf(false) }
    var showEditAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    val layoutMode = LocalDeviceLayoutMode.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(accounts) {
        WidgetUpdateHelper.updateWidgets(context)
    }

    if (layoutMode == DeviceLayoutMode.COMPACT) {
        AccountsCompactLayout(
            accounts = accounts,
            selectedCurrency = selectedCurrency,
            exchangeRates = exchangeRates,
            totalBalance = totalBalance,
            unconvertedAccountCount = unconvertedAccountCount,
            onBack = onBack,
            onAddAccount = { showAddAccount = true },
            onEditAccount = { showEditAccount = it },
            onDeleteAccount = { accountToDelete = it },
            onOpenExchangeRates = onOpenExchangeRates,
            hapticManager = hapticManager
        )
    } else if (layoutMode == DeviceLayoutMode.EXPANDED && accounts.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
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
                        LargeTitleItem(title = "Accounts", scrollState = listState)
                    }
                    item {
                        StaggeredItem(index = 1) {
                        CardContainer {
                            Column {
                                Text(
                                    text = "Net worth in $selectedCurrency",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(dimens.spacingXSmall))
                                AnimatedCounter(
                                    targetValue = totalBalance,
                                    formatter = { CurrencyFormatter.format(it, selectedCurrency) },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (unconvertedAccountCount > 0) {
                                    Text(
                                        text = "$unconvertedAccountCount account${if (unconvertedAccountCount == 1) "" else "s"} excluded until rates are available",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        }
                    }
                    if (config.layoutMode != DeviceLayoutMode.COMPACT && config.isLandscape) {
                        val chunked = accounts.chunked(2)
                        itemsIndexed(chunked, key = { _, chunk -> chunk.map { it.id }.joinToString() }) { index, chunk ->
                            StaggeredItem(index = index + 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(config.cardSpacing)
                            ) {
                                chunk.forEach { account ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        val dotColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { LocalMakeFlowColors.current.colorSuccess }
                                        CardContainer {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().pressScale().combinedClickable(onClick={ selectedAccount = account; showEditAccount = null }, onLongClick={ accountToDelete = account }),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(14.dp).clip(CircleShape).background(dotColor, CircleShape))
                                                Spacer(Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(account.name, style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
                                                    Text("Initial: ${CurrencyFormatter.format(account.initialBalance, account.currencyCode ?: selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                                }
                                                Text(CurrencyFormatter.format(account.balance, account.currencyCode ?: selectedCurrency), style=MaterialTheme.typography.titleMedium, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.End)
                                            }
                                        }
                                    }
                                }
                                if (chunk.size == 1) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                            }
                        }
                    } else {
                        itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                            val dotColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { LocalMakeFlowColors.current.colorSuccess }
                            StaggeredItem(index = index + 2) {
                            CardContainer {
                                Row(
                                    modifier = Modifier.fillMaxWidth().pressScale().combinedClickable(onClick={ selectedAccount = account; showEditAccount = null }, onLongClick={ accountToDelete = account }),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(dotColor, CircleShape))
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(account.name, style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
                                        Text("Initial: ${CurrencyFormatter.format(account.initialBalance, account.currencyCode ?: selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                    }
                                    Text(CurrencyFormatter.format(account.balance, account.currencyCode ?: selectedCurrency), style=MaterialTheme.typography.titleMedium, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.End)
                                }
                            }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
                CollapsingTopBar(
                    title = "Accounts",
                    scrollState = listState,
                    modifier = Modifier.align(Alignment.TopStart),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddAccount = true }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add account",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                if (selectedAccount != null) {
                    val acct = selectedAccount!!
                    val dotColor = try { Color(android.graphics.Color.parseColor(acct.colorHex)) } catch (_: Exception) { LocalMakeFlowColors.current.colorSuccess }
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
                                text = "Account Details",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Text(
                                text = acct.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CurrencyFormatter.format(acct.balance, acct.currencyCode ?: selectedCurrency),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Initial balance: ${CurrencyFormatter.format(acct.initialBalance, acct.currencyCode ?: selectedCurrency)}",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select an account to see details",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (accounts.isEmpty()) {
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
                            icon = Icons.Outlined.AccountBalanceWallet,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "No accounts yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Tap + to add your first account",
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
                        bottom = 140.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(config.cardSpacing)
                ) {
                    item {
                        LargeTitleItem(title = "Accounts", scrollState = listState)
                    }
                    item {
                        StaggeredItem(index = 1) {
                        CardContainer {
                            Column {
                                Text(
                                    text = "Net worth in $selectedCurrency",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(dimens.spacingXSmall))
                                AnimatedCounter(
                                    targetValue = totalBalance,
                                    formatter = { CurrencyFormatter.format(it, selectedCurrency) },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (unconvertedAccountCount > 0) {
                                    Text(
                                        text = "$unconvertedAccountCount account${if (unconvertedAccountCount == 1) "" else "s"} excluded until rates are available",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        }
                    }

                    if (config.layoutMode != DeviceLayoutMode.COMPACT && config.isLandscape) {
                        val chunked = accounts.chunked(2)
                        itemsIndexed(chunked, key = { _, chunk -> chunk.map { it.id }.joinToString() }) { index, chunk ->
                            StaggeredItem(index = index + 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(config.cardSpacing)
                            ) {
                                chunk.forEach { account ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        val dotColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { LocalMakeFlowColors.current.colorSuccess }
                                        CardContainer {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().pressScale().combinedClickable(onClick={ showEditAccount = account }, onLongClick={ accountToDelete = account }),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(14.dp).clip(CircleShape).background(dotColor, CircleShape))
                                                Spacer(Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(account.name, style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
                                                    Text("Initial: ${CurrencyFormatter.format(account.initialBalance, account.currencyCode ?: selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                                }
                                                Text(CurrencyFormatter.format(account.balance, account.currencyCode ?: selectedCurrency), style=MaterialTheme.typography.titleMedium, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.End)
                                            }
                                        }
                                    }
                                }
                                if (chunk.size == 1) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                            }
                        }
                    } else {
                        itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                            val dotColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { LocalMakeFlowColors.current.colorSuccess }

                            StaggeredItem(index = index + 2) {
                            CardContainer {
                                Row(
                                    modifier = Modifier.fillMaxWidth().pressScale().combinedClickable(onClick={ showEditAccount = account }, onLongClick={ accountToDelete = account }),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(dotColor, CircleShape))
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(account.name, style=MaterialTheme.typography.bodyLarge, color=MaterialTheme.colorScheme.onSurface)
                                        Text("Initial: ${CurrencyFormatter.format(account.initialBalance, account.currencyCode ?: selectedCurrency)}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                                    }
                                    Text(CurrencyFormatter.format(account.balance, account.currencyCode ?: selectedCurrency), style=MaterialTheme.typography.titleMedium, color=MaterialTheme.colorScheme.onSurface, textAlign=TextAlign.End)
                                }
                            }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            CollapsingTopBar(
                title = "Accounts",
                scrollState = listState,
                modifier = Modifier.align(Alignment.TopStart),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddAccount = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add account",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    }

    if (showAddAccount) {
        AddEditAccountBottomSheet(
            onDismiss = { showAddAccount = false },
            onSave = { name, balance, colorHex, currencyCode ->
                scope.launch {
                    viewModel.addAccount(name, balance, colorHex, currencyCode)
                    showAddAccount = false
                }
            },
            account = null,
            defaultCurrencyCode = selectedCurrency
        )
    }

    showEditAccount?.let { acct ->
        AddEditAccountBottomSheet(
            onDismiss = { showEditAccount = null },
            onSave = { name, balance, colorHex, currencyCode ->
                scope.launch {
                    viewModel.updateAccount(acct, name, balance, colorHex, currencyCode)
                    showEditAccount = null
                }
            },
            account = acct,
            defaultCurrencyCode = selectedCurrency
        )
    }

    accountToDelete?.let { account ->
        AdaptiveAlertSheet(
            onDismissRequest = { accountToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Delete Account",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will remove the account. Transactions linked to it will remain but will show no account.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val error = onDeleteAccount?.let { it(account) }
                        if (error != null) {
                            deleteError = error
                        } else {
                            viewModel.deleteAccount(account)
                        }
                        accountToDelete = null
                    }
                }) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    deleteError?.let { msg ->
        AdaptiveAlertSheet(
            onDismissRequest = { deleteError = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Cannot Delete", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            confirmButton = { TextButton(onClick = { deleteError = null }) { Text("OK", color = MaterialTheme.colorScheme.onSurface) } }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AccountsCompactLayout(
    accounts: List<AccountEntity>,
    selectedCurrency: String,
    exchangeRates: List<ExchangeRateEntity>,
    totalBalance: Double,
    unconvertedAccountCount: Int,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onOpenExchangeRates: () -> Unit,
    hapticManager: HapticManager?
) {
    val dimens = LocalAppDimens.current
    val config = LocalAdaptiveConfig.current
    val exchangeRate = exchangeRates.firstOrNull {
        it.baseCurrencyCode == selectedCurrency && it.currencyCode == "USD" && it.currencyCode != selectedCurrency
    } ?: exchangeRates.firstOrNull { it.baseCurrencyCode == selectedCurrency && it.currencyCode != selectedCurrency }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .height(config.barHeight)
                .padding(horizontal = config.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = config.horizontalPadding,
                top = dimens.paddingExtra,
                end = config.horizontalPadding,
                bottom = dimens.spacingLarge
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cornerRadiusLarge))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(dimens.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
                ) {
                    Text(
                        text = "Total net worth · $selectedCurrency",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedCounter(
                        targetValue = totalBalance,
                        formatter = { CurrencyFormatter.format(it, selectedCurrency) },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (unconvertedAccountCount == 0) "All account balances converted" else
                            "$unconvertedAccountCount account${if (unconvertedAccountCount == 1) "" else "s"} excluded until rates are available",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unconvertedAccountCount == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }
            if (accounts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
                    ) {
                        AnimatedDoodle(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No accounts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                    StaggeredItem(index = index) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale()
                                .combinedClickable(
                                    onClick = { onEditAccount(account) },
                                    onLongClick = { onDeleteAccount(account) }
                                )
                                .padding(vertical = dimens.spacingSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dimens.avatarSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = account.name.trim().take(1).uppercase().ifBlank { "?" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(dimens.spacingLarge))
                            Text(
                                text = account.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(dimens.spacingSmall))
                            val accountCurrency = account.currencyCode ?: selectedCurrency
                            Text(
                                text = CurrencyFormatter.format(account.balance, accountCurrency),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cornerRadiusLarge))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onOpenExchangeRates)
                        .padding(dimens.paddingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
                    ) {
                        Text(
                        text = "Daily reference rate",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val rateText = exchangeRate?.let {
                            "1 ${it.currencyCode} = ${CurrencyFormatter.format(it.rateToDisplayCurrency, selectedCurrency)}"
                        } ?: "Rates will appear after sync"
                        Text(
                            text = rateText,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        PrimaryButton(
            label = "Add account",
            onClick = onAddAccount,
            hapticManager = hapticManager,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = config.horizontalPadding)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        dimens.spacingLarge
                )
        )
    }
}
