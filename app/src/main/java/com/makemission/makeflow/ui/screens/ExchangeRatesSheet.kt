package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.makemission.makeflow.core.format.CurrencyCatalog
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.layout.AdaptiveSheetHeader
import com.makemission.makeflow.core.layout.AdaptiveSheetPrimaryButton
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.ui.theme.LocalAppDimens

@Composable
fun ExchangeRatesSheet(
    rates: List<ExchangeRateEntity>,
    displayCurrency: String,
    isRefreshing: Boolean,
    refreshError: String?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val visibleRates = remember(rates, displayCurrency) {
        rates.filter { it.baseCurrencyCode == displayCurrency && it.currencyCode != displayCurrency }
            .sortedBy { it.currencyCode }
    }
    val rateDate = visibleRates.mapNotNull { it.rateDate }.maxOrNull()

    AdaptiveSheetDialog(onDismiss = onDismiss, scrollable = false) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.78f),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            AdaptiveSheetHeader(
                title = "Exchange rates",
                subtitle = if (rateDate != null) "Daily reference rates · dated $rateDate · base $displayCurrency" else
                    "No reference rates are cached for $displayCurrency yet.",
                onDismiss = onDismiss
            )
            Text(
                text = "Rates refresh automatically when you open the app. They are mid-market reference rates, not live trading prices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AdaptiveSheetPrimaryButton(
                label = if (isRefreshing) "Refreshing rates" else "Refresh rates",
                onClick = onRefresh,
                enabled = !isRefreshing
            )

            if (refreshError != null) {
                Text(
                    text = "$refreshError Showing the last saved rates, if available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (visibleRates.isEmpty()) {
                Text(
                    text = "No rates are available for this base currency yet. Check your connection and refresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                ) {
                    items(visibleRates, key = { it.currencyCode }) { rate ->
                        val name = CurrencyCatalog.currencies.firstOrNull { it.code == rate.currencyCode }?.name.orEmpty()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${rate.currencyCode} · $name",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "1 ${rate.currencyCode} = ${CurrencyFormatter.format(rate.rateToDisplayCurrency, displayCurrency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}
