package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.makemission.makeflow.R
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.domain.AccountActivity
import com.makemission.makeflow.ui.components.PrimaryDockContentInset
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun AccountActivityPane(
    account: AccountEntity?,
    activity: List<AccountActivity.Entry>?,
    defaultCurrency: String,
    topPadding: Dp,
    onEdit: () -> Unit,
    onClear: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier.fillMaxSize().testTag("account_activity_pane")
                .padding(top = topPadding, start = dimens.paddingMedium, end = dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXLarge)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.accounts_activity), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (account != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.accounts_edit))
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.accounts_clear_selection))
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = PrimaryDockContentInset()),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingXLarge)
            ) {
                if (account == null) {
                    item { Text(stringResource(R.string.accounts_select_activity), style = MaterialTheme.typography.bodyLarge) }
                } else {
                    val currency = account.currencyCode ?: defaultCurrency
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                            Text(account.name, style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.accounts_current_balance, currency), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.format(account.balance, currency),
                                style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.accounts_initial_balance, CurrencyFormatter.format(account.initialBalance, currency)),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { Text(stringResource(R.string.accounts_recent_activity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                    if (activity == null) {
                        item { Text(stringResource(R.string.accounts_activity_loading), style = MaterialTheme.typography.bodyMedium) }
                    } else if (activity.isEmpty()) {
                        item { Text(stringResource(R.string.accounts_activity_empty), style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        items(activity, key = { it.transaction.id }) { entry ->
                            SelectionContainer {
                                Column(modifier = Modifier.fillMaxWidth().testTag("account_activity_${entry.transaction.id}"),
                                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                                    val directionLabel = when (entry.direction) {
                                        AccountActivity.Direction.INCOME -> R.string.add_income
                                        AccountActivity.Direction.EXPENSE -> R.string.add_expense
                                        AccountActivity.Direction.TRANSFER_IN -> R.string.accounts_transfer_in
                                        AccountActivity.Direction.TRANSFER_OUT -> R.string.accounts_transfer_out
                                        AccountActivity.Direction.UNAVAILABLE -> if (entry.transaction.type == "TRANSFER") R.string.add_transfer
                                            else R.string.accounts_activity_unknown
                                    }
                                    Text(stringResource(directionLabel), style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val incoming = entry.direction == AccountActivity.Direction.INCOME || entry.direction == AccountActivity.Direction.TRANSFER_IN
                                    Text(
                                        entry.amount?.let { (if (incoming) "+" else "-") + CurrencyFormatter.format(it, currency) }
                                            ?: stringResource(R.string.accounts_activity_amount_unavailable),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (entry.amount == null) LocalMakeFlowColors.current.colorWarning
                                            else if (incoming) LocalMakeFlowColors.current.colorSuccess else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (entry.transaction.note.isNotBlank()) Text(entry.transaction.note, style = MaterialTheme.typography.bodyLarge)
                                    Text(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                        .format(Instant.ofEpochMilli(entry.transaction.timestamp).atZone(ZoneId.systemDefault())),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    entry.transaction.externalReference?.takeIf { it.isNotBlank() }?.let {
                                        Text(stringResource(R.string.accounts_activity_reference, it), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = dimens.spacingXLarge))
                        }
                    }
                }
            }
        }
    }
}
