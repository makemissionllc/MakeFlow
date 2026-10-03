package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.ui.theme.LocalAppDimens

private val ALL_CURRENCIES = listOf("KES", "USD", "EUR", "GBP", "UGX", "TZS")

@Composable
fun ExchangeRatesSheet(
    rates: List<ExchangeRateEntity>,
    displayCurrency: String,
    onDismiss: () -> Unit,
    onSaveRate: (ExchangeRateEntity) -> Unit
) {
    val dimens = LocalAppDimens.current
    var editValues by remember {
        mutableStateOf(rates.associate { it.currencyCode to it.rateToDisplayCurrency.toString() })
    }
    var selectedBase by remember { mutableStateOf(displayCurrency) }

    val rateMap = rates.associateBy { it.currencyCode }

    AdaptiveSheetDialog(onDismiss = onDismiss, scrollable = false) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exchange Rates",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Text(
                    text = "1 $selectedBase = ? (enter rate to convert to $selectedBase)",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = dimens.textSm,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(ALL_CURRENCIES) { currency ->
                if (currency != selectedBase) {
                    val currentRate = rateMap[currency]
                    val editText = editValues[currency] ?: currentRate?.rateToDisplayCurrency?.toString() ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = dimens.textMl,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.3f)
                        )
                        BasicTextField(
                            value = editText,
                            onValueChange = { newValue ->
                                editValues = editValues + (currency to newValue)
                                val parsed = newValue.toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    onSaveRate(ExchangeRateEntity(currencyCode = currency, rateToDisplayCurrency = parsed))
                                }
                            },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textM),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (editText.isEmpty()) {
                                        Text(
                                            text = "e.g. 150.0",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            fontSize = dimens.textM
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerXSmall))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Text(
                            text = selectedBase,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = dimens.textSm,
                            modifier = Modifier.weight(0.2f).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
