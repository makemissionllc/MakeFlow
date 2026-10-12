package com.makemission.makeflow.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.makemission.makeflow.R
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.components.PrimaryDockContentInset
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.theme.OverlayTransition
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun HistoryReviewPane(
    transaction: TransactionEntity?,
    feeTransaction: TransactionEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    defaultCurrency: String,
    zone: ZoneId,
    position: Int,
    count: Int,
    hasPrevious: Boolean,
    hasNext: Boolean,
    topPadding: Dp,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStartReview: () -> Unit,
    onClearSelection: () -> Unit,
    onEdit: ((TransactionEntity) -> Unit)?,
    hapticManager: HapticManager
) {
    val dimens = LocalAppDimens.current
    val paneFocus = remember { FocusRequester() }
    fun navigate(action: () -> Unit) {
        hapticManager.fire(HapticEvent.NAV_SELECT)
        action()
    }
    LaunchedEffect(transaction?.id) {
        if (transaction != null) paneFocus.requestFocus()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("history_review_pane")
                .focusRequester(paneFocus)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false else when {
                        event.key == Key.PageUp && hasPrevious -> { navigate(onPrevious); true }
                        event.key == Key.PageDown && hasNext -> { navigate(onNext); true }
                        else -> false
                    }
                }
                .focusable()
                .padding(top = topPadding, start = dimens.paddingMedium, end = dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.history_review_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                if (transaction != null) {
                    IconButton(onClick = { navigate(onClearSelection) }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.history_review_clear))
                    }
                }
            }
            if (transaction != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = hasPrevious, onClick = { navigate(onPrevious) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.history_review_previous))
                    }
                    Text(
                        stringResource(R.string.history_review_position, position, count),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                    IconButton(enabled = hasNext, onClick = { navigate(onNext) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.history_review_next))
                    }
                    if (onEdit != null) {
                        IconButton(onClick = { navigate { onEdit(transaction) } }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.history_review_edit))
                        }
                    }
                }
            }
            Crossfade(
                targetState = transaction,
                modifier = Modifier.weight(1f),
                animationSpec = if (LocalReducedMotion.current) OverlayTransition.reducedNavigationCrossfade
                    else OverlayTransition.navigationCrossfade,
                label = "historyReview"
            ) { entry ->
                key(entry?.id) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(bottom = PrimaryDockContentInset()),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingXLarge)
                    ) {
                        if (entry == null) {
                            Text(
                                stringResource(if (count == 0) R.string.history_no_matches else R.string.history_review_select),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(enabled = count > 0, onClick = { navigate(onStartReview) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Text(stringResource(R.string.history_review_start))
                            }
                        } else {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXLarge)) {
                                    val category = categories.find { it.id == entry.categoryId }
                                    val parent = categories.find { it.id == category?.parentCategoryId }
                                    Text(
                                        if (entry.type == "TRANSFER") stringResource(R.string.add_transfer)
                                        else category?.let { if (parent != null) "${parent.name} / ${it.name}" else it.name }
                                            ?: stringResource(R.string.history_uncategorized),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    if (entry.type == "TRANSFER") {
                                        val source = accounts.find { it.id == (entry.fromAccountId ?: entry.accountId) }
                                        val destination = accounts.find { it.id == entry.toAccountId }
                                        ReviewField(stringResource(R.string.add_from), source?.name ?: stringResource(R.string.history_unlinked_account))
                                        ReviewAmount(entry.amount, source, defaultCurrency)
                                        ReviewField(stringResource(R.string.add_to), destination?.name ?: stringResource(R.string.history_unlinked_account))
                                        if (entry.destinationAmount != null) ReviewAmount(entry.destinationAmount, destination, defaultCurrency)
                                        else Text(stringResource(R.string.history_amount_unavailable), style = MaterialTheme.typography.bodyLarge)
                                    } else {
                                        val account = accounts.find { it.id == entry.accountId }
                                        Text(
                                            stringResource(if (entry.type == "INCOME") R.string.add_income else R.string.add_expense),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        ReviewAmount(entry.amount, account, defaultCurrency)
                                        ReviewField(stringResource(R.string.add_account), account?.name ?: stringResource(R.string.history_unlinked_account))
                                        if (feeTransaction != null) {
                                            val feeAccount = accounts.find { it.id == feeTransaction.accountId }
                                            ReviewField(
                                                stringResource(R.string.history_mpesa_fee),
                                                CurrencyFormatter.format(feeTransaction.amount, feeAccount?.currencyCode ?: defaultCurrency)
                                            )
                                        }
                                    }
                                    ReviewField(
                                        stringResource(R.string.history_review_date),
                                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                            .format(Instant.ofEpochMilli(entry.timestamp).atZone(zone))
                                    )
                                    if (entry.note.isNotBlank()) ReviewField(stringResource(R.string.history_review_note), entry.note)
                                    entry.externalReference?.takeIf { it.isNotBlank() }?.let {
                                        ReviewField(stringResource(R.string.history_review_reference), it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewAmount(amount: Double, account: AccountEntity?, defaultCurrency: String) {
    Text(
        if (account == null) CurrencyFormatter.formatNumber(amount, 2)
        else CurrencyFormatter.format(amount, account.currencyCode ?: defaultCurrency),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (account == null) {
        Text(stringResource(R.string.history_currency_unavailable), style = MaterialTheme.typography.bodySmall,
            color = LocalMakeFlowColors.current.colorWarning)
    }
}

@Composable
private fun ReviewField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.spacingXSmall)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}
