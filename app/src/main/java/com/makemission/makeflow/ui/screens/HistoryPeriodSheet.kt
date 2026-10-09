package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.makemission.makeflow.R
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.layout.AdaptiveSheetHeader
import com.makemission.makeflow.domain.HistoryPeriod
import com.makemission.makeflow.ui.theme.LocalAppDimens
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun historyPeriodLabel(period: HistoryPeriod): String = stringResource(
    when (period) {
        HistoryPeriod.ALL_TIME -> R.string.history_all_time
        HistoryPeriod.TODAY -> R.string.history_today
        HistoryPeriod.LAST_SEVEN_DAYS -> R.string.history_last_seven_days
        HistoryPeriod.THIS_MONTH -> R.string.history_this_month
        HistoryPeriod.LAST_MONTH -> R.string.history_last_month
    }
)

@Composable
internal fun HistoryPeriodSheet(
    selectedPeriod: HistoryPeriod,
    calendarNow: ZonedDateTime,
    onSelect: (HistoryPeriod) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.history_date_range)
) {
    val dimens = LocalAppDimens.current
    val locale = Locale.getDefault()
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    AdaptiveSheetDialog(onDismiss = onDismiss) {
        AdaptiveSheetHeader(title = title, onDismiss = onDismiss)
        Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            HistoryPeriod.entries.forEach { period ->
                val range = period.timestampRange(calendarNow.toLocalDate(), calendarNow.zone)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = dimens.primaryButtonHeight)
                        .selectable(
                            selected = period == selectedPeriod,
                            role = Role.RadioButton,
                            onClick = { onSelect(period) }
                        )
                        .padding(vertical = dimens.spacingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
                    ) {
                        Text(historyPeriodLabel(period), style = MaterialTheme.typography.titleMedium)
                        if (range != null) {
                            val start = Instant.ofEpochMilli(range.first).atZone(calendarNow.zone).toLocalDate()
                            val end = Instant.ofEpochMilli(range.last).atZone(calendarNow.zone).toLocalDate()
                            Text(
                                text = if (start == end) formatter.format(start) else
                                    stringResource(R.string.history_period_dates, formatter.format(start), formatter.format(end)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(selected = period == selectedPeriod, onClick = null)
                }
            }
        }
    }
}
