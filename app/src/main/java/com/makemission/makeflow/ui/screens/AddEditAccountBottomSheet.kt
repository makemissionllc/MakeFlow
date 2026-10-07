package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.theme.MindfulAccentHex
import kotlinx.coroutines.launch

private val accountColors = listOf(
    MindfulAccentHex, "#2196F3", "#FF9800", "#E91E63",
    "#9C27B0", "#00BCD4", "#FF5722", "#607D8B"
)

@Composable
fun AddEditAccountBottomSheet(
    onDismiss: () -> Unit,
    onSave: suspend (name: String, balance: Double, colorHex: String) -> Unit,
    account: AccountEntity? = null,
    currencySymbol: String
) {
    val dimens = LocalAppDimens.current
    val isEdit = account != null
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(account?.name ?: "") }
    var balanceInput by remember { mutableStateOf(account?.balance?.let { String.format("%.2f", it) } ?: "") }
    var selectedColorHex by remember { mutableStateOf(account?.colorHex ?: accountColors.first()) }

    val parsedBalance = balanceInput.toDoubleOrNull()
    val isSaveEnabled = name.isNotBlank()

    AdaptiveSheetDialog(
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEdit) "Edit Account" else "New Account",
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

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textM),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        Box {
                            if (name.isEmpty()) {
                                Text(
                                    text = "Account name",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = dimens.textM
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                Text(
                    text = "Current balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currencySymbol,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingMedium))
                    BasicTextField(
                        value = balanceInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                balanceInput = newValue
                            }
                        },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            Box {
                                if (balanceInput.isEmpty()) {
                                    Text(
                                        text = "0.00",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.width(200.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)) {
                    accountColors.forEach { colorHex ->
                        val isSelected = colorHex == selectedColorHex
                        val dotColor = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (_: Exception) {
                            LocalMakeFlowColors.current.colorSuccess
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .padding(if (isSelected) 3.dp else 0.dp)
                                .clip(CircleShape)
                                .size(28.dp)
                                .background(dotColor, CircleShape)
                                .clickable { selectedColorHex = colorHex }
                        )
                    }
                }
            }

            androidx.compose.material3.Button(
                onClick = {
                    val balance = parsedBalance ?: 0.0
                    scope.launch {
                        onSave(name.trim(), balance, selectedColorHex)
                        onDismiss()
                    }
                },
                enabled = isSaveEnabled,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(dimens.shapeCornerMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (isEdit) "Update Account" else "Save Account",
                    fontSize = dimens.textL,
                    fontWeight = FontWeight.Bold
                )
            }
    }
}
