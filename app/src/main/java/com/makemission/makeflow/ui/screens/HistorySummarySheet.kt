package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.makemission.makeflow.R
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.layout.AdaptiveSheetHeader
import com.makemission.makeflow.domain.HistorySummary
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors

@Composable
internal fun HistorySummarySheet(
    summary: HistorySummary.Result,
    scopeLabel: String,
    searchQuery: String,
    onDismiss: () -> Unit
) {
    val dimens = LocalAppDimens.current
    AdaptiveSheetDialog(onDismiss = onDismiss) {
        AdaptiveSheetHeader(
            title = stringResource(R.string.history_summary),
            subtitle = scopeLabel,
            onDismiss = onDismiss
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
            Text(
                pluralStringResource(R.plurals.history_result_count, summary.transactionCount, summary.transactionCount),
                style = MaterialTheme.typography.titleMedium
            )
            if (searchQuery.isNotBlank()) {
                Text(
                    stringResource(R.string.history_summary_search, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        summary.currencies.forEach { totals ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                Text(totals.currencyCode, style = MaterialTheme.typography.titleMedium)
                SummaryAmount(
                    stringResource(R.string.history_summary_income),
                    CurrencyFormatter.format(totals.income, totals.currencyCode),
                    LocalMakeFlowColors.current.colorSuccess
                )
                SummaryAmount(
                    stringResource(R.string.history_summary_spending),
                    CurrencyFormatter.format(totals.spending, totals.currencyCode),
                    MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider()
                SummaryAmount(
                    stringResource(R.string.history_summary_net),
                    CurrencyFormatter.format(totals.net, totals.currencyCode),
                    MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (summary.transferCount > 0) {
            Text(
                pluralStringResource(R.plurals.history_summary_transfers, summary.transferCount, summary.transferCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (summary.excludedCount > 0) {
            Text(
                pluralStringResource(R.plurals.history_summary_excluded, summary.excludedCount, summary.excludedCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (summary.transactionCount == 0) {
            Text(stringResource(R.string.history_no_matches), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SummaryAmount(label: String, amount: String, amountColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.spacingMedium)
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            amount,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
            textAlign = TextAlign.End
        )
    }
}
