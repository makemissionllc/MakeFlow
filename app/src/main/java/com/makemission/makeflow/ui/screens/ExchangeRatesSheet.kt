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

private val ALL_CURRENCIES = listOf("KES", "USD", "EUR", "GBP", "UGX", "TZS")

@Composable
fun ExchangeRatesSheet(
    rates: List<ExchangeRateEntity>,
    displayCurrency: String,
    onDismiss: () -> Unit,
    onSaveRate: (ExchangeRateEntity) -> Unit
) {
    var editValues by remember {
        mutableStateOf(rates.associate { it.currencyCode to it.rateToDisplayCurrency.toString() })
    }
    var selectedBase by remember { mutableStateOf(displayCurrency) }

    val rateMap = rates.associateBy { it.currencyCode }

    AdaptiveSheetDialog(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exchange Rates",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        Text(
            text = "1 $selectedBase = ? (enter rate to convert to $selectedBase)",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(ALL_CURRENCIES) { currency ->
                if (currency != selectedBase) {
                    val currentRate = rateMap[currency]
                    val editText = editValues[currency] ?: currentRate?.rateToDisplayCurrency?.toString() ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1C1F), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency,
                            color = Color.White,
                            fontSize = 15.sp,
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
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (editText.isEmpty()) {
                                        Text(
                                            text = "e.g. 150.0",
                                            color = Color.White.copy(alpha = 0.3f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Text(
                            text = selectedBase,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(0.2f).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
