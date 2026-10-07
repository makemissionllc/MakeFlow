package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.makemission.makeflow.core.format.CurrencyCatalog
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.ui.theme.LocalAppDimens

@Composable
fun CurrencyPickerBottomSheet(
    selectedCurrency: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    title: String = "Choose currency"
) {
    val dimens = LocalAppDimens.current
    var query by remember { mutableStateOf("") }
    val choices = remember(query) { CurrencyCatalog.filter(query) }

    AdaptiveSheetDialog(onDismiss = onDismiss, scrollable = false) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.78f),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by currency or country") },
                singleLine = true
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
            ) {
                items(choices, key = { it.code }) { currency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(currency.code) }
                            .padding(vertical = dimens.spacingMedium, horizontal = dimens.spacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
                    ) {
                        Text(
                            text = CurrencyFormatter.symbolFor(currency.code),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currency.code, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(currency.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (currency.code == selectedCurrency) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
